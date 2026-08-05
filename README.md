# Villager Reroll

A public, server-only Forge mod for Minecraft 1.20.1. Players can pay to rebuild
a villager's unlocked trade offers without replacing the villager or installing anything client-side.

> **Current status:** 0.2.0 development candidate. The earlier 0.1 live test passed; these partial-tier
> and Trade Retraining Manual changes are not deployed.

## Player experience

1. Craft a genuine **Trade Retraining Manual** from eight emeralds surrounding one Book and Quill.
   The result carries a survival-unforgeable namespaced marker, gold name, and hidden-enchantment glint; renaming
   an ordinary book cannot counterfeit it. Master librarians also sell one for eight emeralds plus
   one Book and Quill (six uses, 30 villager XP, normal restocking).
2. Hold the manual and sneak-right-click an eligible villager. The server prepares the exact offers
   once, names preserved tiers, price, and timeout, and caches that plan without taking payment.
3. Sneak-right-click that **same villager** again within **10 seconds (200 server ticks)**.
4. If every condition still passes, the server consumes one genuine manual and commits the cached
   plan. Tiers unable to produce two valid offers keep deep-copied, serialized-equivalent old trades.
   Success gets restrained chat/sound feedback. Expiry or any failed check cancels without charge.

Normal right-click remains normal trading. Confirmation is bound to player UUID + villager UUID, so
clicking a different villager starts that villager's own confirmation instead of approving the first.

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

## Safeguards

- Dedicated-server authority only; no packet, screen, keybind, model, or client installation.
- Handle the Forge entity-interaction event once for the main hand and only while sneaking.
- Require an adult `Villager`, a real profession (not unemployed or nitwit), and at least one unlocked
  tier. Reject dead/removed villagers and villagers already trading with another player.
- On confirmation, re-check same player, same villager UUID, deadline, dimension, range, line of
  sight, eligibility, offer sources, and payment. Never trust the first click's state.
- Reserve a villager during the final server-thread transaction so two players cannot both pay for
  the same reroll. Consume only after a complete replacement offer list has been built in memory.
- Clear pending confirmations on expiry, logout, death, and dimension change. Do not save transient
  confirmations to world/player data.
- Use server config validation, bounded counts, registry-backed item parsing, and restrained logging.
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
- [x] Per-world confirmation timeout and marker-authenticated fixed Manual payment policy
- [x] Final vanilla+modded trade-pool snapshot and atomic offer-list builder
- [x] Two-step interaction state machine and concurrency guard
- [x] Feedback, cancellation hooks, and public cancellable pre-reroll event
- [x] Unit tests for confirmation state and shuffle boundaries
- [ ] GameTests for payment/mutation and villager-state preservation boundaries
- [ ] Dedicated-server test with a client lacking the mod
- [ ] Compatibility tests with vanilla professions and modded trade-pool contributors
- [ ] Reobfuscated release jar, changelog, and public repository release

## Contributing

Issues and pull requests are welcome. Until the client and GameTest rows above pass, treat this as an
MVP candidate rather than a production release.
