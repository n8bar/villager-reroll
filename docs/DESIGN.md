# Design specification

This document describes intended behavior. No class beyond the minimal mod entry point implements it
yet.

## Components

### Configuration

`VillagerRerollConfig` will define a server config with:

- `paymentItem` registry id, default `minecraft:emerald_block`;
- `paymentCount`, default `1`, bounded to a practical stack-sized range;
- `confirmationTicks`, default and hard maximum `200` unless later design work justifies otherwise;
- optional interaction range and feedback controls with conservative bounds.

Configuration must resolve the item through Forge registries and reject air, missing ids, nonpositive
counts, or a count larger than the chosen item's stack limit.

### Trade pool capture

Subscribe to Forge's `VillagerTradesEvent` and copy each profession's tiered `ItemListing` pools after
other mods have contributed. Keep immutable server-lifecycle snapshots keyed by profession and tier.
Clear/rebuild them on server resource lifecycle events as Forge behavior requires. Do not hard-code
vanilla professions or offer contents.

### Offer builder

Given a server-side villager, read its profession and current level. For every tier `1..level`:

1. obtain that tier's captured listings;
2. randomize candidates with the villager/server random source;
3. invoke listings until two distinct candidate slots yield non-null `MerchantOffer`s;
4. collect exactly two offers in tier order.

Build the entire `MerchantOffers` value off to the side. If any tier lacks enough valid listings or
returns fewer than two offers, fail without charging or mutating. A future implementation must define
whether two different listings producing identical visible offers are acceptable; the safe initial
policy is to require different listing entries, not attempt brittle ItemStack-level deduplication.

### Confirmation service

Hold an in-memory map keyed by player UUID with villager UUID, dimension key, and expiry game time.
The first valid sneak/main-hand interaction creates or replaces that entry and returns a confirmation
message. The second matching interaction inside 200 ticks enters the transaction. Expired or invalid
entries are removed eagerly and by a bounded periodic sweep.

### Atomic transaction

All work runs on the logical server thread:

1. acquire a short-lived reservation keyed by villager UUID;
2. repeat every eligibility, proximity, identity, and payment check;
3. build the complete replacement `MerchantOffers`;
4. post a cancellable pre-reroll event containing player, villager, price, and proposed offers;
5. remove the exact payment from the player's inventory, unless an explicitly designed Creative-mode
   exemption is added later;
6. replace only the villager's offers and notify normal entity/container synchronization;
7. release reservation and emit success feedback.

If payment removal or offer replacement cannot complete, preserve the old offer object and return or
avoid consuming payment. The implementation must include failure-injection tests around this boundary.

## Event and side rules

The likely entry point is Forge's player/entity interaction event. Filter logical client calls,
off-hand duplicates, non-sneaking interactions, and non-villagers before doing work. Cancel only the
handled sneak interaction so ordinary trading remains untouched. `displayTest="IGNORE_SERVER_VERSION"`
allows clients without the mod to connect; gameplay code must not reference client-only classes.

## Test matrix before calling it implemented

- First click, expiry, wrong villager, wrong dimension, insufficient payment, and changed config.
- Novice through Master villagers: exactly 2/4/6/8/10 offers after reroll.
- Name, UUID, profession/type/level/XP, gossip, brain, inventory, health, location, and restock fields
  byte-for-byte or semantically unchanged as appropriate; only offers differ.
- Fresh offers have zero uses and no leaked demand/special-price state from old offers.
- Null/short/modded trade pools abort without mutation or charge.
- Two players confirm the same villager in the same tick; at most one transaction commits.
- Main/off-hand event duplication cannot double-charge or double-reroll.
- Restart/reload leaves no stale pending confirmation.
- Dedicated Forge server accepts a client with no Villager Reroll jar.
- No code path runs or classloads client-only Minecraft classes.
