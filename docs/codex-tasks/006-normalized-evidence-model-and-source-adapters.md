# Task 006 — Normalized Evidence Model and Source Adapters

## Status

APPROVED FOR IMPLEMENTATION

## Objective

Build the normalized evidence layer that converts raw case-source files into a stable, auditable, member-linked, comparison-ready evidence model.

Task 006 must establish the data contracts and normalization behavior needed before implementing Flag 01, Flag 02, or Flag 03.

The normalized evidence layer must separate:

1. raw source parsing
2. authoritative household membership
3. source-to-member linkage
4. normalized income / benefit evidence
5. evidence applicability
6. evidence quality
7. provenance
8. comparison-ready evidence groups

Do not implement final flag decisions in this task.

## Confirmed data contracts

### Manifest population

`manifest.xlsx` defines the case population processed by the pipeline.

A case directory that is not listed in the manifest is not part of the evaluation population.

### Household membership

`household.json` is authoritative for household membership.

A CIN found only in `refids.json`, `income.json`, `rfi.json`, `ivs_*.json`, document JSON, or other sources must not be promoted to household membership unless it can be joined to a member present in `household.json`.

### Household activity

Observed household line field:

```text
$.household_members[].CLNT_LINE_NO
```

Confirmed interpretation:

- populated line number → active/current household member
- blank line number → no longer part of the current household

### REFIDS

`refids.json` may contain historical or otherwise associated CINs.

REFIDS is a mapping/reference source, not authoritative household membership.

### NQRF01

QA confirmed:

```text
NQRF01.LINE_NO
→ household.household_members[].CLNT_LINE_NO
→ household CIN
```

Preserve `CASE_SUFFIX` as source metadata, but it is not currently required for the member join.

For historical interpretation of RFI records, preserve `YR` and `QTR` for:

```text
NQRF01
NQRF02
NQRF03
```

The current 20-case tranche has little or no NQRF01 coverage, so synthetic tests must cover this linkage.

### NQRF02 / NQRF03 / NQRF11

Use direct CIN linkage when CIN is present.

Household membership must still be validated against `household.json`.

### IVS

The CIN encoded in the `ivs_*.json` filename identifies the member.

Payload REF_ID may be retained for cross-source correlation but is not required for member linkage.

### Income records

Observed identifiers include:

```text
EVAL_CIN
REF_ID
MATCH_REF_ID
RESULT_DETAIL_ID
```

Use direct `EVAL_CIN` linkage when present.

For records lacking EVAL_CIN, REF_ID / MATCH_REF_ID may be used only when they map uniquely through `refids.json`.

The resulting CIN must then be validated against authoritative `household.json`.

If the mapping is missing, non-unique, conflicting, or resolves only to a non-household CIN, do not silently assign the record.

### Document-derived JSON

The structural investigation found that the previously unclassified 38 JSON files include populated top-level:

```text
case_num
cin
f_docnumber
doc_type
```

Task 006 may classify these as document-derived evidence when their structure matches the confirmed shape.

Do not rely on filename exclusion alone.

## QA-derived evidence principles

The QA workbook establishes that evidence should not be evaluated solely by file presence, amount presence, source presence, raw document age, or SPOS amount.

Evidence must be assessed using:

```text
member attribution
income / benefit relationship
event dates
pay dates
pay periods
effective dates
review period
budget/reporting period
evidence completeness
status
source role
chronology
```

No universal source precedence is supported.

No universal 30-day production rule is supported.

## Candidate decision states

Preserve the ability for later rules to return:

```text
FLAG
NO_FLAG
REVIEW
INSUFFICIENT_EVIDENCE
NOT_APPLICABLE
```

Task 006 does not produce final flag decisions, but the normalized model must support these states later.

## Evidence applicability states

Implement a normalized applicability vocabulary.

At minimum support:

```text
CURRENT_ACTIONABLE
CURRENT_CORROBORATING
HISTORICAL_LEAD
STALE_FOR_REVIEW_PERIOD
FUTURE
ZERO_VALUE
INCOMPLETE
UNDATED
UNATTRIBUTABLE
CONFLICTING
SUPERSEDED
NOT_APPLICABLE
NEEDS_VERIFICATION
```

These states are evidence-level classifications, not Flag 01/02/03 outcomes.

Where available data is insufficient to determine an applicability state, use:

```text
NEEDS_VERIFICATION
```

Do not guess.

## Required normalized entities

Implement a Python normalized evidence model containing at least the following logical entities.

### EvaluationContext

Fields should include:

```text
case_id
manifest_status
evaluation_date
review_window_start
review_window_end
dataset_version
normalizer_version
```

Review-window values must be provided/configured.

Do not hard-code 30 or 90 days globally.

### HouseholdMember

Fields should include:

```text
member_id
household_line_no
household_status
active
source_member_identifier_hash
```

Do not expose raw CIN in public generated output.

Private source identifier mappings may remain under:

```text
local-data/generated/private/
```

### SourceMemberLink

Fields should include:

```text
source_system
source_record_id
member_id
linkage_method
linkage_status
linkage_confidence
source_identifier_type
```

Candidate linkage methods:

```text
DIRECT_CIN
HOUSEHOLD_LINE_NO
IVS_FILENAME_CIN
REFID_UNIQUE_MAP
DOCUMENT_TOP_LEVEL_CIN
UNRESOLVED
```

Candidate linkage statuses:

```text
LINKED
NON_HOUSEHOLD
AMBIGUOUS
UNRESOLVED
```

Do not use an LLM for linkage in Task 006.

### IncomeRelationship

Fields should include:

```text
income_relationship_id
member_id
income_category
income_type
employer_raw
employer_normalized
employer_match_status
employment_status
benefit_status
relationship_start_date
relationship_end_date
```

Do not implement sophisticated employer entity resolution yet.

For now provide a normalization scaffold and explicit match state.

### EvidenceItem

Fields should include:

```text
evidence_id
member_id
income_relationship_id
source_system
source_category
source_record_id
amount_raw
amount_normalized
frequency_raw
frequency_normalized
pay_date
pay_period_start
pay_period_end
receipt_date
effective_date
end_date
final_pay_date
source_date
refresh_date
document_date
rfi_year
rfi_quarter
employment_status
benefit_status
evidence_quality
evidence_completeness
applicability_state
member_linkage_method
member_linkage_status
provenance
```

### ApplicabilityAssessment

Fields should include:

```text
evidence_id
applicability_state
reason_codes
review_window_start
review_window_end
date_basis_used
manual_review_required
```

### ComparisonSet

Fields should include:

```text
comparison_set_id
member_id
income_category
income_type
normalized_employer
evidence_ids
comparison_status
```

Task 006 should group evidence only when the grouping is deterministic.

If employer identity is unresolved, keep records separate or mark the comparison set as review-required.

Do not create false certainty.

## Monetary normalization

Use Python `Decimal`.

Never use binary floating point for monetary values.

Preserve:

```text
raw amount
parsed Decimal amount
source frequency
normalized frequency
```

Do not calculate policy-specific monthly values unless an explicitly confirmed conversion rule exists.

If frequency normalization is useful, support canonical values such as:

```text
WEEKLY
BIWEEKLY
SEMIMONTHLY
MONTHLY
QUARTERLY
ANNUAL
UNKNOWN
```

Preserve raw source values.

## Date normalization

Use ISO 8601 normalized dates.

Preserve distinct concepts:

```text
pay_date
pay_period_start
pay_period_end
receipt_date
employment_start_date
employment_end_date
final_pay_date
benefit_effective_date
benefit_end_date
source_date
refresh_date
document_date
review_window_start
review_window_end
```

Do not collapse all date fields into a single generic date.

Do not use document date as a universal applicability date.

## RFI normalization

Implement source adapters for:

```text
NQRF01
NQRF02
NQRF03
NQRF11
```

### NQRF01

Member linkage:

```text
LINE_NO → household CLNT_LINE_NO
```

If no unique household line match exists:

```text
linkage_status = UNRESOLVED
```

Preserve `CASE_NO`, `CASE_SUFFIX`, `LINE_NO`, `YR`, and `QTR` in private provenance as appropriate.

Public normalized output must use surrogate identifiers.

### NQRF02 / NQRF03 / NQRF11

Use direct CIN linkage when present.

Preserve YR / QTR for NQRF02 and NQRF03.

Do not infer current applicability from quarter alone.

## Income adapter

Normalize each typed income record.

Prefer direct:

```text
EVAL_CIN
```

When absent, attempt unique REFID linkage through:

```text
REF_ID
MATCH_REF_ID
```

If both point to different members or mapping is non-unique:

```text
linkage_status = AMBIGUOUS
```

Do not choose arbitrarily.

Preserve `RESULT_DETAIL_ID` as source provenance but do not expose it publicly.

## IVS adapter

Use filename CIN linkage.

Normalize relevant evidence fields without assuming that every IVS record is current/actionable.

Preserve evidence dates, amount, frequency, source status, and provenance when available.

Historical / zero-value / undated records must remain distinguishable.

## Document adapter

For JSON files with confirmed document-derived structure, normalize:

```text
member_id
doc_type
amount
employer / benefit source
pay date
pay-period dates
document date
employment dates
benefit dates
evidence completeness
source provenance
```

Do not require every field.

Missing fields must be explicit.

Do not use raw OCR/document text in public outputs.

## Employer normalization scaffold

Implement deterministic basic normalization only.

Allowed transformations may include:

```text
trim leading/trailing whitespace
collapse repeated whitespace
uppercase/lowercase canonicalization
punctuation normalization
corporate suffix normalization
```

Preserve raw employer text.

Do not yet implement:

```text
curated aliases
DBA relationships
parent-company relationships
payroll processors
agency synonym mapping
fuzzy matching
LLM matching
```

Instead return one of:

```text
EXACT_MATCH
NORMALIZED_MATCH
POSSIBLE_MATCH_REVIEW
NO_MATCH
UNRESOLVED
```

Only assign `NORMALIZED_MATCH` when deterministic normalization produces an exact canonical match.

## Evidence quality and completeness

Implement explicit fields for:

```text
COMPLETE
PARTIAL
MISSING_REQUIRED_FIELDS
UNPARSEABLE
UNKNOWN
```

Evidence quality must not be inferred solely from source type.

An evidence item may be well-formed but historical, current but incomplete, current but unattributable, zero-value, conflicting, or undated.

Preserve these independently.

## Provenance

Every normalized field must be traceable to source.

At minimum preserve:

```text
source_file_category
source_file_surrogate_id
source_json_path
raw_value_present
transformation
normalizer_version
```

For private local outputs, raw values may be preserved where necessary.

For tracked/public outputs, do not expose case numbers, CINs, names, addresses, phone numbers, document IDs, reference IDs, or raw source text.

## Source adapters to implement

Create adapters/modules for at least:

```text
household
income
rfi
ivs
documents
refids
```

Keep adapters source-specific.

Do not place source parsing logic directly into comparison/rule code.

## Package structure

Extend `case-normalizer` cleanly.

A reasonable structure is:

```text
case-normalizer/src/snapper_normalizer/
├── models/
│   ├── evaluation_context.py
│   ├── household_member.py
│   ├── source_member_link.py
│   ├── income_relationship.py
│   ├── evidence_item.py
│   ├── applicability.py
│   └── comparison_set.py
├── adapters/
│   ├── household.py
│   ├── income.py
│   ├── rfi.py
│   ├── ivs.py
│   ├── documents.py
│   └── refids.py
├── normalization/
│   ├── money.py
│   ├── dates.py
│   ├── frequency.py
│   ├── employer.py
│   └── applicability.py
└── normalized_case.py
```

Codex may refine this structure if the existing package conventions favor a different organization.

Do not perform broad unrelated refactoring.

## Required CLI

Add a normalization command without removing the inventory command.

Example:

```bash
python -m snapper_normalizer normalize   --input ../local-data/s3-min   --manifest ../local-data/s3-min/manifest.xlsx   --output ../local-data/generated/normalized   --review-window-start 2026-05-21   --review-window-end 2026-06-20
```

If the implementation uses a different argument shape, document it.

The review window must be explicit input/configuration.

## Required local outputs

Write real normalized data only under:

```text
local-data/generated/normalized/
```

Suggested structure:

```text
local-data/generated/normalized/
├── cases/
│   └── <surrogate_case_id>/
│       ├── normalized_case.json
│       ├── member_links.json
│       ├── evidence_items.json
│       ├── applicability.json
│       └── comparison_sets.json
├── normalization_summary.json
├── linkage_summary.json
├── applicability_summary.json
└── normalization_errors.json
```

Do not commit these files.

## Required aggregate reporting

### Normalization summary

Report:

```text
manifest_cases_normalized
household_members_normalized
income_records_normalized
rfi_records_normalized
ivs_records_normalized
document_records_normalized
refid_records_read
normalization_errors
```

### Linkage summary

Report counts by:

```text
DIRECT_CIN
HOUSEHOLD_LINE_NO
IVS_FILENAME_CIN
REFID_UNIQUE_MAP
DOCUMENT_TOP_LEVEL_CIN
AMBIGUOUS
NON_HOUSEHOLD
UNRESOLVED
```

### Applicability summary

Report counts by:

```text
CURRENT_ACTIONABLE
CURRENT_CORROBORATING
HISTORICAL_LEAD
STALE_FOR_REVIEW_PERIOD
FUTURE
ZERO_VALUE
INCOMPLETE
UNDATED
UNATTRIBUTABLE
CONFLICTING
SUPERSEDED
NOT_APPLICABLE
NEEDS_VERIFICATION
```

Do not produce Flag 01/02/03 decisions.

## QA-model alignment

Use `docs/qa-evidence-model-proposal.md` as a candidate design reference.

Do not treat it as authoritative policy.

Where the QA-derived proposal conflicts with explicit confirmed data contracts, the confirmed data contract wins.

Where behavior remains ambiguous, preserve:

```text
NEEDS_VERIFICATION
```

or:

```text
UNRESOLVED
```

## Update decision log

Add a tracked decision entry to `docs/codex-decisions.md` recording:

### NQRF01 member linkage

QA confirmed:

```text
NQRF01.LINE_NO → household CLNT_LINE_NO → CIN
```

`CASE_SUFFIX` is preserved as source metadata and is not currently required for the member join.

For historical interpretation preserve `YR` and `QTR` for NQRF01, NQRF02, and NQRF03.

Current real-case coverage of NQRF01 is limited, so synthetic tests are required.

Do not include raw case/CIN examples in the decision log.

## Testing requirements

All committed tests must use synthetic fixtures only.

Add tests for at least:

### Household

- active member
- inactive member
- duplicate household line numbers
- missing line number
- household membership is authoritative

### Income

- direct EVAL_CIN linkage
- REFID fallback linkage
- ambiguous REFID linkage
- REFID resolving to non-household CIN
- missing identifier

### RFI

- NQRF01 line-number linkage
- NQRF01 missing household line
- NQRF01 duplicate household line ambiguity
- NQRF01 YR/QTR preservation
- NQRF02 direct CIN linkage
- NQRF03 direct CIN linkage
- NQRF11 direct CIN linkage
- RFI CIN not in household remains non-household

### IVS

- filename CIN linkage
- filename CIN not in household
- payload REF_ID preservation
- historical/zero-value record remains distinct

### Documents

- top-level CIN linkage
- missing CIN
- document member not in household
- missing dates
- incomplete evidence

### Money

- Decimal parsing
- blank amount
- invalid amount
- negative amount
- zero amount
- no floating-point conversion

### Dates

- valid ISO normalization
- blank date
- invalid date
- future date
- pay date distinct from document date

### Frequency

- known frequency
- unknown frequency
- raw value preserved

### Employer normalization

- case difference
- whitespace difference
- punctuation difference
- corporate suffix normalization
- deterministic normalized match
- no fuzzy/alias inference

### Applicability

- current actionable
- current corroborating
- historical lead
- stale
- future
- zero value
- incomplete
- undated
- unattributable
- needs verification

### Determinism / privacy

- source ordering does not change normalized output
- repeated run is deterministic
- raw identifiers absent from public reports
- no real-data fixtures are committed

## Do not implement in Task 006

Do not implement:

```text
FLAG_01
FLAG_02
FLAG_03
confusion matrix metrics
precision
recall
F1
accuracy
false-positive rate
final pipeline eligibility denominator
LLM extraction
LLM explanation
fuzzy employer matching
policy-specific monthly conversion unless confirmed
database storage
UI
```

Task 006 creates evidence suitable for those later tasks.

## Validation after implementation

Run:

```bash
cd case-normalizer
source .venv/bin/activate
pytest
```

Run the normalization CLI against the local manifest cases and QA 30-day review window.

Then run:

```bash
cd ../rules-service
./mvnw clean test
```

Also run the privacy audit.

Confirm:

- no private local-data files staged
- `rules-service` source unchanged
- ground truth unchanged
- Task 004 eligibility logic unchanged unless explicitly required for interface compatibility
- no flag outputs created
- no LLM calls made

## Required final report

Report only privacy-safe aggregate information.

### Changed files

List every tracked file created or modified.

### Tests

Report:

```text
Python passed/failed
Java passed/failed
Maven build result
privacy audit result
```

### Normalization

Report:

```text
manifest cases normalized
household members normalized
income records normalized
RFI records normalized by NQRF group
IVS records normalized
document records normalized
normalization errors
```

### Member linkage

Report counts by linkage method and status.

### Evidence applicability

Report counts by applicability state.

### NQRF01

Report:

- records observed
- records linked by household line
- unresolved
- ambiguous
- synthetic test coverage result

### Comparison sets

Report:

- comparison sets created
- unresolved employer groupings
- review-required groupings

### Unresolved assumptions

Explicitly identify:

- source fields whose meaning remains uncertain
- evidence applicability rules that are still provisional
- employer mappings that require curated aliases
- pipeline eligibility behavior not yet reproducible
- policy rules required before Flag implementation

Do not hide uncertainty.

## Definition of done

Task 006 is complete when:

- manifest cases can be normalized independently of flag logic
- authoritative household membership is enforced
- source records can be linked to household members deterministically where supported
- NQRF01 line-number linkage is implemented and tested
- YR/QTR are preserved for NQRF01/02/03
- monetary values use Decimal
- dates preserve distinct semantic roles
- evidence applicability is represented explicitly
- provenance is retained
- comparison-ready evidence can be generated without flag decisions
- ambiguous/unresolved evidence remains explicit
- all committed tests use synthetic data
- tests pass
- privacy audit passes
- no real data is committed
- rules-service remains unchanged
- ground truth remains unchanged
- no flag decision logic is introduced
- no commit is created

Wait for explicit review and approval before moving to Flag implementation.
