# Event-Sourced EclipseStore Spike

This is a spike project exploring simple event-sourced persistence using [EclipseStore](https://eclipsestore.io/).

<img width="572" height="257" alt="CleanShot 2026-04-27 at 14 03 51" src="https://github.com/user-attachments/assets/d55e1eeb-e75c-4d94-b362-c5aace51e1fa" />

## Overview

The goal of this project is to implement a lightweight Event Store using EclipseStore's object-graph persistence capabilities. It demonstrates how to store, retrieve, and query domain events while maintaining the benefits of a pure Java domain model.

This project is heavily inspired by Ted Young's [JitterTicket (Event Sourced version)](https://github.com/jitterted/jitterticket-event-sourced).

## Technical Stack

- **Java 26**
- **Spring Boot 4.x**
- **EclipseStore**: Used as the underlying persistence engine.
- **EclipseStore GigaMap**: Utilized for efficient indexing of events by Aggregate ID and Sequence Number.
- **AssertJ**: For fluent test assertions.
- **Maven**: Project management and build tool.

## Key Concepts

### Architecture
The project follows a **Ports and Adapters (Hexagonal Architecture)** pattern:
- **Domain**: Contains the `EventSourcedAggregate` base class and the `ShowBook` domain logic.
- **Application**: Defines the `EventStore` port and the `BaseEventStore` logic.
- **Adapter**: The `EclipseStoreEventStore` implements the port, mapping domain events to EclipseStore persistence.

### Events as Records + Sealed Interface

Events are modeled as immutable Java `record`s implementing a `sealed interface` (`ShowBookEvent`). This differs from
the abstract-class hierarchy used in the JitterTicket project that inspired this spike: instead of mutating an
`eventSequence` field on the event after the store assigns it, the store calls a `withSequence(...)` wither that returns
a new record. Benefits:

- No hand-rolled `equals`/`hashCode`/`toString`.
- Truly immutable events — no setters anywhere.
- Exhaustive pattern matching in `apply(...)` switches; the compiler flags missing cases when a new event type is added.

### Persistence Strategy
Unlike traditional relational databases or specialized event stores, this project uses EclipseStore to persist an object graph.
- **DataRoot**: The root object for EclipseStore, containing a `GigaMap` of events.
- **Indexing**: `GigaMap` provides bitmap indexes for:
    - `ShowBookId`: Efficiently retrieving all events for a specific aggregate.
    - `EventSequence`: Allowing for "catch-up" subscriptions by querying events after a specific checkpoint.

### Testability
Following the **Null Object Pattern** (as popularized by James Shore), the `EventStore` provides a `createNull()` factory method. This allows for fast, in-memory unit testing of application logic without requiring a real EclipseStore instance or file system access.

## Project Structure

```text
src/main/java/dev/nathanlively/event_sourced_eclipsestore/
├── adapter/out/store/       # EclipseStore implementation (Adapter)
├── application/             # EventStore interfaces and base logic (Port/App)
└── domain/                  # Core domain logic and Event Sourcing base classes
    └── showbook/            # The "ShowBook" sample domain
```

## Getting Started

### Prerequisites
- JDK 17 or higher
- Maven 3.8+

### Build and Run Tests
To compile the project and run all tests (including integration tests):

```bash
./mvnw clean test
```

## Reflection & Learnings
*Documentation of findings during the spike can be found in the `journal/` directory.*
