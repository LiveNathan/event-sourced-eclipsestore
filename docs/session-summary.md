# Session Summary — BaseEventStoreTest + Nullables

## Goal

Spike on EclipseStore for event-sourced persistence, modeled on Ted Young's JitterTicket. Implement James Shore's
nullables testing pattern instead of JitterTicket's `InMemoryEventStore`.

## What we built

### 1. Nullables refactor of `EclipseStoreEventStore`

Adapter at `src/main/java/.../adapter/out/store/EclipseStoreEventStore.java`.

- Introduced a private `Storage` SPI inside the adapter with three impls:
    - `EclipseStorage` — production GigaMap-backed.
    - `InMemoryStorage` — in-process list, used by `createNull()`.
    - `ThrowingStorage` — throws a supplied `RuntimeException`, used by `createNull(StoreOptions.WithException)`.
- Factories: `create(EmbeddedStorageManager)`, `createNull()`, `createNull(StoreOptions)`.
- Pattern mirrors `showbook/.../EclipseStoreSourceDocumentEventStore`.

### 2. Domain-specific port `ShowBookEventStore`

At `src/main/java/.../application/port/ShowBookEventStore.java`.

- Extends generic `EventStore<ShowBookId, ShowBookEvent, ShowBook>`.
- Hosts static `createNull()` and `createNull(StoreOptions)` factories that delegate to the adapter.
- Owns the sealed `StoreOptions` interface with `WithException(RuntimeException)` record.
- `EclipseStoreEventStore implements ShowBookEventStore`.
- Tests now depend on the port, not the adapter — fixes the hexagonal boundary leak.

### 3. `BaseEventStoreTest`

At `src/test/java/.../application/BaseEventStoreTest.java`. Two tests, ports of JitterTicket's:

- `saveSendsEventsToMultipleSubscribedConsumers` — counter spy, expects 2 invocations.
- `subscribersOnlyReceiveDesiredEventTypes` — three `SpyConsumer` instances; triggers create + rename, asserts delete
  subscriber not invoked.

Uses `ShowBookEventStore.createNull()`. Inner `SpyConsumer` class verifies invocation + event-type filtering.

## Key design decisions

- **Nullables over test-local subclass.** Same idiom as showbook; in-memory backend serves both unit and future
  app-layer tests; keeps `BaseEventStore` (application) ignorant of test fixtures.
- **`StoreOptions` lives on the port, not the adapter.** Pragmatic deviation from strict hexagonal (port statically
  references adapter) — same tradeoff Shore makes; keeps test ergonomics.
- **`ThrowingStorage`** unused so far but available for future error-path tests.

## Project conventions (from CLAUDE.md)

- Java + Spring Boot + Maven.
- TDD; AAA test structure with blank-line separation; evident data.
- Behavioral camelCase test names: `<subject><verb><expectedOutcome>`. Never embed production method names.
- `@Nested` groups with noun-phrase or "when" names.
- AssertJ only — no Mockito. Real impls / in-memory / hand-rolled doubles.
- No comments; no test-only code in production.
- Specialized AssertJ exception matchers; chained calls one per line.

## Domain shape

- `ShowBook` aggregate, sealed `ShowBookEvent` permits `ShowBookCreated`, `ShowBookNameUpdated`, `ShowBookDeleted`.
- `Event` has a `Long eventSequence` mutable via `setEventSequence`; storage stamps on append.
- `BaseEventStore` handles subscribe/notify; adapter implements `save(id, stream)`, `eventsFor(id)`,
  `allEventsAfter(checkpoint, types)`.
- Integration test `EclipseStoreEventStoreIntegrationTest` (`@Tag("database")`) covers create/read/update/delete across
  storage restarts.

## Status

- 5 prior tests + 2 new `BaseEventStoreTest` tests; all passing as of session end.
- Branch: `main` (uncommitted: refactor of `EclipseStoreEventStore`, new `ShowBookEventStore` port,
  `BaseEventStoreTest`).

## Reference material

- `/Users/nathanlively/dev/personal/showbook/docs/info/event-sourcing-ted-young.md`
- `/Users/nathanlively/dev/personal/showbook/docs/info/gigamap-guide.md`
- `https://github.com/jitterted/jitterticket-event-sourced/`
- Showbook's nullables exemplar: `showbook/.../EclipseStoreSourceDocumentEventStore.java` and
  `.../port/SourceDocumentEventStore.java`.

## Likely next tests

- Error path using `ShowBookEventStore.createNull(new StoreOptions.WithException(...))`.
- Projection / read-model tests driven by `allEventsAfter(Checkpoint, ...)`.
- Additional aggregate behavior tests on `ShowBook` (rename twice, delete idempotency, etc.).
