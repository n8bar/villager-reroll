# Design specification

This document describes the implemented MVP and the remaining test boundary.

## Components

### Configuration

`VillagerRerollConfig` defines a per-world server config with:

- `confirmationTicks`, default and hard maximum `200` unless later design work justifies otherwise;
- optional interaction range and feedback controls with conservative bounds.

Payment is fixed policy: one server-authenticated Trade Retraining Manual.

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

Build the entire plan on the first click and cache it. For short, null, or throwing tiers, the plan
preserves serialized-equivalent copies of the old offers. A versioned ledger proves boundaries with
profession, level, tier counts, and structural signatures that omit uses, demand, and special price.
Legacy positional inference is
allowed only for vanilla professions when the exact offer total equals the chronological sum of
`min(2, current pool size)` for every unlocked tier; ambiguous partial splices refuse without charge.
Level changes invalidate the ledger rather than guessing at appended boundaries; a subsequent full
reroll may safely establish a new ledger, while a partial reroll must pass the legacy proof.

### Confirmation service

Hold an in-memory prepared plan keyed by player UUID with villager UUID, dimension, full Offers NBT
fingerprint, profession, level, payment policy, and expiry. The first click consumes RNG exactly once;
the second commits that same plan only if every captured field remains fresh.

### Atomic transaction

All work runs on the logical server thread:

1. acquire a short-lived reservation keyed by villager UUID;
2. repeat every eligibility, proximity, identity, and payment check;
3. validate the cached replacement `MerchantOffers` and full old-offer fingerprint;
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
handled sneak interaction so ordinary trading remains untouched. `displayTest="IGNORE_ALL_VERSION"`
is Forge 47.2.0's server-only compatibility setting and allows clients without the mod to connect;
gameplay code must not reference client-only classes or register a network channel.

## Test matrix before calling it implemented

- First click, expiry, wrong villager, wrong dimension, insufficient payment, and changed config.
- Novice through Master villagers: exactly 2/4/6/8/10 offers after reroll.
- Name, UUID, profession/type/level/XP, gossip, brain, inventory, health, location, and restock fields
  byte-for-byte or semantically unchanged as appropriate; only offers differ.
- Fresh offers have zero uses and no leaked demand/special-price state from old offers.
- Null/short/throwing tiers preserve proven old tier offers; ambiguous provenance or a complete no-op
  refuses without mutation or charge.
- Two players confirm the same villager in the same tick; at most one transaction commits.
- Main/off-hand event duplication cannot double-charge or double-reroll.
- Restart/reload leaves no stale pending confirmation.
- Dedicated Forge server accepts a client with no Villager Reroll jar.
- No code path runs or classloads client-only Minecraft classes.
