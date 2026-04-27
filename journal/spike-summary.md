# Event-Sourced EclipseStore Spike — Implementation Summary

A small spike showing event-sourced persistence with EclipseStore + GigaMap, modeled after Ted
Young's [JitterTicket](https://github.com/jitterted/jitterticket-event-sourced) but adapted for EclipseStore's POJO
persistence model (no JDBC/JSON/DBO/DTO scaffolding).

## Architecture

Hexagonal: `application/port/EventStore` is the port; `adapter/out/store/EclipseStoreEventStore` is the adapter.

```
domain/                          domain logic (no infra)
  Event                          base class, mutable eventSequence (set on save)
  EventSourcedAggregate          enqueue/applyAll/uncommittedEvents
  Id                             marker interface, exposes UUID
  showbook/
    ShowBook                     aggregate (create/reconstitute/apply)
    ShowBookEvent (sealed)       permits ShowBookCreated, ShowBookNameUpdated
    ShowBookId                   record(UUID)

application/
  port/EventStore                port interface
  BaseEventStore                 abstract — owns subscriber registry,
                                 findById/save(aggregate)/notify orchestration
  Checkpoint                     value object for sequence cursors
  EventStreamConsumer            functional interface for subscribers

adapter/out/store/
  DataRoot                       EclipseStore root: GigaMap<ShowBookEvent>
                                 + sequenceCounter + DataRoot.from(sm)
  EclipseStoreEventStore         concrete BaseEventStore<ShowBookId,...>
  StorageConfig                  Spring beans: DataRoot, EclipseStoreEventStore
```

## Key design decisions

1. **Concrete, not generic, store.** `EclipseStoreEventStore` is bound to `<ShowBookId, ShowBookEvent, ShowBook>` —
   simpler for a one-aggregate spike. Re-genericize later if a second aggregate appears.

2. **POJO persistence — no JSON.** `GigaMap<ShowBookEvent>` stores events as objects; EclipseStore handles
   serialization. The JitterTicket `EventDbo`/`EventDto` indirection was removed.

3. **Bitmap index on aggregate id.** `DataRoot.SHOW_BOOK_ID_INDEX` is a `BinaryIndexerUUID<ShowBookEvent>` keyed on
   `showBookId().id()`. Queries: `events.query(SHOW_BOOK_ID_INDEX.is(uuid)).toList()`.

4. **Sort in Java, not in the index.** Bitmap indexes don't preserve order. `eventsFor` sorts the result list by
   `eventSequence` after the query. JDBC handles ordering via `ORDER BY`; EclipseStore doesn't, so we do it client-side.

5. **Sequence counter on `DataRoot`.** Mirrors the showbook-project pattern: `dataRoot.nextSequence()` returns a
   monotonic `long`. The counter is part of the persisted root, so it survives JVM restarts.
   `EclipseStoreEventStore.save` calls `dataRoot.nextSequence()` per event, then persists `dataRoot` (which transitively
   saves the counter and the GigaMap).

6. **Single resolver: `DataRoot.from(EmbeddedStorageManager)`.** Idempotent — returns the existing root if one is
   attached, otherwise creates + sets + stores. Both `StorageConfig.dataRoot()` (Spring bean) and
   `EclipseStoreEventStore.create(sm)` (test factory) delegate to it. Tests bypass Spring and use the static factory.

## Persistence flow

**save**

```
EclipseStoreEventStore.save(showBookId, eventStream)
  → for each event: setEventSequence(dataRoot.nextSequence())
  → events.addAll(toSave)
  → events.store()
  → storageManager.store(dataRoot)   // captures counter + map
```

**findById**

```
BaseEventStore.findById(id)
  → eventsFor(id) = query SHOW_BOOK_ID_INDEX.is(id.id()), sort by eventSequence
  → if non-empty: ShowBook.reconstitute(events) → applyAll → return Optional
```

**allEventsAfter(checkpoint, eventTypes)**

```
GigaIterator inside try-with-resources, filter by sequence > checkpoint
and (eventTypes empty OR contains event.getClass()), sort, return stream
```

## Configuration

`application.properties`:

```
org.eclipse.store.storage-directory=${user.home}/.event-sourced-eclipsestore/storage
org.eclipse.store.root=dev.nathanlively.event_sourced_eclipsestore.adapter.out.store.DataRoot
```

Spring Boot integration auto-instantiates the configured root class on startup.

## Build / test invocation

EclipseStore on Java 26 needs JDK-internal access. Pass JVM flags to surefire:

```
./mvnw test -DargLine="-ea --add-exports java.base/jdk.internal.misc=ALL-UNNAMED"
```

To bake this into the project, add to `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>--add-exports java.base/jdk.internal.misc=ALL-UNNAMED</argLine>
    </configuration>
</plugin>
```

(Not yet committed — user interrupted.)

## Tests

### Existing (passing)

- `EventSourcedAggregateTest` (domain — pre-existing)
- `ShowBookTest` (domain — pre-existing)
- `EclipseStoreEventStoreIntegrationTest.persistedShowBookIsRetrievableAfterStorageRestart` — the round-trip integration
  test (save → close storage → restart → findById). **Tagged `@Tag("database")`.** Uses `@TempDir` +
  `EmbeddedStorage.start(storageDir)` + `EclipseStoreEventStore.create(storageManager)`, no Spring.

### Suggested next tests, ordered by value

1. **Sequence assignment** — save two aggregates, assert their first events have sequences `1L, 2L`. Catches
   `nextSequence()` wiring.
2. **`findById` returns empty for unknown id** — null-path contract.
3. **`eventsForAggregate` returns events sorted by sequence** — guards the in-memory sort.
4. **`allEventsAfter(checkpoint, types)`** — populate, query with checkpoint, assert filter behavior on both sequence
   and event class.
5. **Subscriber notification** — register an `EventStreamConsumer` via `subscribe()`, save aggregate, assert the
   consumer was called with the expected events.

Pattern to follow:
`/Users/nathanlively/dev/personal/showbook/src/test/java/dev/nathanlively/adapter/out/persistence/EclipseStoreSourceDocumentEventStoreIntegrationTest.java`.

## Open items / known limitations

- Surefire `argLine` not committed to `pom.xml`; tests must be invoked with `-DargLine=...` until then.
- `Event.eventSequence` is mutable. The store mutates it in place during `save`. JitterTicket does the same.
- No concurrency guarantees beyond `synchronized` on `DataRoot.nextSequence()`. Multi-process writers would need
  external coordination. The showbook project uses an `@Write` proxy in production for serialization; not implemented
  here.
- `EclipseStoreEventStore` is concrete (bound to ShowBook). If a second aggregate is added, either (a) re-genericize
  with a per-aggregate factory, or (b) add a second concrete event store + a second GigaMap on `DataRoot`.
- `BaseEventStore.subscribe(...)` requires a `Set<Class<? extends Event>>` of desired types — the consumer must declare
  what it wants. The "evolved projector" pattern from the docs (`EventHandler` + reflection-based discovery) is not
  implemented.

## Reference docs (read-only context)

- `/Users/nathanlively/dev/personal/showbook/docs/info/event-sourcing-ted-young.md` — domain/architecture guide
- `/Users/nathanlively/dev/personal/showbook/docs/info/gigamap-guide.md` — GigaMap API cheat sheet
- `/Users/nathanlively/dev/personal/showbook/src/main/java/dev/nathanlively/adapter/out/persistence/DataRoot.java` —
  production reference for DataRoot patterns
- `/Users/nathanlively/dev/personal/showbook/src/test/java/dev/nathanlively/adapter/out/persistence/` — production test
  patterns
