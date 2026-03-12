import "DPI-C" function void tma_counter_store(input int tile_id, input int idx, input longint unsigned value);
import "DPI-C" function void tma_counter_dump_final(input int tile_id);

module SimTMACounterDump #(
    parameter NUM_COUNTERS = 57,
    parameter TILE_ID = 0
) (
    input        clock,
    input        reset,
    input [63:0] counters_0,
    input [63:0] counters_1,
    input [63:0] counters_2,
    input [63:0] counters_3,
    input [63:0] counters_4,
    input [63:0] counters_5,
    input [63:0] counters_6,
    input [63:0] counters_7,
    input [63:0] counters_8,
    input [63:0] counters_9,
    input [63:0] counters_10,
    input [63:0] counters_11,
    input [63:0] counters_12,
    input [63:0] counters_13,
    input [63:0] counters_14,
    input [63:0] counters_15,
    input [63:0] counters_16,
    input [63:0] counters_17,
    input [63:0] counters_18,
    input [63:0] counters_19,
    input [63:0] counters_20,
    input [63:0] counters_21,
    input [63:0] counters_22,
    input [63:0] counters_23,
    input [63:0] counters_24,
    input [63:0] counters_25,
    input [63:0] counters_26,
    input [63:0] counters_27,
    input [63:0] counters_28,
    input [63:0] counters_29,
    input [63:0] counters_30,
    input [63:0] counters_31,
    input [63:0] counters_32,
    input [63:0] counters_33,
    input [63:0] counters_34,
    input [63:0] counters_35,
    input [63:0] counters_36,
    input [63:0] counters_37,
    input [63:0] counters_38,
    input [63:0] counters_39,
    // L2 cache counters (40-56)
    input [63:0] counters_40,
    input [63:0] counters_41,
    input [63:0] counters_42,
    input [63:0] counters_43,
    input [63:0] counters_44,
    input [63:0] counters_45,
    input [63:0] counters_46,
    input [63:0] counters_47,
    input [63:0] counters_48,
    input [63:0] counters_49,
    input [63:0] counters_50,
    input [63:0] counters_51,
    input [63:0] counters_52,
    input [63:0] counters_53,
    input [63:0] counters_54,
    input [63:0] counters_55,
    input [63:0] counters_56
);

    reg enabled;
    wire [63:0] ctr_array [0:56];

    assign ctr_array[0]  = counters_0;
    assign ctr_array[1]  = counters_1;
    assign ctr_array[2]  = counters_2;
    assign ctr_array[3]  = counters_3;
    assign ctr_array[4]  = counters_4;
    assign ctr_array[5]  = counters_5;
    assign ctr_array[6]  = counters_6;
    assign ctr_array[7]  = counters_7;
    assign ctr_array[8]  = counters_8;
    assign ctr_array[9]  = counters_9;
    assign ctr_array[10] = counters_10;
    assign ctr_array[11] = counters_11;
    assign ctr_array[12] = counters_12;
    assign ctr_array[13] = counters_13;
    assign ctr_array[14] = counters_14;
    assign ctr_array[15] = counters_15;
    assign ctr_array[16] = counters_16;
    assign ctr_array[17] = counters_17;
    assign ctr_array[18] = counters_18;
    assign ctr_array[19] = counters_19;
    assign ctr_array[20] = counters_20;
    assign ctr_array[21] = counters_21;
    assign ctr_array[22] = counters_22;
    assign ctr_array[23] = counters_23;
    assign ctr_array[24] = counters_24;
    assign ctr_array[25] = counters_25;
    assign ctr_array[26] = counters_26;
    assign ctr_array[27] = counters_27;
    assign ctr_array[28] = counters_28;
    assign ctr_array[29] = counters_29;
    assign ctr_array[30] = counters_30;
    assign ctr_array[31] = counters_31;
    assign ctr_array[32] = counters_32;
    assign ctr_array[33] = counters_33;
    assign ctr_array[34] = counters_34;
    assign ctr_array[35] = counters_35;
    assign ctr_array[36] = counters_36;
    assign ctr_array[37] = counters_37;
    assign ctr_array[38] = counters_38;
    assign ctr_array[39] = counters_39;
    // L2 cache counters
    assign ctr_array[40] = counters_40;
    assign ctr_array[41] = counters_41;
    assign ctr_array[42] = counters_42;
    assign ctr_array[43] = counters_43;
    assign ctr_array[44] = counters_44;
    assign ctr_array[45] = counters_45;
    assign ctr_array[46] = counters_46;
    assign ctr_array[47] = counters_47;
    assign ctr_array[48] = counters_48;
    assign ctr_array[49] = counters_49;
    assign ctr_array[50] = counters_50;
    assign ctr_array[51] = counters_51;
    assign ctr_array[52] = counters_52;
    assign ctr_array[53] = counters_53;
    assign ctr_array[54] = counters_54;
    assign ctr_array[55] = counters_55;
    assign ctr_array[56] = counters_56;

    initial begin
        enabled = $test$plusargs("dump-tma-counters");
    end

    integer i;
    always @(posedge clock) begin
        if (!reset && enabled) begin
            for (i = 0; i < NUM_COUNTERS; i = i + 1) begin
                tma_counter_store(TILE_ID, i, ctr_array[i]);
            end
        end
    end

    final begin
        if (enabled) begin
            tma_counter_dump_final(TILE_ID);
        end
    end

endmodule
