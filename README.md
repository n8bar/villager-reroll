# Villager Reroll

A public, server-only Forge mod for Minecraft 1.20.1. Players can pay to rebuild
villager or wandering-trader offers without replacing the trader or installing anything client-side.

> **Current status:** 0.4.0 development candidate. Live deployment and acceptance status are tracked
> separately from this source README.

## Player experience

1. Craft a genuine **Trade Retraining Manual** from eight emeralds surrounding one Book and Quill.
   The result carries a survival-unforgeable namespaced marker, gold name, and hidden-enchantment glint; renaming
   an ordinary book cannot counterfeit it. Master librarians also sell one for a base price of
   twelve emeralds only (six uses, 30 villager XP, normal discounts, demand, and restocking).
2. Hold the Manual, sneak, and **left-click once** on an adult employed villager or wandering trader.
   Forge cancels every attack made with a genuine Manual at its earliest hook, before
   damage, knockback, crits, sweep, fire, durability, exhaustion,
   statistics, villager panic/gossip, or golem anger.
3. The server builds one local plan, validates it before and after the public event, and spends one
   Manual only when at least one trade changes. Unswappable tiers keep serialized-equivalent trades.
   Success gets restrained chat/sound feedback. Any failed check cancels without charge.

Normal right-click remains trading and ordinary items retain vanilla attacks. A genuine Manual is
harmless against every entity. A non-sneaking Manual left-click on a supported trader is canceled
without damage, payment, or retraining and reminds the player to sneak.

## Exact state semantics

The existing villager entity remains aboard. A successful reroll preserves its UUID, position,
rotation, dimension, custom name, profession, biome type, villager level, villager XP, health,
effects, equipment, age, inventory, gossip/reputation, brain/memories, job site, home, restock data,
persistence, leash/passenger state, and every other field outside its merchant-offer list.

Only `Villager#getOffers()` is replaced. Every rerolled offer is discarded, including its ingredients,
result, uses, maximum uses, demand, special price, price multiplier, merchant XP reward, and
reward-XP flag. New offers begin with their freshly generated vanilla/modded values and zero uses.
Player reputation and gossip stay on the villager and may still affect prices through normal game
logic; old offer-local discounts and demand do not survive.

The replacement contains two newly generated offers for every safely rerollable unlocked tier.
Short, null, or throwing tiers preserve their serialized offers and ordering. A versioned ledger
records tier counts and structural signatures while ignoring mutable restock fields. Legacy villagers
are spliced only when vanilla chronological boundaries are provable from profession, level, exact
offer total, and current tier caps; ambiguous layouts refuse without payment.
The new Manual trade makes the current Master-librarian cap 10. Pre-0.2 nine-offer Masters therefore
cannot use legacy partial inference against that new cap; they may still take a safe full reroll,
which establishes the ledger, but any partial failure refuses until provenance is proven.

A wandering trader reroll replaces its complete offer list from the finalized Forge pools using
vanilla's shape: up to five distinct ordinary listings and one rare listing, each invoked once.
Null or throwing listings are not retried. Only the existing offer list changes; UUID, health,
position, inventory, despawn delay, wander target, leash and llama relationships, and all other
entity state remain on the same trader.

## Safeguards

- Dedicated-server authority only; no packet, screen, keybind, model, or client installation.
- Handle Forge `AttackEntityEvent` at highest priority and cancel immediately for every genuine-Manual
  attack against any entity; dispatch supported transactions only when the player is sneaking.
- Require an adult `Villager`, a real profession (not unemployed or nitwit), and at least one unlocked
  tier. Reject dead/removed villagers and villagers already trading with another player.
- Re-check villager identity, profession/level, range, line of sight, eligibility, full Offers NBT,
  event proposal, and genuine Manual before charging.
- Reserve a trader during the final server-thread transaction so two players cannot both pay for
  the same reroll. Consume only after a complete replacement offer list has been built in memory.
- Use bounded counts, registry-backed item parsing, and restrained logging.
- Fire a cancellable mod event before payment/mutation so protection or economy integrations can veto.

## Compatibility target

- Minecraft 1.20.1
- Forge 47.2.0
- Java 17
- Server-only installation; vanilla or matching Forge clients do not need this jar
- Mod id: `villager_reroll`
- License: MIT

See [docs/DESIGN.md](docs/DESIGN.md) for the implementation architecture and failure boundaries.

## Build

The wrapper files come unmodified from the official Forge 1.20.1-47.2.0 MDK:

```bash
./gradlew build
```

No release should be published until the roadmap's gameplay and dedicated-server tests pass.

## Roadmap

- [x] ForgeGradle/Java 17 project scaffold and server-only compatibility metadata
- [x] Marker-authenticated fixed Manual payment policy
- [x] Final vanilla+modded trade-pool snapshot and atomic offer-list builder
- [x] One-click attack gate and concurrency guard
- [x] Feedback, cancellation hooks, and public cancellable pre-reroll event
- [x] Unit tests for attack trigger, transaction messages, Manual NBT, and shuffle boundaries
- [ ] GameTests for payment/mutation and villager-state preservation boundaries
- [ ] Dedicated-server test with a client lacking the mod
- [ ] Compatibility tests with vanilla professions and modded trade-pool contributors
- [ ] Reobfuscated release jar, changelog, and public repository release

## Contributing

Issues and pull requests are welcome. Until the client and GameTest rows above pass, treat this as an
MVP candidate rather than a production release.
