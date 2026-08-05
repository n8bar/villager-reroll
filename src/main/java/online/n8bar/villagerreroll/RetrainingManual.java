package online.n8bar.villagerreroll;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

final class RetrainingManual {
    private static final String MARKER = VillagerReroll.MOD_ID + ":trade_retraining_manual";
    private RetrainingManual() { }

    static ItemStack create() {
        ItemStack stack = new ItemStack(Items.WRITABLE_BOOK);
        stampMarker(stack.getOrCreateTag());
        stack.setHoverName(Component.literal("Trade Retraining Manual").withStyle(ChatFormatting.GOLD));
        ListTag enchantments = new ListTag();
        CompoundTag glint = new CompoundTag();
        glint.putString("id", "minecraft:unbreaking");
        glint.putShort("lvl", (short) 1);
        enchantments.add(glint);
        stack.getOrCreateTag().put("Enchantments", enchantments);
        stack.getOrCreateTag().putInt("HideFlags", stack.getOrCreateTag().getInt("HideFlags") | 1);
        return stack;
    }

    static boolean isGenuine(ItemStack stack) {
        return stack.is(Items.WRITABLE_BOOK) && stack.hasTag() && hasMarker(stack.getTag());
    }

    static void stampMarker(CompoundTag tag) { tag.putBoolean(MARKER, true); }
    static boolean hasMarker(CompoundTag tag) { return tag != null && tag.getBoolean(MARKER); }

    @SubscribeEvent
    public static void addLibrarianTrade(VillagerTradesEvent event) {
        if (event.getType() != net.minecraft.world.entity.npc.VillagerProfession.LIBRARIAN) return;
        event.getTrades().get(5).add((trader, random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, 8), new ItemStack(Items.WRITABLE_BOOK), create(),
                6, 30, 0.05f));
    }
}
