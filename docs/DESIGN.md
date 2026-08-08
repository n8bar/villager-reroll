# Design specification

This document describes the implemented MVP and the remaining test boundary.

## Components

### Policy

There is no gameplay config. Payment is exactly one server-authenticated Trade Retraining Manual.
The crafting recipe remains eight emeralds around one writable book. Master librarians add one
Manual trade at a base cost of twelve emeralds with no second ingredient, six uses, 30 villager XP,
and a 0.05 price multiplier so normal reputation discounts and demand remain in force.

### Trade pool capture

Subscribe to Forge's `VillagerTradesEvent` and copy each profession's tiered `ItemListing` pools after
other mods have contributed. Keep immutable server-lifecycle snapshots keyed by profession and tier.
Clear/rebuild them on server resource lifecycle events as Forge behavior requires. Do not hard-code
vanilla professions or offer contents.

At `ServerStarting`, snapshot the finalized ordinary and rare wandering-trader arrays after Forge's
trade event contributions. A reroll uniformly chooses up to five distinct ordinary listings and one
rare listing, invokes each exactly once, and does not retry null results. When the ordinary pool has
five or fewer entries, vanilla uses every entry in original order without a selection RNG draw; a
larger pool repeats full-length draws into a `HashSet` until five indices are distinct. Only after
those listings run does the rare pool consume one full-length draw. A throwing listing aborts free.

### Offer builder

Given a server-side villager, read its profession and current level. For every tier `1..level`:

1. obtain that tier's captured listings;
2. randomize candidates with the villager/server random source;
3. invoke listings until two distinct candidate slots yield non-null `MerchantOffer`s;
4. collect exactly two offers in tier order.

Build the entire local plan once inside the canceled attack event. For short, null, or throwing tiers, the plan
preserves serialized-equivalent copies of the old offers. A versioned ledger proves boundaries with
profession, level, tier counts, and structural signatures that omit uses, demand, and special price.
Legacy positional inference is
allowed only for vanilla professions when the exact offer total equals the chronological sum of
`min(2, current pool size)` for every unlocked tier; ambiguous partial splices refuse without charge.
Level changes invalidate the ledger rather than guessing at appended boundaries; a subsequent full
reroll may safely establish a new ledger, while a partial reroll must pass the legacy proof.

### Attack gate

Subscribe to `AttackEntityEvent` at `HIGHEST`. For every genuine-Manual attack against any entity,
cancel before any validation. Forge posts this from `ForgeHooks.onPlayerAttackTarget` before `Player#attack`,
so the handled path cannot reach vanilla damage, knockback, crit/sweep/fire, durability, exhaustion,
stats, panic/gossip, or golem anger. Sneaking continues into the transaction; without it, no payment
or retraining occurs and the player gets a brief instruction. Ordinary items pass through unchanged;
unsupported Manual targets remain harmless and do nothing further.

### Atomic transaction

All work runs on the logical server thread:

1. acquire a short-lived reservation keyed by villager UUID;
2. repeat every eligibility, proximity, identity, and payment check;
3. validate the local replacement `MerchantOffers` and full old-offer fingerprint;
4. post a cancellable pre-reroll event containing player, villager, price, and proposed offers;
5. remove the exact payment from the player's inventory, unless an explicitly designed Creative-mode
   exemption is added later;
6. replace only the villager's offers and notify normal entity/container synchronization;
7. release reservation and emit success feedback.

If payment removal or offer replacement cannot complete, preserve the old offer object and return or
avoid consuming payment. The implementation must include failure-injection tests around this boundary.

Wandering traders use the same reservation, revalidation, event isolation, freshness, and payment
boundary. Their whole proposal is built off-side, then the existing `MerchantOffers` is cleared and
filled only at commit because this Minecraft version has no functional public offer setter. Rollback
restores a deep copy of the old serialized offers. No other entity field is touched.

## Event and side rules

Filter logical client work after canceling every genuine-Manual attack. `displayTest="IGNORE_ALL_VERSION"`
is Forge 47.2.0's server-only compatibility setting and allows clients without the mod to connect;
gameplay code must not reference client-only classes or register a network channel.

## Test matrix before calling it implemented

- Sneaking and non-sneaking Manual clicks, wrong villager, wrong dimension, and insufficient payment.
- Novice through Master villagers: exactly 2/4/6/8/10 offers after reroll.
- Name, UUID, profession/type/level/XP, gossip, brain, inventory, health, location, and restock fields
  byte-for-byte or semantically unchanged as appropriate; only offers differ.
- Fresh offers have zero uses and no leaked demand/special-price state from old offers.
- Null/short/throwing tiers preserve proven old tier offers; ambiguous provenance or a complete no-op
  refuses without mutation or charge.
- Two players attack the same villager in the same tick; at most one transaction commits.
- Main/off-hand event duplication cannot double-charge or double-reroll.
- Dedicated Forge server accepts a client with no Villager Reroll jar.
- No code path runs or classloads client-only Minecraft classes.
