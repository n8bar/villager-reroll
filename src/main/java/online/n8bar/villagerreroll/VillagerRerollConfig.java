package online.n8bar.villagerreroll;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

final class VillagerRerollConfig {
    static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<String> PAYMENT_ITEM;
    private static final ForgeConfigSpec.IntValue PAYMENT_COUNT;
    private static final ForgeConfigSpec.IntValue CONFIRMATION_TICKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("reroll");
        PAYMENT_ITEM = builder.comment("Registry id of the item held and consumed for a reroll.")
                .define("paymentItem", "minecraft:emerald_block", VillagerRerollConfig::validItemSyntax);
        PAYMENT_COUNT = builder.comment("Items consumed from the player's main hand.")
                .defineInRange("paymentCount", 1, 1, 64);
        CONFIRMATION_TICKS = builder.comment("Ticks allowed between first and confirming clicks (20 ticks = 1 second).")
                .defineInRange("confirmationTicks", 200, 20, 200);
        builder.pop();
        SPEC = builder.build();
    }

    private VillagerRerollConfig() { }

    static Item paymentItem() {
        ResourceLocation id = ResourceLocation.tryParse(PAYMENT_ITEM.get());
        if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) {
            return Items.AIR;
        }
        return ForgeRegistries.ITEMS.getValue(id);
    }

    static int paymentCount() {
        Item item = paymentItem();
        int configured = PAYMENT_COUNT.get();
        return item == Items.AIR || configured > item.getMaxStackSize() ? 0 : configured;
    }

    static int confirmationTicks() {
        return CONFIRMATION_TICKS.get();
    }

    private static boolean validItemSyntax(Object value) {
        return value instanceof String text && ResourceLocation.tryParse(text) != null;
    }
}
