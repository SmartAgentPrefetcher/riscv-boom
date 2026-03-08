#include <cstdio>
#include <cstdint>

#define MAX_TILES 8
#define NUM_COUNTERS 40

static uint64_t last_counters[MAX_TILES][NUM_COUNTERS];

static const char* counter_names[NUM_COUNTERS] = {
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
    "br_mispredict", "br_resolve", "jalr_mispredict", "br_mispred_bpd", "br_mispred_btb"
};

extern "C" void tma_counter_store(int tile_id, int idx, uint64_t value) {
    if (tile_id >= 0 && tile_id < MAX_TILES && idx >= 0 && idx < NUM_COUNTERS) {
        last_counters[tile_id][idx] = value;
    }
}

extern "C" void tma_counter_dump_final(int tile_id) {
    if (tile_id < 0 || tile_id >= MAX_TILES) return;
    fprintf(stderr, "===== TMA PERFORMANCE COUNTERS (tile %d) =====\n", tile_id);
    for (int i = 0; i < NUM_COUNTERS; i++) {
        fprintf(stderr, "  %-24s = %20lu\n", counter_names[i], last_counters[tile_id][i]);
    }
    fprintf(stderr, "==============================================\n");
}
