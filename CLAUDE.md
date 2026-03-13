# BOOM (Berkeley Out-of-Order Machine)

BOOM v3 is a superscalar out-of-order RISC-V (RV64GC) processor core written in Chisel. It is the primary optimization target in this project.

## Source Layout

All BOOM v3 sources live under `src/main/scala/v3/`:

### Frontend (`ifu/`)
- `frontend.scala` — Top-level fetch unit. Orchestrates if0→if1→if2→if3→if4 stages, drives BPD, manages redirects
- `icache.scala` — L1 instruction cache
- `fetch-buffer.scala` — Fetch buffer between frontend and decode
- `fetch-target-queue.scala` — FTQ: tracks fetch packets for branch resolution and BPD updates

### Branch Prediction (`ifu/bpd/`)
- `predictor.scala` — Abstract base class for all predictors; defines BranchPredictorBank interface
- `composer.scala` — Composes multiple predictor components into a single bank
- `tage.scala` — TAGE direction predictor (tagged geometric history length)
- `btb.scala` — Branch Target Buffer
- `ita.scala` — Indirect Target Array (custom: single-table indexed by PC hash)
- `ras.scala` — Return Address Stack
- `ubtb.scala` — Micro BTB (small, fast, tagless)
- `faubtb.scala` — Fully-Associative Micro BTB
- `bim.scala` — Bimodal direction predictor
- `hbim.scala` — History-indexed Bimodal
- `local.scala` — Local history predictor
- `tourney.scala` — Tournament predictor (selector between two sub-predictors)
- `loop.scala` — Loop predictor
- `sw_predictor.scala` — Software-programmable predictor

### Execution (`exu/`)
- `core.scala` — Top-level core module. Wires decode→rename→dispatch→issue→execute→commit
- `decode.scala` — Decode stage: RV64GC instruction decoding to micro-ops
- `dispatch.scala` — Dispatch stage: routes micro-ops to issue queues
- `rob.scala` — Reorder Buffer: tracks in-flight instructions, handles commit/rollback

#### Issue Units (`exu/issue-units/`)
- `issue-unit.scala` — Abstract issue unit base class
- `issue-unit-age-ordered.scala` — Age-ordered issue unit (used by MegaBoom)
- `issue-unit-unordered.scala` — Unordered issue unit (used by SmallBoom)
- `issue-slot.scala` — Single issue queue slot

#### Register Read (`exu/register-read/`)
- `register-read.scala` — Register read stage
- `regfile.scala` — Physical register file
- `func-unit-decode.scala` — Functional unit control signal decode

#### Execution Units (`exu/execution-units/`)
- `execution-units.scala` — Container for all execution units
- `execution-unit.scala` — Single execution unit (ALU, MUL, FPU, MEM, etc.)
- `functional-unit.scala` — Functional unit implementations (ALU, MUL, DIV, etc.)
- `fpu.scala` — FPU wrapper (interfaces with rocket-chip FPU)
- `fdiv.scala` — FP divide/sqrt unit
- `rocc.scala` — RoCC accelerator interface

#### Rename (`exu/rename/`)
- `rename-stage.scala` — Rename stage: maps architectural to physical registers
- `rename-maptable.scala` — Rename map table
- `rename-freelist.scala` — Physical register free list
- `rename-busytable.scala` — Busy table (tracks which physical regs have valid data)

### Load/Store Unit (`lsu/`)
- `lsu.scala` — Top-level LSU: load/store queues, memory ordering, address disambiguation
- `dcache.scala` — L1 data cache (wraps rocket-chip's non-blocking DCache)
- `mshrs.scala` — Miss Status Holding Registers for L1D
- `prefetcher.scala` — Hardware prefetcher (experimental)
- `tlb.scala` — Data TLB wrapper
- `tracegen.scala` — Trace generator for memory system testing

### Common (`common/`)
- `parameters.scala` — All BOOM parameters (BoomCoreParams, decoded defaults for MegaBoom/LargeBoom/MediumBoom/SmallBoom)
- `config-mixins.scala` — Config fragments: `WithNSmallBooms`, `WithNMediumBooms`, `WithNLargeBooms`, `WithNMegaBooms`, and optimization fragments
- `tile.scala` — BoomTile: diplomacy node integrating BOOM core with caches
- `consts.scala` — Constants and enumerations
- `types.scala` — Bundle type definitions
- `micro-op.scala` — MicroOp bundle: the fundamental data structure flowing through the pipeline
- `package.scala` — Package-level implicits and utilities
- `BoomPerfCounterDevice.scala` — TMA performance counter MMIO device
- `SimTMACounterDump.scala` — Simulation-only TMA counter dump via plusarg

### Utilities (`util/`)
- `util.scala` — General utility functions
- `elastic-reg.scala` — Elastic pipeline register
- `elastic-sram.scala` — Elastic SRAM wrapper
- `seqmem-transformable.scala` — Transformable sequential memory

## Pipeline Stages

```
Fetch: if0 → if1 → if2 → if3 → if4
                    ↑ BPD predicts here (TAGE at f2, BTB at f2)
Backend: dec → ren → dis → iss → rrd → exe → mem → sxt → wb → com
```

## MegaBoom Default Parameters

| Parameter | Value |
|-----------|-------|
| fetchWidth | 8 (instructions per fetch) |
| decodeWidth | 4 |
| numRobEntries | 128 |
| numIntPhysRegisters | 128 |
| numFpPhysRegisters | 96 |
| numLdqEntries | 32 |
| numStqEntries | 32 |
| maxBrCount | 16 |
| numFetchBufferEntries | 32 |
| ftq.nEntries | 32 |
| nPerfCounters | 29 |
| numDCacheBanks | 1 |
| nMSHRs | 8 (16 in optimized config) |
| memWidth | 2 (dual memory ports) |

## Config Fragments

BOOM configs are composed from fragments in `config-mixins.scala`. The pattern:

```scala
class WithNMegaBooms(n: Int = 1, ...) extends Config(
  new Config((site, here, up) => {
    case TilesLocated(InSubsystem) => {
      // Returns List[BoomTileAttachParams] with MegaBoom parameters
    }
  })
)
```

Top-level SoC configs are in `generators/chipyard/src/main/scala/config/BoomConfigs.scala`.

## Known Bugs and Pitfalls

### `dfmaLatency` controls ALL FP ops (`fpu.scala`)
At `fpu.scala:179`, BOOM uses `dfmaLatency` for the FPU pipeline depth for all FP operations. `sfmaLatency` is completely ignored. Setting `dfmaLatency < 3` causes pipeline hangs in memory-intensive benchmarks.

### ALU+MUL write port collision (`execution-unit.scala:219`)
Combined ALU+MUL execution units had `numBypassStages` hardcoded to 3, which caused write port collisions when `imulLatency != 3`. Fix: set `numBypassStages = imulLatency`.

### FTQ `cfi_is_jalr` never driven (`fetch-target-queue.scala`)
In the BPD update path, `cfi_is_jal` was set but `cfi_is_jalr` was never assigned. This meant all jalr-specific predictor logic (ITA, ITAGE) was dead code. Fix: add `cfi_is_jalr := bpd_entry.cfi_type === CFI_JALR`.

### Store commits only on port 0 (`lsu.scala`)
Store commit logic uses only `w==0`. Port 1 is used for `load_wakeup`. `store_blocked_counter` only activates for `memWidth==1`, so it is irrelevant for MegaBoom (`memWidth=2`).

### `nLBEntries` = `nMSHRs` (`parameters.scala:242`)
The LoadBuffer size is tied to MSHR count. Changing `nMSHRs` also changes `nLBEntries`.

## Build Notes

After editing BOOM Scala sources, you must delete `.classpath_cache/chipyard.jar` to force SBT recompilation. Without this, `make` in `sims/verilator/` uses the stale cached JAR.

```bash
rm -f .classpath_cache/chipyard.jar
cd sims/verilator && make CONFIG=MegaBoomV3OptConfig
```
