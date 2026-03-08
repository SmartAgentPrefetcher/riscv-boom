package boom.v3.common

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{RegField, RegWriteFn}
import freechips.rocketchip.tilelink._

// Number of 64-bit counter registers exposed via MMIO
// Layout (offsets in bytes):
//   0x000: control   (W: bit 0 = snapshot, bit 1 = release snapshot, R: 0)
//   0x008: cycles    (debug_tsc_reg)
//   0x010: instret   (debug_irt_reg)
//   0x018: tma_retiring
//   0x020: tma_bad_speculation
//   0x028: tma_frontend_bound
//   0x030: tma_backend_bound
//   0x038: tma_fetch_latency
//   0x040: tma_fetch_bandwidth
//   0x048: tma_branch_mispredict
//   0x050: tma_machine_clears
//   0x058: tma_memory_bound
//   0x060: tma_core_bound
//   0x068: retired_loads
//   0x070: retired_stores
//   0x078: retired_branches
//   0x080: retired_jals
//   0x088: retired_jalrs
//   0x090: retired_fp
//   0x098: retired_amo
//   0x0A0: retired_system
//   0x0A8: rob_full_cycles
//   0x0B0: ldq_full_cycles
//   0x0B8: stq_full_cycles
//   0x0C0: int_iq_full_cycles
//   0x0C8: mem_iq_full_cycles
//   0x0D0: branch_mask_full_cycles
//   0x0D8: rename_stall_cycles
//   0x0E0: flush_cycles
//   0x0E8: rollback_cycles
//   0x0F0: icache_miss
//   0x0F8: dcache_miss
//   0x100: dcache_release
//   0x108: itlb_miss
//   0x110: dtlb_miss
//   0x118: l2tlb_miss
//   0x120: br_mispredict
//   0x128: br_resolve
//   0x130: jalr_mispredict
//   0x138: br_mispredict_bpd
//   0x140: br_mispredict_btb

object BoomPerfCounterConsts {
  val NUM_COUNTERS = 40
}

case class BoomPerfCounterParams(
  address: BigInt = 0x10030000L
)

class BoomPerfCounterIO extends Bundle {
  val counters = Input(Vec(BoomPerfCounterConsts.NUM_COUNTERS, UInt(64.W)))
}

class BoomPerfCounterDevice(params: BoomPerfCounterParams, beatBytes: Int)(implicit p: Parameters)
  extends LazyModule
{
  val device = new SimpleDevice("boom-perf-counters", Seq("ucb-bar,boom-perf-counters"))
  val node = TLRegisterNode(
    Seq(AddressSet(params.address, 4096 - 1)),
    device,
    "reg/control",
    beatBytes = beatBytes)

  override lazy val module: BoomPerfCounterDeviceImp = new BoomPerfCounterDeviceImp(this)
  class BoomPerfCounterDeviceImp(outer: BoomPerfCounterDevice) extends LazyModuleImp(outer) {
    val io = IO(new BoomPerfCounterIO)

    // Snapshot registers: when software writes bit 0 of control, latch all counters
    val snapshot = Reg(Vec(BoomPerfCounterConsts.NUM_COUNTERS, UInt(64.W)))
    val snapshotValid = RegInit(false.B)

    // Determine which values to read: snapshot if valid, else live counters
    val readValues = Wire(Vec(BoomPerfCounterConsts.NUM_COUNTERS, UInt(64.W)))
    for (i <- 0 until BoomPerfCounterConsts.NUM_COUNTERS) {
      readValues(i) := Mux(snapshotValid, snapshot(i), io.counters(i))
    }

    // Control write handler
    val controlWrite = WireDefault(0.U(64.W))
    when (controlWrite(0)) {
      // Snapshot: latch all counter values
      for (i <- 0 until BoomPerfCounterConsts.NUM_COUNTERS) {
        snapshot(i) := io.counters(i)
      }
      snapshotValid := true.B
    }
    when (controlWrite(1)) {
      // Release snapshot
      snapshotValid := false.B
    }
    when (controlWrite(2)) {
      // Dump all counters to simulation console
      val names = Seq(
        "cycles", "instret",
        "retiring", "bad_speculation", "frontend_bound", "backend_bound",
        "fetch_latency", "fetch_bandwidth", "branch_mispredict", "machine_clears",
        "memory_bound", "core_bound",
        "retired_loads", "retired_stores", "retired_branches",
        "retired_jals", "retired_jalrs", "retired_fp", "retired_amo", "retired_system",
        "rob_full", "ldq_full", "stq_full", "int_iq_full", "mem_iq_full",
        "branch_mask_full", "rename_stall", "flush_cycles", "rollback_cycles",
        "icache_miss", "dcache_miss", "dcache_release",
        "itlb_miss", "dtlb_miss", "l2tlb_miss",
        "br_mispredict", "br_resolve", "jalr_mispredict", "br_mispred_bpd", "br_mispred_btb")
      printf("===== TMA PERFORMANCE COUNTERS =====\n")
      for (i <- 0 until BoomPerfCounterConsts.NUM_COUNTERS) {
        printf(cf"  ${names(i)}%24s = ${io.counters(i)}%d\n")
      }
      printf("====================================\n")
    }

    // Build register map: control at 0x000, then counters at 0x008, 0x010, ...
    val regmapEntries = Seq(
      0x000 -> Seq(RegField(64, 0.U(64.W), RegWriteFn((valid, data) => {
        when (valid) { controlWrite := data }
        true.B
      })))
    ) ++ (0 until BoomPerfCounterConsts.NUM_COUNTERS).map { i =>
      (0x008 + i * 0x008) -> Seq(RegField.r(64, readValues(i)))
    }

    node.regmap(regmapEntries: _*)
  }
}
