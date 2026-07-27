# Task 004 — Evaluation Population and Manifest Alignment

## Status

APPROVED FOR IMPLEMENTATION

## Objective

Update Milestone 1 so that evaluation eligibility is based on the actual
pipeline population rather than every case directory physically present
under local-data/s3-min.

The implementation must use:

- manifest-listed cases as the authoritative case population
- household.json as the authoritative source for household membership
- household membership status to distinguish active from inactive/former members
- refids.json only as reference/historical linkage, never as proof of household membership

Do not implement flag logic.
Do not normalize income or benefits yet.
Do not use an LLM.
Do not modify rules-service.
Do not alter ground-truth labels.

## Confirmed data contract

### Case population

The pipeline runs only cases listed in the manifest.

Therefore:

- a directory physically present under local-data/s3-min is not automatically part of the evaluation tranche
- manifest-listed cases are eligible for pipeline evaluation
- non-manifest case folders must be inventoried but excluded from evaluation metrics
- cases listed in the manifest but absent from storage must be reported as data-quality errors

Two previously observed case directories are McKinsey test cases that were
accidentally left in the folder and are not part of the intended evaluation tranche.

Do not hard-code those case IDs. Determine eligibility from the manifest.

### Household membership

household.json is authoritative for household membership.

A CIN appearing in:

- refids.json
- income.json
- budget.json
- RFI
- IVS
- other source files

must not be promoted to household membership unless that CIN is also represented
in household.json according to the household membership rules.

refids.json may contain historical or other associated CINs.

### Active versus inactive household member

The household API response contains a line-number field.

Confirmed interpretation:

- populated line number → current/active household member
- blank line number → CIN is no longer part of the current household

The implementation must discover the actual field name from household.json rather
than hard-code an assumed spelling unless the existing source schema already
defines it.

If a household member is present but membership status cannot be determined,
classify as:

UNKNOWN_HOUSEHOLD_STATUS

Do not silently assume active.

## Evaluation eligibility

A label is eligible for model evaluation only when all of the following are true:

1. the case is listed in the manifest
2. the corresponding case directory exists
3. the CIN exists in household.json
4. the household member is ACTIVE
5. the candidate ground-truth label set is valid

Use the following membership classifications:

- ACTIVE_HOUSEHOLD_MEMBER
- INACTIVE_HOUSEHOLD_MEMBER
- UNKNOWN_HOUSEHOLD_STATUS
- NON_HOUSEHOLD_CIN
- LABEL_CIN_NOT_PRESENT_IN_CASE_PACKAGE

Use the following case classifications:

- MANIFEST_CASE_PRESENT
- MANIFEST_CASE_MISSING
- NON_MANIFEST_CASE_PRESENT

## REFIDS-only CINs

A labeled CIN found in refids.json but absent from household.json must be classified as:

NON_HOUSEHOLD_CIN

The presence of the CIN in refids.json may be recorded as supporting diagnostic
metadata, but must not change membership status.

Do not classify REFIDS-only CINs as household members.

## CINs absent from the case package

A labeled CIN not found in household.json and not found elsewhere in the corresponding
case package should be classified as:

LABEL_CIN_NOT_PRESENT_IN_CASE_PACKAGE

This is a candidate-label/data-quality discrepancy.

Do not remove or rewrite the ground-truth record.

Do not include such labels in TP/FP/TN/FN evaluation.

## Manifest handling

Inspect the available manifest workbook/file under local-data.

Determine:

- exact manifest filename
- worksheet(s)
- relevant case identifier column
- row count
- unique manifest case count
- duplicate case entries
- blank or malformed case identifiers

Do not infer the manifest case identifier column solely from position.

Use column names and structural inspection.

If the manifest structure is ambiguous, stop and report the ambiguity before
changing evaluation logic.

Do not print raw case identifiers in console output.

## Required implementation changes

Update the case-normalizer so that it can:

1. parse the manifest
2. determine manifest case membership
3. classify stored case directories as:
   - MANIFEST_CASE_PRESENT
   - NON_MANIFEST_CASE_PRESENT
4. report manifest cases missing from storage
5. extract household membership and line-number status
6. classify each household CIN as:
   - ACTIVE_HOUSEHOLD_MEMBER
   - INACTIVE_HOUSEHOLD_MEMBER
   - UNKNOWN_HOUSEHOLD_STATUS
7. validate ground-truth labels against:
   - manifest case membership
   - household membership
   - household active status
8. classify labels for non-household CINs
9. classify labels for CINs absent from the case package
10. calculate evaluation-eligible label counts

Do not calculate SNAPPER flag predictions.

## Required outputs

Update or create privacy-safe reports under:

local-data/generated/inventory/

At minimum:

- tranche_summary.json
- manifest_summary.json
- household_membership_summary.json
- ground_truth_summary.json
- label_join_summary.json
- evaluation_population_summary.json
- validation_errors.json

## manifest_summary.json

Include aggregate counts:

- manifest rows
- unique manifest cases
- duplicate manifest case entries
- blank/malformed case identifiers
- manifest cases present in storage
- manifest cases missing from storage
- non-manifest case directories present in storage

No raw case identifiers.

## household_membership_summary.json

Include:

- total CIN records in household.json
- active household members
- inactive household members
- unknown-status household members
- cases with no active household members
- cases with unknown membership status
- detected household line-number field/path
- line-number data types observed

Do not include raw CINs.

## label_join_summary.json

Include:

- labeled CINs in manifest cases
- labeled active household CINs
- labeled inactive household CINs
- labeled unknown-status household CINs
- labeled NON_HOUSEHOLD_CIN
- labeled LABEL_CIN_NOT_PRESENT_IN_CASE_PACKAGE
- labels attached to non-manifest cases
- valid complete three-flag sets
- invalid/incomplete label sets

## evaluation_population_summary.json

Report:

- manifest cases eligible for evaluation
- active household members in those cases
- active household members with complete ground truth
- active household members without complete ground truth
- candidate labels excluded because member is inactive
- candidate labels excluded because CIN is non-household
- candidate labels excluded because CIN is absent from the package
- candidate labels excluded because case is non-manifest
- total evaluation-eligible flag rows
- counts of eligible TRUE/FALSE labels by flag

Do not compute model metrics yet.

## Privacy requirements

Do not print or report:

- raw case numbers
- CINs
- names
- addresses
- phone numbers
- document IDs
- source reference IDs
- raw JSON values
- raw manifest case identifiers

Use surrogate identifiers where per-case diagnostics are necessary.

All real-data-derived output must remain under local-data/.

## Testing requirements

Add synthetic-only tests for:

- manifest-listed case present in storage
- manifest-listed case missing from storage
- non-manifest case present in storage
- duplicate manifest entries
- malformed manifest entry
- active household member
- inactive household member
- blank line-number behavior
- unknown membership status
- REFIDS-only CIN remains NON_HOUSEHOLD_CIN
- CIN absent from all case files becomes LABEL_CIN_NOT_PRESENT_IN_CASE_PACKAGE
- inactive member labels excluded from evaluation population
- non-manifest case labels excluded from evaluation population
- eligible active member with complete labels included
- deterministic results across repeated runs
- no raw identifiers in public reports

Do not use real data in committed tests.

## Backward compatibility

Preserve the Milestone 1 inventory capabilities.

Do not:

- remove prior validation outputs without replacement
- change public CLI behavior unnecessarily
- modify rules-service
- introduce flag logic
- introduce database dependencies
- introduce LLM calls

If the CLI needs a manifest argument, prefer:

python -m snapper_normalizer inventory \
  --input ../local-data/s3-min \
  --manifest ../local-data/<manifest-file> \
  --labels ../local-data/ground-truth/ground_truth_data.csv \
  --output ../local-data/generated/inventory

Determine the actual manifest filename before documenting the final command.

## Validation after implementation

Run:

1. pytest
2. the inventory CLI against local data
3. ./mvnw clean test in rules-service

Report only:

- changed files
- Python test results
- Java test results
- detected manifest structure
- manifest case counts
- active/inactive/unknown household counts
- label membership classifications
- evaluation-eligible population counts
- unresolved assumptions

Do not display raw case numbers or CINs.

Do not commit anything.

Wait for review and explicit approval.