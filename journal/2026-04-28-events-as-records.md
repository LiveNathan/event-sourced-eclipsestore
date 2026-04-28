# 2026-04-28 — Events as Records + Sealed Interface

## Context

The original event hierarchy followed JitterTicket's pattern: an abstract `Event` class with a mutable `eventSequence` (
set by the store after construction), an abstract sealed `ShowBookEvent` extending it, and three concrete subclasses
with hand-rolled `equals` / `hashCode` / `toString`. About 120 lines of ceremony across the three event files.

Spike question: would modern Java records + a sealed interface be a real improvement, or just a stylistic swap?

## Change

- `Event` → `interface Event { Long eventSequence(); }`
- `ShowBookEvent` → `sealed interface` permitting the three records, exposing `showBookId()` and a `withSequence(Long)`
  wither.
- `ShowBookCreated`, `ShowBookNameUpdated`, `ShowBookDeleted` → `record`s implementing `ShowBookEvent`. Each implements
  `withSequence` by returning a new instance with the sequence filled in.
- `EclipseStoreEventStore` (both `EclipseStorage` and `InMemoryStorage`) replaced `event.setEventSequence(seq)` with
  `uncommittedEvents.map(e -> e.withSequence(seq))`, then stored the resulting list.
- `StubEvent` in `EventSourcedAggregateTest` became a record.
- `Event.setEventSequence(...)` deleted entirely — events are now genuinely immutable.

## Contrast with JitterTicket

JitterTicket's pattern works in plain Java: shared state (sequence, aggregate id) lives on a base class, the store
mutates the sequence post-hoc, and each subclass overrides `equals`/`hashCode`. That's the path of least resistance when
the language gives you classes and not much else.

The records + sealed interface variant trades a *single* small thing — a per-record `withSequence` method — for several
wins:

- ~120 lines of `equals`/`hashCode`/`toString` deleted.
- No `setEventSequence` setter anywhere; events are truly immutable values.
- The `apply(...)` switch in `ShowBook` is now exhaustive over the sealed permits — adding a fourth event type without
  updating `apply` is a compile error, not a runtime surprise.
- Shared accessors (`showBookId()`, `eventSequence()`) live as interface methods that each record satisfies
  automatically because the components match the names.

The wither cost is real but trivial at this scale: three one-line methods. A library (Derive4J, record-builder) would be
overkill. JEP 468's "with" expressions, when they exit preview, would let the records drop the wither entirely — but
enabling preview just for that isn't worth it today.

## Surprise

I expected this to be a half-day of fiddling with constructor arity, EclipseStore serialization quirks, and test
fallout. It was a 15-minute mechanical change: rewrite five files, update two store methods, swap one test stub. Tests
passed first try.

Two reasons it went smoothly, in retrospect:

1. The existing design already separated "construct event" from "assign sequence" — the store was the only thing calling
   `setEventSequence`. The wither slotted in cleanly because the seam was already in the right place.
2. The records' canonical constructor `(showBookId, eventSequence, name)` is a structural superset of what the secondary
   constructor `(showBookId, name)` needed — the latter just delegates with `null`. No call sites broke.

The lesson: when a refactor feels suspiciously cheap, it usually means the prior design had already done the hard
structural work. The mutable `eventSequence` setter wasn't a deep architectural commitment — it was a small concession
to "records didn't exist when this pattern was written." Removing it cost almost nothing because nothing else depended
on it being mutable.

## Not done

- Commands aren't yet records — they're not modeled as types in this spike at all (commands are method calls on the
  aggregate). If a future iteration introduces explicit command objects, they should follow the same pattern.
- The `Event` interface is non-sealed. Sealing it would require listing every aggregate's event type as a permit, which
  couples the domain root to every aggregate. Left open intentionally.
