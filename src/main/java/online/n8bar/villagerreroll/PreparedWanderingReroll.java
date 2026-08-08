package online.n8bar.villagerreroll;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.trading.MerchantOffers;

record PreparedWanderingReroll(UUID traderId, CompoundTag oldOffersTag, MerchantOffers proposal) { }
