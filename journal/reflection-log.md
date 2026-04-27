## 2026-04-26 - Event Sourced EclipseStore

### Scope

- Files analyzed: 2
- Files modified: 2

### Refactorings Applied

- Reverted OpenRewrite's verbose exception assertions to specialized AssertJ methods (
  `assertThatIllegalArgumentException`, `assertThatIllegalStateException`).
- Added `.as()` descriptions to all assertions for better failure documentation.
- Ensured SCA (Setup, Command, Assert) structure with blank line separation in all test methods.
- Extracted magic strings to local variables for clarity.
- Used `ShowBookAssert` for domain-specific assertions (`hasId`).
- Fixed compilation errors in `CheckpointTest.java` introduced by OpenRewrite.

### Patterns Observed

- The codebase uses a "reconstitute" pattern for event-sourced aggregates, which is well-tested in `EventsProjectState`.
- `ShowBookFactory` and `ShowBookAssert` were already present but underutilized.

### Skill Improvement Notes

- OpenRewrite's `org.openrewrite.java.testing.assertj.Assertj` recipe can sometimes move away from specialized AssertJ
  methods (like `assertThatIllegalArgumentException`) towards a more verbose
  `assertThatThrownBy(...).isInstanceOf(...)`. This contradicts the "prefer specialized exception assertions" guideline
  in some project contexts. I should be careful when running it project-wide.
- The `refactor-tests` skill should explicitly mention checking for existing test factories/assertions before proposing
  new ones.

## 2026-04-26 - Event Sourced EclipseStore

### Scope

- Files analyzed: 1
- Files modified: 1 (`CheckpointTest.java`)

### Refactorings Applied

- **Add `.as()` Descriptions**: Applied to 11 assertions (count: 11)
- **Nested Grouping**: Organized tests into `Value`, `Creation`, `Validation`, `Comparison`, `Equality`, and
  `Observability` groups (count: 6)
- **Behavioral Naming**: Renamed all tests to follow the `<subject><verb><expectedOutcome>` pattern (count: 11)
- **Extract Magic Values**: Extracted raw integers to named local variables (count: 4)
- **SCA Structure**: Ensured clear separation between Arrange, Act, and Assert sections with blank lines (count: 11)

### Patterns Observed

- The `Checkpoint` value object has distinct responsibilities (validation, comparison, equality) that naturally
  clustered into `@Nested` groups.
- Using `.as()` descriptions significantly improves the "loudness" of failures, aligning with Paranoic Telemetry goals.

### Skill Improvement Notes

- The "Paranoic Telemetry" audit combined with the "Refactor Tests" skill creates a strong workflow: audit for *what* is
  missing, then refactor the result for *how* it's communicated.

## 2026-04-26 - event-sourced-eclipsestore

### Scope

- Files analyzed: 3
- Files modified: 3

### Refactorings Applied

- **A1/C (Split Integration Test)**: Decomposed a monolithic integration test into four focused tests within a `@Nested`
  class (`SurvivesStorageRestart`).
- **A2/B1 (withStore Helper)**: Introduced a try-with-resources wrapper for storage lifecycle management in integration
  tests.
- **A3 (Failure Factory)**: Added `storeThatThrows(RuntimeException)` in `EclipseStoreEventStoreTest` to consolidate
  repetitive error-path setup.
- **B2 (Custom Assertions)**: Extended `ShowBookAssert` with `hasName` and `isDeleted` to improve domain-centric
  readability in tests.

### Patterns Observed

- **Lifecycle Duplication**: Infrastructure-heavy tests (EclipseStore) tend to accumulate boilerplate for
  setup/teardown (storage restart). Wrappers like `withStore` are highly effective at restoring focus to the behavior
  being tested.
- **Assertion Consistency**: Domain objects with custom assertions should strive for completeness; partial assertions
  lead developers back to standard `assertThat(actual.field()).isEqualTo(expected)` which dilutes the "isolation" of the
  test.

### Skill Improvement Notes

- The "withStore" pattern for infrastructure-heavy tests is a recurring need in this project.
