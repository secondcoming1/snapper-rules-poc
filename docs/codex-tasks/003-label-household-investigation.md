# Task 003 — Investigate Ground-Truth CIN / Household Mismatches

## Status

APPROVED FOR INVESTIGATION

## Objective

Determine why 14 of the 64 labeled CINs in the candidate ground-truth
dataset are not currently matched to CINs discovered from `household.json`.

This is an investigation task only.

Do not:

- change flag logic
- normalize income or benefit evidence
- use an LLM
- modify `rules-service`
- modify candidate ground-truth labels
- commit implementation changes

## Current finding

Milestone 1 reported:

- 64 distinct labeled CINs
- 50 labeled CINs found in household data
- 14 labeled CINs not found using the current household CIN extraction
- 20 labeled cases
- 22 tranche cases
- 0 ground-truth cases absent from the tranche
- 64 complete three-flag label sets
- 0 incomplete three-flag label sets

The current household CIN extraction recognizes:

- `cin`
- `*_cin`
- `cins`
- `unique_cins`

This extraction logic may be incomplete.

## Investigation requirements

For each of the 14 unmatched labeled CINs, investigate privately whether
the CIN appears anywhere within the corresponding case directory.

Search all JSON structures recursively, not only `household.json`.

Do not print or report:

- CIN values
- case numbers
- names
- addresses
- phone numbers
- document IDs
- reference IDs
- raw JSON values
- raw document text

Use surrogate identifiers only in reports.

For each unmatched labeled CIN, assign exactly one investigation classification:

- `FOUND_IN_HOUSEHOLD_UNSUPPORTED_PATH`
- `FOUND_IN_OTHER_STRUCTURED_SOURCE`
- `FOUND_ONLY_IN_UNCLASSIFIED_JSON`
- `FORMAT_NORMALIZATION_DIFFERENCE`
- `NOT_FOUND_IN_CASE_DATA`
- `AMBIGUOUS`
- `INVESTIGATION_ERROR`

Do not automatically change the production extractor based on findings.

## CIN comparison

For investigation purposes, compare CINs using both:

1. exact source value
2. a diagnostic normalized form

The diagnostic normalization may:

- trim leading whitespace
- trim trailing whitespace
- normalize alphabetic characters to uppercase

Do not:

- remove arbitrary characters
- strip internal punctuation
- remove leading zeros
- add padding
- truncate identifiers
- perform fuzzy matching
- transform identifiers in ways that could create false matches

Report whether diagnostic normalization changes any match result.

## Household structure investigation

Inspect `household.json` structurally for all 22 cases.

Identify privacy-safe structural paths containing fields whose key names
suggest household-member or CIN identity.

Examples of relevant key-name patterns may include:

- `cin`
- `member`
- `client`
- `participant`
- `person`
- `individual`
- `household`

Do not assume a field is a CIN solely from its key name.

Report only:

- structural path
- value type
- number of cases containing the path
- number of occurrences
- whether the path appears to contain candidate identifier-shaped values

Do not report actual values.

Determine whether the current CIN extractor misses legitimate household
schema paths.

## Cross-source investigation

For each of the 14 unmatched labeled CINs, determine privately whether it
occurs anywhere in the corresponding case package.

Check the following source categories independently:

- `household.json`
- `income.json`
- `budget.json`
- `rfi.json`
- `refids.json`
- `ivs_*.json`
- `UNCLASSIFIED_JSON`

Presence in another source is not proof that the person is a household
member.

Do not alter household membership based solely on cross-source presence.

For the privacy-safe report, include only:

- surrogate case ID
- surrogate member ID
- source category
- presence: true/false
- exact-match result
- diagnostic-normalized-match result

Do not include source paths that themselves reveal private identifiers.

## Source-category interpretation

Use the existing Milestone 1 file classifications:

- `HOUSEHOLD`
- `INCOME`
- `BUDGET`
- `RFI`
- `REFIDS`
- `IVS`
- `UNCLASSIFIED_JSON`
- `NON_JSON`

Do not infer that `UNCLASSIFIED_JSON` files are parsed document output
unless structure establishes that in a later approved task.

For this investigation, they remain `UNCLASSIFIED_JSON`.

## Required outputs

Write all real-data-derived investigation output under:

```text
local-data/generated/investigation/
```

Create:

```text
cin_mismatch_summary.json
household_identity_paths.json
cross_source_presence_summary.json
```

These outputs must remain ignored by Git.

### `cin_mismatch_summary.json`

Include aggregate counts for:

- total unmatched labeled CINs investigated
- `FOUND_IN_HOUSEHOLD_UNSUPPORTED_PATH`
- `FOUND_IN_OTHER_STRUCTURED_SOURCE`
- `FOUND_ONLY_IN_UNCLASSIFIED_JSON`
- `FORMAT_NORMALIZATION_DIFFERENCE`
- `NOT_FOUND_IN_CASE_DATA`
- `AMBIGUOUS`
- `INVESTIGATION_ERROR`

It may also contain privacy-safe per-item entries using only:

- surrogate case ID
- surrogate member ID
- investigation classification
- exact-match status
- diagnostic-normalization-match status
- safe diagnostic code

Do not include raw identifiers.

### `household_identity_paths.json`

Include privacy-safe structural information for `household.json`:

- candidate structural path
- JSON value type
- case prevalence
- occurrence count
- whether the key name suggests CIN/member/client identity
- whether values at the path appear identifier-shaped
- recommendation status:
  - `CURRENTLY_SUPPORTED`
  - `CANDIDATE_FOR_EXTRACTOR`
  - `NOT_IDENTIFIER`
  - `AMBIGUOUS`

Do not include actual values.

### `cross_source_presence_summary.json`

Include aggregate source-presence counts for the 14 unmatched CINs.

For example:

- found in HOUSEHOLD
- found in INCOME
- found in BUDGET
- found in RFI
- found in REFIDS
- found in IVS
- found in UNCLASSIFIED_JSON
- not found anywhere

If privacy-safe per-item entries are included, use surrogate IDs only.

## Implementation boundaries

This task is primarily investigative.

Do not modify the Milestone 1 extractor unless explicitly approved after
the findings are reviewed.

You may add temporary investigation code only if needed, but:

- keep it isolated from production extraction behavior
- do not alter existing output contracts
- do not change tests merely to accommodate findings
- do not commit anything
- prefer a small diagnostic script or investigation module if code is necessary

If temporary code is created, list every changed file in the final report.

## Validation requirements

Before reporting results:

1. Confirm all 14 unmatched labeled CINs were investigated.
2. Confirm no raw case number appears in public/generated reports.
3. Confirm no raw CIN appears in public/generated reports.
4. Confirm no names, addresses, phone numbers, document IDs, or reference IDs appear.
5. Confirm `rules-service` has not changed.
6. Confirm candidate ground-truth data has not changed.
7. Confirm no flag calculation was performed.
8. Confirm no LLM or Bedrock call was made.
9. Confirm investigation outputs are under `local-data/`.
10. Confirm nothing was committed.

## Expected final report

Report only privacy-safe findings:

1. Number of the 14 CINs found in `household.json` through unsupported paths.
2. Number found in other known structured sources but not `household.json`.
3. Number found only in `UNCLASSIFIED_JSON`.
4. Number resolved by whitespace/case normalization.
5. Number not found anywhere in the corresponding case package.
6. Number remaining ambiguous.
7. Number of investigation errors, if any.
8. Household identity paths discovered and their prevalence.
9. Whether the current household CIN extractor is incomplete.
10. Exact proposed extractor changes, if any.
11. Whether Milestone 1 should be rerun after an approved extractor change.
12. Changed files, if temporary investigation code was created.
13. Confirmation that no commit was created.

Do not display raw case numbers, CINs, or source values.

Wait for review and explicit approval before implementing any extractor changes.
