# Spec Quality Checklist: Store it! — Storage Rack Organiser & Item Locator

**Purpose**: Validate specification completeness, clarity, consistency, and coverage before implementation.  
**Created**: 2025-02-10  
**Feature**: [spec.md](../spec.md)

**Note**: This checklist tests the quality of the requirements as written (unit tests for the spec), not implementation behaviour.

## Requirement Completeness

- [ ] CHK001 Are requirements defined for both photo sources (camera and gallery) for rack and item? [Completeness, Spec §FR-001, §FR-004]
- [ ] CHK002 Is the minimum and maximum number of mock/dummy records (1–5) explicitly reflected in requirements? [Completeness, Spec §FR-011]
- [ ] CHK003 Are requirements specified for how the rack list is ordered or displayed (e.g. by name, date)? [Gap]
- [ ] CHK004 Are “open, edit metadata, or remove a rack” behaviours fully specified, or is “within product rules” clarified? [Completeness, Spec §US1 Acceptance 4]
- [ ] CHK005 Are requirements defined for search result ordering or ranking? [Gap, Spec §FR-008]
- [ ] CHK006 Is the set of “other metadata” for rack and “other metadata/editable fields” for item enumerated or bounded in the spec? [Completeness, Spec §FR-001, §FR-004, §FR-007]

## Requirement Clarity

- [ ] CHK007 Is “photo mandatory vs optional” for rack and item explicitly decided and stated in requirements? [Clarity, Spec §Assumptions Photo requirements]
- [ ] CHK008 Is “tap region” or “shelf slot” definition (single tap vs area) specified so slot identity is unambiguous? [Clarity, Spec §FR-002, Key Entities Shelf slot]
- [ ] CHK009 Is “enough information to identify rack and slot” in search results defined (e.g. rack name + slot label)? [Clarity, Spec §FR-008]
- [ ] CHK010 Are “truncation or scrolling acceptable for display” and “without data loss” both specified for long names/descriptions/tags? [Clarity, Spec Edge Cases]
- [ ] CHK011 Is the persistence progression (none → local → remote) documented as a requirement or only as an assumption? [Clarity, Spec §FR-009, Assumptions]

## Requirement Consistency

- [ ] CHK012 Do functional requirements for “add item” (FR-004, FR-005) align with both user-story flows described in US2? [Consistency, Spec §US2, §FR-004, §FR-005]
- [ ] CHK013 Are edge-case behaviours (no racks, empty slot, no search results, delete rack/item) reflected in at least one FR or explicit edge-case statement? [Consistency, Spec Edge Cases]
- [ ] CHK014 Is “one placement per item” (no duplicate across slots) stated consistently with “multiple items per slot” (FR-003)? [Consistency, Spec §FR-003, Key Entities Item]

## Acceptance Criteria Quality

- [ ] CHK015 Are all success criteria (SC-001–SC-005) expressed in measurable terms (time, percentage)? [Measurability, Spec §Success Criteria]
- [ ] CHK016 Is “90% of users” in SC-004 defined (e.g. sample size or validation method) or left to validation plan? [Measurability, Spec §SC-004]
- [ ] CHK017 Can “register first rack in under two minutes” and “add item in under three minutes” be verified without implementation details? [Acceptance Criteria, Spec §SC-001, §SC-002]

## Scenario Coverage

- [ ] CHK018 Are primary flows (register rack, add item, locate via slot, locate via search) each covered by at least one FR and one acceptance scenario? [Coverage, Spec §US1–US3]
- [ ] CHK019 Are exception flows (no racks, no photo, search no results, delete last item/rack) addressed in requirements or edge cases? [Coverage, Spec Edge Cases]
- [ ] CHK020 Are requirements defined for the “reverse” add-item flow (rack/slot first, then item) so it is testable? [Coverage, Spec §FR-005, §US2]

## Edge Case Coverage

- [ ] CHK021 Is empty-state behaviour (no racks yet, empty slot) explicitly required (e.g. “MUST guide” or “MUST allow add”)? [Edge Case, Spec Edge Cases]
- [ ] CHK022 Is behaviour when photo capture or gallery pick is cancelled or fails specified? [Gap]
- [ ] CHK023 Are requirements for “remove last item from slot” and “delete rack” (confirmation, cascade) defined? [Edge Case, Spec Edge Cases]

## Non-Functional Requirements

- [ ] CHK024 Are performance or responsiveness expectations beyond success criteria (SC-001–SC-003) specified? [Gap]
- [ ] CHK025 Are accessibility requirements (e.g. screen reader, touch targets) documented or explicitly out of scope? [Gap]
- [ ] CHK026 Is the intended use of mock data (debug only, excluded from production) clearly stated? [Clarity, Spec §FR-011, Clarifications]

## Dependencies & Assumptions

- [ ] CHK027 Are assumptions (persistence strategy, no login, owner as free text, slot definition) traceable to requirements that depend on them? [Assumption, Spec §Assumptions]
- [ ] CHK028 Is the assumption “photo optional for MVP” or “photo mandatory” resolved and reflected in FRs? [Assumption, Spec §Assumptions Photo requirements]

## Ambiguities & Conflicts

- [ ] CHK029 Is “within product rules” for opening/editing/removing a rack replaced with concrete rules or marked for later definition? [Ambiguity, Spec §US1 Acceptance 4]
- [ ] CHK030 Are “coordinates or a logical position” for shelf slot defined so implementation can choose one approach consistently? [Ambiguity, Spec Key Entities Shelf slot]

## Notes

- Check items off as completed: `[x]`
- Use [Gap] items to propose spec additions; use [Ambiguity]/[Clarity] to tighten wording.
- Items are numbered sequentially for traceability.
