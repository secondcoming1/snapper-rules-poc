# Task 004 — Evaluation Population and Pipeline-Validation Alignment

## Status

APPROVED FOR IMPLEMENTATION

## Objective

Align the evaluation population to the actual population processed by the production pipeline.

The implementation must distinguish:

1. physical storage population
2. manifest population
3. household population
4. active household population
5. pipeline-processing population
6. validation population

Only the final validation population may contribute to model-performance metrics.

A CIN that is skipped by the production pipeline must not enter the validation denominator, even when it has an explicit FALSE/FALSE/FALSE ground-truth outcome.

Do not:

- implement Flag 01, Flag 02, or Flag 03 logic
- normalize income or benefit evidence beyond what is required to determine pipeline eligibility
- use an LLM
- call Bedrock
- modify `rules-service`
- alter candidate ground-truth labels
- add a database
- add Docker or containers
- commit anything

## Confirmed data contract

### 1. Manifest controls case population

The production pipeline only runs cases listed in the manifest.

Therefore:

- physical presence of a case directory under `local-data/s3-min` does not make it part of the evaluation tranche
- a case listed in the manifest is part of the intended pipeline population
- a stored case directory not listed in the manifest must be classified as non-manifest and excluded from validation
- a manifest-listed case missing from storage must be reported as a data-quality problem

Current confirmed manifest structure:

```text
Filename: manifest.xlsx
Location: local-data/s3-min/manifest.xlsx
Worksheet: Sheet1
Case identifier column: case_no
```

The implementation must discover and validate this structure at runtime rather than hard-code the current row count.

The current supplied manifest contains 20 unique cases.

Two additional case directories currently present in storage belong to a separate McKinsey test set and are not part of this manifest population.

Do not hard-code those case identifiers. Determine case eligibility from the manifest.

### 2. `household.json` is authoritative for household membership

`household.json` is the authoritative source for determining whether a CIN belongs to the household represented by the case.

A CIN found only in another source must not be promoted to household membership.

This includes CINs found in:

- `refids.json`
- `income.json`
- `budget.json`
- `rfi.json`
- `ivs_*.json`
- `UNCLASSIFIED_JSON`

`refids.json` may contain historical or otherwise associated CINs.

Presence in REFIDS does not establish current household membership.

### 3. Household active status

The observed household member line-number path is:

```text
$.household_members[].CLNT_LINE_NO
```

Confirmed interpretation:

- populated line number → active/current household member
- blank line number → no longer part of the current household

The implementation must continue to detect the line-number field structurally rather than relying solely on the current spelling, provided detection is unambiguous.

If the relevant line-number field is missing or ambiguous, classify the member as:

```text
UNKNOWN_HOUSEHOLD_STATUS
```

Do not silently assume active.

### 4. Household membership does not imply pipeline eligibility

A CIN may be present in `household.json`, active, and still skipped by the pipeline.

Examples include:

- active CIN with zero income
- active CIN with no relevant SPOS / IVS / RFI / document data
- rejected case/member status
- closed case/member status

Therefore:

```text
ACTIVE_HOUSEHOLD_MEMBER != PIPELINE_ELIGIBLE
```

Pipeline eligibility must be represented separately from household membership.

### 5. Active CINs with zero income

Confirmed pipeline behavior:

Active CINs with no income are skipped by the pipeline.

They may:

- appear in `household.json`
- be active household members
- have explicit FALSE/FALSE/FALSE ground-truth rows

They must still be excluded from the validation denominator if the production pipeline would skip them.

Classify as:

```text
EXCLUDED_ACTIVE_NO_INCOME
```

Do not count these rows as true negatives.

### 6. Active CINs with no relevant source data

QA confirmed that some active CINs have no corresponding usable data in:

- SPOS
- IVS
- RFI
- Documents

When such CINs are skipped by the production pipeline, classify them as:

```text
EXCLUDED_ACTIVE_NO_RELEVANT_SOURCE_DATA
```

Do not count their ground-truth rows in TP / FP / TN / FN metrics.

### 7. Rejected and closed statuses

QA confirmed that some historical/non-current CINs are:

```text
RJ = Rejected
CL = Closed
```

in WMS and SPOS Production.

These CINs are not part of pipeline validation.

Normalize supported status values to at least:

```text
ACTIVE
REJECTED
CLOSED
UNKNOWN
```

Pipeline exclusion classifications:

```text
EXCLUDED_REJECTED_STATUS
EXCLUDED_CLOSED_STATUS
```

If status cannot be established or sources conflict, do not silently choose ACTIVE.

Use:

```text
EXCLUDED_UNKNOWN_CASE_STATUS
```

or an equivalent explicit uncertainty classification.

### 8. REFIDS-only labeled CINs

A labeled CIN that is absent from `household.json` but present in `refids.json` must be treated as a non-household CIN.

Classify as:

```text
EXCLUDED_NON_HOUSEHOLD_CIN
```

REFIDS presence may be retained as privacy-safe diagnostic metadata.

It must not make the CIN validation eligible.

### 9. Ground truth does not determine pipeline eligibility

Ground-truth presence alone does not make a CIN eligible for pipeline validation.

The ground-truth dataset may contain records for:

- zero-income CINs
- historical CINs
- rejected CINs
- closed CINs
- non-household CINs
- CINs skipped by the pipeline
- CINs included for QA or reference purposes

Ground-truth FALSE does not mean:

```text
pipeline evaluated CIN and predicted no flag
```

Those are different concepts.

A skipped CIN with FALSE/FALSE/FALSE labels must not be automatically counted as three true negatives.

## Population model

### Physical storage population

All case directories physically present under:

```text
local-data/s3-min/
```

Classification:

```text
STORED_CASE
```

### Manifest population

Cases listed in `manifest.xlsx`.

Classifications:

```text
MANIFEST_CASE_PRESENT
MANIFEST_CASE_MISSING
NON_MANIFEST_CASE_PRESENT
```

### Household population

CINs present in authoritative `household.json`.

Classifications:

```text
HOUSEHOLD_MEMBER
NON_HOUSEHOLD_CIN
```

### Household activity population

Household CINs classified as:

```text
ACTIVE_HOUSEHOLD_MEMBER
INACTIVE_HOUSEHOLD_MEMBER
UNKNOWN_HOUSEHOLD_STATUS
```

### Pipeline-processing population

CINs that the production pipeline would actually process.

Classifications:

```text
PIPELINE_ELIGIBLE
EXCLUDED_NON_MANIFEST_CASE
EXCLUDED_NON_HOUSEHOLD_CIN
EXCLUDED_INACTIVE_HOUSEHOLD_MEMBER
EXCLUDED_ACTIVE_NO_INCOME
EXCLUDED_ACTIVE_NO_RELEVANT_SOURCE_DATA
EXCLUDED_REJECTED_STATUS
EXCLUDED_CLOSED_STATUS
EXCLUDED_UNKNOWN_CASE_STATUS
EXCLUDED_UNKNOWN_HOUSEHOLD_STATUS
EXCLUDED_PIPELINE_ELIGIBILITY_UNDETERMINED
EXCLUDED_OTHER
```

### Validation population

CIN/flag records that are pipeline eligible and have valid ground truth for the flag being evaluated.

A validation-eligible CIN must not be included merely because it is in the manifest, active, present in ground truth, or labeled FALSE.

The denominator must represent actual production pipeline execution scope.

## Pipeline-validation eligibility rule

A CIN is eligible for pipeline validation only when all required conditions are true:

1. case is listed in the manifest
2. case package exists in storage
3. CIN exists in authoritative `household.json`
4. CIN is an active household member
5. case/member status is eligible for pipeline execution
6. CIN has relevant income/source data required for pipeline execution
7. production pipeline would actually process that CIN
8. valid ground truth exists for the evaluation target

Do not collapse these conditions into a single implicit Boolean.

Preserve the reason for every inclusion or exclusion.

## Pipeline source-data eligibility

The implementation must determine and report source-data presence relevant to pipeline execution.

At minimum, preserve privacy-safe indicators for:

```text
income_present
SPOS_data_present
IVS_data_present
RFI_data_present
document_data_present
relevant_source_data_present
```

Do not assume every source is required.

Do not invent pipeline business rules that have not been confirmed.

The immediate confirmed rule is:

```text
active CIN with zero income → pipeline skipped
```

Where the existing case package does not contain enough information to determine production pipeline eligibility, classify as:

```text
EXCLUDED_PIPELINE_ELIGIBILITY_UNDETERMINED
```

and report the reason.

Do not guess.

## Income eligibility

For Task 004, determine only enough income presence/absence to reproduce the pipeline population.

Do not yet perform:

- employer matching
- income amount reconciliation
- frequency normalization
- monthly conversion
- evidence precedence
- discrepancy classification
- Flag 01 calculation
- Flag 02 calculation
- Flag 03 calculation

The output should identify whether an active CIN has income relevant to pipeline execution, not whether that income is correct.

## Case-status handling

Where case/member status is available, preserve:

```text
raw status source
normalized status
status source system
```

Supported normalized statuses must include:

```text
ACTIVE
REJECTED
CLOSED
UNKNOWN
```

If WMS and SPOS statuses disagree:

- preserve both
- classify normalized status as UNKNOWN unless an approved precedence rule exists
- exclude from validation until resolved
- report a privacy-safe conflict reason

Do not invent status precedence.

## Required implementation changes

Update `case-normalizer` so it can:

1. parse and validate the manifest
2. determine manifest case membership
3. classify stored cases as `MANIFEST_CASE_PRESENT` or `NON_MANIFEST_CASE_PRESENT`
4. identify `MANIFEST_CASE_MISSING`
5. identify authoritative household CINs from `household.json`
6. classify household member activity
7. determine privacy-safe source-data presence for each CIN
8. determine whether an active CIN has zero income
9. determine relevant-source-data availability where the available input permits it
10. preserve case/member status where available
11. classify pipeline eligibility
12. validate candidate labels against pipeline eligibility
13. classify REFIDS-only labels as `EXCLUDED_NON_HOUSEHOLD_CIN`
14. classify zero-income active CINs as `EXCLUDED_ACTIVE_NO_INCOME`
15. classify no-source active CINs as `EXCLUDED_ACTIVE_NO_RELEVANT_SOURCE_DATA`
16. classify rejected CINs as `EXCLUDED_REJECTED_STATUS`
17. classify closed CINs as `EXCLUDED_CLOSED_STATUS`
18. preserve unknown/undetermined eligibility explicitly
19. calculate the validation-eligible population
20. calculate eligible ground-truth row counts by flag and TRUE/FALSE value

Do not calculate model predictions.

## Required outputs

Write all real-data-derived outputs under:

```text
local-data/generated/inventory/
```

At minimum create or update:

```text
tranche_summary.json
manifest_summary.json
household_membership_summary.json
pipeline_population_summary.json
ground_truth_summary.json
label_join_summary.json
evaluation_population_summary.json
validation_errors.json
```

All output must remain ignored by Git.

## Required report contents

### `manifest_summary.json`

Include aggregate counts for manifest rows, unique manifest cases, duplicates, blanks/malformed IDs, present/missing manifest cases, and non-manifest stored directories.

### `household_membership_summary.json`

Include aggregate counts for total household CIN records, active/inactive/unknown members, active manifest members, non-manifest household members, detected line-number path, and line-number types.

### `pipeline_population_summary.json`

Include aggregate counts for:

```text
active_manifest_household_members
pipeline_eligible_cins
excluded_cins_total
EXCLUDED_NON_MANIFEST_CASE
EXCLUDED_NON_HOUSEHOLD_CIN
EXCLUDED_INACTIVE_HOUSEHOLD_MEMBER
EXCLUDED_ACTIVE_NO_INCOME
EXCLUDED_ACTIVE_NO_RELEVANT_SOURCE_DATA
EXCLUDED_REJECTED_STATUS
EXCLUDED_CLOSED_STATUS
EXCLUDED_UNKNOWN_CASE_STATUS
EXCLUDED_UNKNOWN_HOUSEHOLD_STATUS
EXCLUDED_PIPELINE_ELIGIBILITY_UNDETERMINED
EXCLUDED_OTHER
```

Also report privacy-safe source-presence aggregates.

### `label_join_summary.json`

Include counts for labeled CINs by household status and pipeline eligibility, complete/incomplete three-flag sets, and exclusions by reason.

### `evaluation_population_summary.json`

This report defines the future metric denominator.

Include:

```text
manifest_cases
manifest_cases_present
manifest_cases_with_at_least_one_pipeline_eligible_member
active_manifest_household_members
pipeline_eligible_members
pipeline_excluded_members
pipeline_eligible_members_with_complete_ground_truth
pipeline_eligible_members_without_complete_ground_truth
eligible_flag_rows
excluded_flag_rows
eligible_FLAG_01_TRUE
eligible_FLAG_01_FALSE
eligible_FLAG_02_TRUE
eligible_FLAG_02_FALSE
eligible_FLAG_03_TRUE
eligible_FLAG_03_FALSE
```

Do not compute TP, FP, TN, FN, precision, recall, F1, accuracy, or false-positive rate in this task.

## Evaluation denominator rule

The evaluation denominator must be calculated from pipeline-eligible records only.

Never derive the denominator directly from:

```text
all stored cases
all manifest cases
all household members
all active household members
all rows in ground truth
all FALSE ground-truth rows
```

Instead:

```text
production pipeline population
+
valid ground truth
=
validation population
```

An excluded CIN must never create an implicit TRUE or FALSE prediction.

## Required audit behavior

For every excluded CIN, preserve a privacy-safe audit record containing only:

```text
surrogate_case_id
surrogate_member_id
manifest_status
household_membership_status
household_activity_status
normalized_case_status
source_presence_summary
pipeline_eligibility_classification
exclusion_reason_code
ground_truth_presence
```

Do not include raw case numbers, CINs, names, addresses, phone numbers, document IDs, reference IDs, source values, or document text.

Any private mappings must remain under:

```text
local-data/generated/private/
```

## Known QA-adjudicated categories

### Active but zero income

These are intentionally skipped by the production pipeline.

Classification:

```text
EXCLUDED_ACTIVE_NO_INCOME
```

Ground-truth FALSE rows for these members do not enter the denominator.

### Active but no usable source data

Some active CINs have no corresponding data in SPOS, IVS, RFI, or Documents.

When the pipeline skips them, classify as:

```text
EXCLUDED_ACTIVE_NO_RELEVANT_SOURCE_DATA
```

### Rejected

QA has confirmed examples with `RJ` status in WMS and SPOS Production.

Classification:

```text
EXCLUDED_REJECTED_STATUS
```

### Closed

QA has confirmed examples with `CL` status in WMS and SPOS Production.

Classification:

```text
EXCLUDED_CLOSED_STATUS
```

### REFIDS-only

CINs found only in REFIDS are historical/reference associations, not household membership.

Classification:

```text
EXCLUDED_NON_HOUSEHOLD_CIN
```

## Do not hard-code QA findings to individual CINs

Do not create logic like:

```text
if CIN == X:
    exclude
```

QA findings must inform general classification rules.

The implementation must derive eligibility from available source attributes.

If current case-package data does not contain enough information to reproduce an adjudicated status, report:

```text
EXCLUDED_PIPELINE_ELIGIBILITY_UNDETERMINED
```

and identify the missing data dependency.

Do not use ground-truth outcomes to reverse-engineer eligibility.

## Ground-truth file handling

Do not alter ground-truth labels.

The current ground-truth dataset may be replaced locally by a newer consolidated version.

The CLI must accept the label file path as input.

Do not hard-code a particular ground-truth filename.

Example:

```bash
python -m snapper_normalizer inventory \
  --input ../local-data/s3-min \
  --manifest ../local-data/s3-min/manifest.xlsx \
  --labels ../local-data/ground-truth/<current-ground-truth-file>.csv \
  --output ../local-data/generated/inventory
```

## Privacy requirements

Do not print or report raw case numbers, CINs, names, addresses, phone numbers, document IDs, source reference IDs, source JSON values, raw document text, or raw manifest case identifiers.

Use deterministic surrogate identifiers where per-case/member diagnostics are necessary.

Use SHA-256 or the existing approved surrogate-ID mechanism.

Do not use Python's built-in `hash()` for persistent identifiers.

## Testing requirements

All committed tests must use synthetic fixtures only.

Add or update synthetic tests for:

### Manifest behavior

- manifest-listed case present
- manifest-listed case missing
- non-manifest stored case
- duplicate manifest entry
- blank manifest entry
- malformed manifest entry

### Household membership

- household CIN is recognized
- REFIDS-only CIN remains non-household
- active household member
- inactive household member
- missing line number
- ambiguous line-number field
- unknown household status

### Pipeline eligibility

- active household CIN with income → pipeline eligible
- active household CIN with zero income → excluded
- active household CIN with no relevant source data → excluded
- inactive CIN → excluded
- rejected status → excluded
- closed status → excluded
- unknown/conflicting status → excluded
- REFIDS-only CIN → excluded
- non-manifest case → excluded
- unknown pipeline eligibility → excluded and reported

### Ground truth

- eligible CIN with complete ground truth → validation eligible
- eligible CIN without complete ground truth → not validation eligible
- excluded CIN with FALSE/FALSE/FALSE labels → remains excluded
- excluded CIN must never become a true negative merely because ground truth is FALSE
- non-household labeled CIN remains excluded
- zero-income labeled CIN remains excluded

### Determinism and privacy

- repeated run produces deterministic classifications
- denominator is deterministic
- source-record ordering does not change eligibility
- public reports contain no exact raw identifiers
- no real fixtures appear in committed tests

## Backward compatibility

Preserve Milestone 1 inventory behavior where it remains valid.

Do not:

- remove prior reports without replacement
- break existing synthetic tests unnecessarily
- modify Java rule-service endpoints
- introduce rules-engine dependencies into Python
- introduce prediction logic
- introduce LLM dependencies
- introduce database dependencies

Update README documentation to explain the distinction between storage, manifest, household, active household, pipeline, and validation populations.

## Validation after implementation

After changes:

1. Run Python tests.
2. Run the inventory CLI against the current local dataset and current ground-truth file.
3. Run `./mvnw clean test` in `rules-service`.
4. Run the privacy audit.
5. Confirm no private files are staged.
6. Confirm `rules-service` source is unchanged.
7. Confirm ground truth is unchanged.

## Required final report

Report only privacy-safe aggregate results.

Include:

### Changed files

List every tracked file created or modified.

### Tests

Report Python test results, Java test results, Maven build result, and privacy audit result.

### Manifest population

Report manifest rows, unique manifest cases, present/missing manifest cases, and non-manifest stored cases.

### Household population

Report total household members, active members, inactive members, unknown-status members, and active members in manifest cases.

### Pipeline population

Report counts for all pipeline eligibility and exclusion classifications.

### Validation population

Report pipeline-eligible members with complete ground truth, pipeline-eligible members missing ground truth, eligible flag rows, excluded flag rows, and eligible TRUE/FALSE counts by flag.

### Unresolved assumptions

Explicitly state:

- any pipeline eligibility rule that cannot be derived from the supplied case package
- any status field whose semantics are not established
- any source-data presence definition that remains provisional
- any QA-adjudicated exclusions that cannot yet be reproduced deterministically from supplied inputs

Do not hide discrepancies.

## Definition of done

Task 004 is complete when:

- manifest case scope is authoritative
- non-manifest stored cases are excluded
- household membership comes only from `household.json`
- active/inactive household status is represented separately
- pipeline eligibility is represented separately from household activity
- zero-income CINs skipped by the pipeline are excluded
- no-source CINs skipped by the pipeline are excluded
- rejected/closed CINs are excluded when status can be established
- REFIDS-only CINs are excluded as non-household
- skipped CINs do not enter the validation denominator
- FALSE ground truth does not automatically create a true negative
- validation denominator reflects actual pipeline execution scope
- all exclusions have explicit reason codes
- all tests pass
- privacy audit passes
- no `rules-service` source changes occur
- ground truth remains unchanged
- no commit is created

Wait for explicit review and approval before committing.
