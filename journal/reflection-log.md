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
