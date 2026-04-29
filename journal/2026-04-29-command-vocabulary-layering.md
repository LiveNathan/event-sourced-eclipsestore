# 2026-04-29 — Command Vocabulary Layering

## Context

As the ShowBook domain grows, every new field tends to come with a new command to update it. With an LLM extracting
facts from meeting transcripts and emitting fine-grained updates, the command list looks like it will grow roughly in
lockstep with the field list. The question: is "one command per field" a design smell, or the right shape? Should the
spike adopt some unifying abstraction (sealed `ShowBookCommand`, pattern-matched `decide`, annotation-driven generation,
an ES framework) before the surface gets bigger?

## Considered

1. **Sealed `ShowBookCommand` + single `handle(Command)` in the aggregate.** Mirrors the existing sealed event
   hierarchy. Compiler-exhaustive switch, mechanical JSON-schema generation for LLM tool-use, guard clauses centralized.
2. **Frameworks (Axon, jMolecules, Evento, Akka).** Full CQRS/ES stacks. Big commitment for a spike.
3. **Decider pattern (Chassaing).** Pure functional `(State, Command) -> List<Event>` shape, ~50 lines, no library.
4. **Annotation-processor / codegen to derive a command per field.** Rejected — re-encodes CRUD assumptions the
   aggregate exists to escape.
5. **Status quo: per-method command API on the aggregate.**

## Decision

Make no changes to the spike's domain. The "many commands" question turned out to be a layering question, not a
code-shape question.

The larger ShowBook project already has a director (`ShowBookCommandApplier`) sitting between the LLM and the domain,
with its own sealed `ShowBookCommand` carrying `Provenance` and `@JsonClassDescription` prose. That layer is the right
home for LLM-shaped ergonomics. The domain underneath should keep whatever method shape best expresses its invariants —
independent of how the LLM happens to phrase intentions today.

Three lists, three growth axes, intentionally decoupled:

- **LLM-facing `ShowBookCommand`** — grows with the LLM's expressive surface. Long and specific is a feature here; each
  entry is a tool the model chooses from.
- **Domain methods on `ShowBook`** — grow with the aggregate's consistency rules. Usually fewer, since multiple LLM
  commands often funnel through one domain method.
- **`ShowBookEvent`** — grows with recorded facts. Already sealed; switch-pattern `apply` already terse.

Unifying the LLM-facing and domain command vocabularies into a single sealed type would re-couple what the director was
built to separate.

## Where the real friction will appear

Not in the aggregate's method count. Two places to watch:

1. **Repeated `ifDeleted()` / guard boilerplate** in the larger `ShowBook` — addressable with a one-line
   `requireAlive()` helper, not architecture.
2. **The applier's switch + `FieldKeys` bookkeeping** — grows as (LLM commands × fields touched). If anything wants a
   declarative treatment later, it's that layer (e.g., commands self-reporting their field keys). Premature now; revisit
   after a few more commands land.

## Takeaway

The architecture already solves the problem the question implied. Resist the temptation to unify command vocabularies
just because they look similar — they grow for different reasons, on different schedules, driven by different consumers.
Keep the spike's domain simple; let the director absorb shape changes on the LLM side.
