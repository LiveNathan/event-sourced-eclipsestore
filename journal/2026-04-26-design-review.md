# 2026-04-26 — Design Review & Refactor

## Context

Design review of the event-sourced EclipseStore spike against three references:

- Ted Young's JitterTicket event sourcing patterns (
  `/Users/nathanlively/dev/personal/showbook/docs/info/event-sourcing-ted-young.md`)
- EclipseStore GigaMap idioms (`/Users/nathanlively/dev/personal/showbook/docs/info/gigamap-guide.md`)
- James Shore's Nullables pattern (NotebookLM "Nullables Livestream Series", id `0373b786-7096-444c-a1e8-713f61835f8b`)

Started with 15 passing tests, ended with 25.

## Findings raised in review (full list)

1. **`ShowBook.create(name)` invented its own ID** → changed to `create(ShowBookId, String)`. Use cases need to know the
   ID before save; tests want evident IDs.
2. **`BaseEventStore.save()` never cleared `uncommittedEvents`** → would re-emit events on second save. Fixed via
   `EventSourcedAggregate.markEventsCommitted()`.
3. **No deletion guard on `rename`/`delete`** → user added `IllegalStateException` guards plus `validateName` for
   null/blank names.
4. **`setId()` was public** → made `protected` (note: user later reverted to public; intentional, left alone).
5. **Test coverage gaps** in `ShowBookTest` (rename/delete commands, name-updated/deleted projections, guard clauses) →
   user filled these in.
6. **`Event.allConcreteImplementationsOf()` was dead code** → deleted.
7. **`ShowBookFactory` had "customer name" leftover** from JitterTicket copy-paste → fixed.
8. **`allEventsAfter` did a full GigaMap scan** → added `EVENT_SEQUENCE_INDEX` (`IndexerLong`) on `DataRoot` and
   switched to `.greaterThan(checkpoint.value())`.
9. **Nullables boundary wrong**: had four layers (`EventStore` port + `ShowBookEventStore` port + `BaseEventStore`
   abstract + `EclipseStoreEventStore` adapter), with the intermediate port statically referencing the adapter. *
   *Decision**: keep `EventStore<>` (generic application port → preserves hexagonal), drop `ShowBookEventStore`, put
   `createNull()` and `StoreOptions` directly on `EclipseStoreEventStore`. Tests depend on the concrete adapter — that
   IS Shore's nullable boundary.
10. **`save(ID, Stream)` was on the public `EventStore` interface** with a comment saying "does NOT notify
    subscribers" → leak. Moved to `protected abstract append(...)` on `BaseEventStore`. Public surface is now only
    `save(aggregate)`.

## Items skipped (user's call)

- Test naming nits (`findByIdReturnsUploadedDocument` etc. embed production method names — flagged but not changed).
- OutputTracker for subscriber assertions — current `SpyConsumer` / `AtomicInteger` in `BaseEventStoreTest` is clearer
  for two tests.
- Verbose `equals/hashCode/toString` on each event class.
- `Id` marker interface — over-abstracted for one aggregate but kept.

## Final architecture

```
domain/
  Event                          base; mutable eventSequence
  EventSourcedAggregate          enqueue/applyAll/uncommittedEvents/markEventsCommitted
  Id                             marker interface, exposes UUID
  showbook/
    ShowBook                     create(ShowBookId, String); rename/delete with guards
    ShowBookEvent (sealed)       permits Created/NameUpdated/Deleted
    ShowBookId                   record(UUID)

application/
  port/EventStore                only: save(aggregate), findById, eventsForAggregate,
                                 subscribe, allEventsAfter
  BaseEventStore                 abstract — owns subscriber registry +
                                 protected abstract append(id, stream)
                                 save(aggregate) → append → markEventsCommitted → notify
  Checkpoint, EventStreamConsumer

adapter/out/store/
  DataRoot                       SHOW_BOOK_ID_INDEX (BinaryIndexerUUID)
                                 EVENT_SEQUENCE_INDEX (IndexerLong)
                                 sequenceCounter, GigaMap<ShowBookEvent>
  EclipseStoreEventStore         extends BaseEventStore<ShowBookId, ShowBookEvent, ShowBook>
                                 hosts: createNull(), createNull(StoreOptions), StoreOptions
                                 strategies: EclipseStorage, InMemoryStorage, ThrowingStorage
  StorageConfig                  Spring beans
```

## Test status: 25/25 green

- `ApplicationTests` (1)
- `EclipseStoreEventStoreIntegrationTest` (1, `@Tag("database")`)
- `EclipseStoreEventStoreTest` Retrieval (1) + Nullability (3)
- `BaseEventStoreTest` (2)
- `CheckpointTest` (4)
- `EventSourcedAggregateTest` (1)
- `ShowBookTest` CommandsGenerateEvents (3) + EventsProjectState (3) + GuardClauses (6)

## Conventions in force (reminder)

- TDD; AAA with blank-line separation; evident data
- Behavioral camelCase test names: `<subject><verb><expectedOutcome>` — never embed production method names
- `@Nested` groups, noun-phrase or "when" names
- AssertJ only, no Mockito; specialized exception matchers; chained calls one per line
- Custom AssertJ classes (e.g. `ShowBookAssert`) for domain assertions
- No comments; meaningful names; no test-only code in production

## Suggested next tests (ordered by value)

1. **Sequence assignment across saves** — save two aggregates, assert their `ShowBookCreated` events have sequences
   `1L, 2L`. Catches `nextSequence()` wiring.
2. **`allEventsAfter(checkpoint, types)`** — populate, query with non-INITIAL checkpoint, assert filter behavior on both
   sequence cursor AND event class.
3. **`allEventsAfter(INITIAL, emptySet)`** — returns every event in sequence order (validates the new
   `EVENT_SEQUENCE_INDEX`).
4. **Re-saving the same aggregate doesn't double-emit** — save, mutate (rename), save again; assert exactly one
   `ShowBookCreated` and one `ShowBookNameUpdated` exist via `eventsForAggregate`. This locks in the
   `markEventsCommitted` fix.
5. **`eventsForAggregate(unknownId)` returns empty list** — null-path contract.
6. **Subscriber receives events in sequence order** when a single save persists multiple events.
7. **Error-path tests using `createNull(WithException)`** for `eventsForAggregate` and `allEventsAfter` (
   forcedErrorThrowsOnSave/FindById already covered).

## Known limitations / open items

- `EVENT_SEQUENCE_INDEX` was added to `DataRoot` after disk format settled. Existing storage at
  `~/.event-sourced-eclipsestore/storage` won't have the index built — wipe it before next prod run.
- `Event.eventSequence` is mutable (set in-place during save). JitterTicket does the same. Acceptable for spike.
- `DataRoot.nextSequence()` uses `synchronized`; multi-process writers would need external coordination.
- No `EventHandler` reflection-based projector discovery yet (the "evolved projector" pattern from Young's doc).
- Single-aggregate spike — `EclipseStoreEventStore` is concrete-bound to ShowBook. Re-genericize when a second aggregate
  appears.

## Reference docs

- `/Users/nathanlively/dev/personal/showbook/docs/info/event-sourcing-ted-young.md`
- `/Users/nathanlively/dev/personal/showbook/docs/info/gigamap-guide.md`
- NotebookLM: Nullables Livestream Series (`0373b786-7096-444c-a1e8-713f61835f8b`)
- https://github.com/jitterted/jitterticket-event-sourced/
