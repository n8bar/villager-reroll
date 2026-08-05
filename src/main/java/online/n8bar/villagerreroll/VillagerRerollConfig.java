package online.n8bar.villagerreroll;

import net.minecraftforge.common.ForgeConfigSpec;

final class VillagerRerollConfig {
    static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.IntValue CONFIRMATION_TICKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("reroll");
        CONFIRMATION_TICKS = builder.comment("Ticks allowed between first and confirming clicks (20 ticks = 1 second).")
                .defineInRange("confirmationTicks", 200, 20, 200);
        builder.pop();
        SPEC = builder.build();
    }

    private VillagerRerollConfig() { }

    static int confirmationTicks() {
        return CONFIRMATION_TICKS.get();
    }

}
