# Task 005 — QA Rule and Evidence Model Extraction

## Status

APPROVED FOR ANALYSIS

## Objective

Analyze the QA workbook:

```text
local-data/20 Cases Review with 30 Day Period Naval.xlsx
```

or the actual local path where this workbook is stored.

The purpose of this task is to extract the human QA review logic, evidence applicability rules, source-linkage rules, review-window behavior, and decision-state semantics that should inform the normalized evidence model.

This is an analysis task.

Do not:

- implement Flag 01, Flag 02, or Flag 03 logic
- modify `rules-service`
- change Task 004 pipeline eligibility logic
- alter ground-truth labels
- use an LLM
- call Bedrock
- add a database
- add Docker or containers
- commit anything

The output of this task is a documented rule/evidence model proposal for review.

---

## Workbook structure already observed

The workbook contains:

```text
Rules Catalog
Flag Defination
20 case-specific worksheets
30 Day Flag Analysis
False Positives
30 Day Flag Raised
30 Day Decision Counts
90 Day Flag Analysis
90 Day Flags Raised
90 Day Decision Counts
```

The 20 case worksheets correspond to the current manifest evaluation set.

Do not hard-code the worksheet count. Discover it.

The workbook currently includes a 30-day review period of:

```text
05/21/2026 through 06/20/2026
```

Treat this as the review period for this QA artifact, not as a universal production rule.

---

## Important caution about the "30-day rule"

The workbook's Rules Catalog contains a broader principle:

```text
Do not use a universal 30-day document rule.
Evaluate evidence against the budget/reporting period and income type.
Use period relevance, not document age alone.
```

Therefore:

- the QA workbook may use a 30-day review window for this analysis
- that does not automatically establish a permanent universal policy rule
- evidence applicability must be modeled separately from simple document age
- preserve the distinction between:
  - review window
  - budget period
  - reporting period
  - pay period
  - payment date
  - effective date
  - source date
  - document date

Do not translate "30-day review" into a hard-coded production threshold without explicit approval.

---

## Observed decision-state semantics

The `30 Day Flag Analysis` worksheet defines:

```text
Yes    = flag raised
Review = worker validation required
No     = flag not supported
```

Task 005 must determine how consistently those semantics are applied.

Do not collapse `Review` into TRUE or FALSE.

Candidate internal decision states should be evaluated:

```text
FLAG
NO_FLAG
REVIEW
INSUFFICIENT_EVIDENCE
NOT_APPLICABLE
```

The final recommendation may refine these states.

---

## Observed flag definitions

The workbook contains the following current flag families.

### Flag 1 — Earned Income / Income Sources Disagree

Observed detail codes include:

```text
pay_cadence_mismatch
net_vs_gross_mismatch
missing_earned_line
income_timing_mismatch
income_sources_disagree
reported change not actioned
employment status incorect
```

Normalize spelling only in the proposed model. Do not alter the source workbook.

### Flag 2 — Duplicate Earned Income

Observed detail code:

```text
duplicate_earned_income_lines
```

### Flag 3 — Unearned Benefits Disagree

Observed detail codes include:

```text
benefit_status_mismatch
benefit_type_transition
unearned_frequency_cadence_mismatch
employment_status_incorrect
missing_unearned_line
benefit_type_outdated
overlapping_benefits
```

Do not assume this list is exhaustive until the Rules Catalog and case worksheets are reviewed.

---

## Existing source/member-linkage findings

Use the following already-established structural findings as context.

### `income.json`

Observed identifiers:

```text
EVAL_CIN
REF_ID
MATCH_REF_ID
RESULT_DETAIL_ID
```

Observed linkage:

- 106 income records total in the structural investigation
- 101 contain direct `EVAL_CIN`
- 5 lack `EVAL_CIN`
- all 5 can be uniquely mapped through `REF_ID` / `MATCH_REF_ID` and `refids.json`

Household membership must still be validated against authoritative `household.json`.

### `rfi.json`

Observed groups:

```text
NQRF01
NQRF02
NQRF03
NQRF11
```

Observed linkage:

- NQRF02: CIN-bearing
- NQRF03: CIN-bearing
- NQRF11: CIN-bearing
- NQRF01: case/line identifiers but no CIN

The relationship between:

```text
NQRF01.CASE_SUFFIX
NQRF01.LINE_NO
```

and authoritative household membership is not yet confirmed.

Do not invent that mapping.

### `ivs_*.json`

The filename CIN directly identifies the member.

Payload `REF_ID` may support cross-source matching but is not required for member linkage.

### `refids.json`

Top-level member identifier keys map to numeric reference IDs.

REFIDS is not authoritative for household membership.

Historical/non-household CINs may exist there.

### Previously `UNCLASSIFIED_JSON`

The structural investigation found that the 38 files currently classified as
`UNCLASSIFIED_JSON` contain:

```text
case_num
cin
f_docnumber
doc_type
```

and all 38 have populated top-level `cin`.

Task 005 may determine whether the QA workbook demonstrates that these are document-derived records.

Do not rename the production category solely from this workbook unless the evidence is sufficient and the change is explicitly proposed for later approval.

---

## Required workbook analysis

Analyze the workbook without modifying it.

### 1. Rules Catalog

Extract and categorize every rule in `Rules Catalog`.

For each rule capture:

```text
rule_id
rule_category
rule_name
rule_statement
implementation_note
```

Classify each rule into one or more candidate domains:

```text
CORE_FRAMEWORK
MEMBER_LINKAGE
SOURCE_APPLICABILITY
EVIDENCE_RECENCY
EVIDENCE_COMPLETENESS
INCOME_STATUS
EMPLOYER_NORMALIZATION
AMOUNT_COMPARISON
FREQUENCY
DATE_APPLICABILITY
EARNED_INCOME
UNEARNED_INCOME
DOCUMENT_QUALITY
IVS
IEVS
RFI
TALX
WORK_NUMBER
FISA
OCSE
OTHER
AUDITABILITY
MANUAL_REVIEW
```

Do not invent categories when a rule does not fit cleanly. Use `OTHER` and explain.

Identify:

- rules that are deterministic
- rules that require configurable policy
- rules that require manual review
- rules that are advisory/lead-generation only
- rules that appear contradictory or overlapping

---

### 2. Flag definition sheet

Extract the source flag definitions from `Flag Defination`.

Produce a normalized proposal mapping:

```text
flag_id
flag_family
source_detail_code
normalized_detail_code
description
```

Do not change the workbook.

Identify:

- spelling inconsistencies
- duplicate meanings
- missing definitions
- codes that overlap multiple flag families

---

### 3. 30-day flag analysis

Analyze every row in `30 Day Flag Analysis`.

Capture privacy-safe structured information for each case:

```text
surrogate_case_id
review_window_start
review_window_end

flag_1_decision
flag_1_reason_category
flag_1_evidence_categories
flag_1_rule_ids

flag_2_decision
flag_2_reason_category
flag_2_evidence_categories
flag_2_rule_ids

flag_3_decision
flag_3_reason_category
flag_3_evidence_categories
flag_3_rule_ids
```

Do not expose raw case numbers or CINs in generated committed artifacts.

For local-only analysis under `local-data`, raw values may be read but public summaries must remain de-identified.

---

## 4. Decision-state analysis

Determine what evidence patterns lead QA to:

```text
Yes
Review
No
```

For each flag, identify recurring reason patterns.

Examples to investigate include:

### Flag 1

- current earned income missing from SPOS
- current SPOS wage amount unsupported by current evidence
- stale pay evidence
- undated pay evidence
- conflicting current external evidence
- final pay during review period after employment termination
- historical wage lead only
- zero-value source record
- no current earned source established
- quarterly evidence requiring validation
- employer/source mismatch

### Flag 2

- same employer across different household members
- alias/abbreviation of same employer
- same member with duplicate active employer lines
- source duplicates versus counted-income duplicates
- historical employer records
- zero-value lines
- potential duplicate requiring review

### Flag 3

- historical benefit records
- expired benefits
- zero-payment benefits
- current benefit amount mismatch
- missing unearned budget line
- current obligation versus actual payment
- benefit status/type mismatch
- insufficient current payment evidence

Identify which patterns are deterministic and which lead to `Review`.

---

## 5. Evidence applicability model

Derive a candidate evidence-applicability model from the workbook.

At minimum consider:

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

Do not force these exact names if the workbook supports a better taxonomy.

For every proposed applicability state provide:

```text
definition
evidence attributes required
example rule IDs
effect on downstream comparison
whether manual review is required
```

---

## 6. Date and review-period model

Extract how QA uses:

```text
review_window_start
review_window_end
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
document_date
refresh_date
quarter
budget_period
```

Determine which date is used for which type of evidence.

Specifically analyze examples where:

- pay date falls just outside the review window
- final payment falls inside the window after employment ended
- quarterly data is current-ish but not sufficient for a hard flag
- old UIB/PUA/WRS data is historical
- a document exists but has no usable pay date
- source date differs from event/payment date

Do not use `document_date` as a universal recency field.

---

## 7. Source applicability and precedence

The Rules Catalog explicitly states that there should not be a universal source hierarchy.

Extract how the workbook treats:

```text
SPOS
IVS
IEVS
RFI
TALX
Work Number
FISA
OCSE
documents
agency records
```

For each source identify:

```text
source_role
member_linkage_method
income_types supported
date fields used
whether it can establish current income
whether it is lead-generation only
known lag/staleness behavior
conditions requiring corroboration
conditions requiring manual review
```

Do not invent a single ranking like:

```text
SOURCE_A > SOURCE_B > SOURCE_C
```

unless the workbook explicitly establishes it for a specific income type and period.

---

## 8. Employer normalization requirements

Extract employer-normalization behavior from the Rules Catalog and case analyses.

Examples already observed include:

```text
UTA
United Talmudical Academy
```

and:

```text
NYPD
Police Department
```

Determine candidate normalization requirements such as:

```text
case normalization
punctuation normalization
corporate suffix normalization
agency abbreviation
DBA / parent-company relationship
payroll processor
known alias
location/store suffix
```

Distinguish:

```text
EXACT_MATCH
NORMALIZED_MATCH
KNOWN_ALIAS
POSSIBLE_MATCH_REVIEW
NO_MATCH
```

Do not implement employer matching yet.

---

## 9. NQRF01 investigation

Use the QA workbook to determine whether there is any documented basis for joining
NQRF01 rows to household members.

Current structure:

```text
CASE_NO
CASE_SUFFIX
LINE_NO
```

but no CIN.

Investigate whether case worksheets or rule documentation demonstrate:

```text
NQRF01.LINE_NO → household CLNT_LINE_NO
```

or any other deterministic member linkage.

If the workbook does not establish this:

```text
NQRF01_MEMBER_LINKAGE_UNRESOLVED
```

must remain the conclusion.

Do not infer the mapping merely because both fields contain line numbers.

---

## 10. Case worksheet analysis

Analyze the 20 case-specific worksheets structurally.

Determine common sections and evidence layouts.

Extract a schema of recurring concepts such as:

```text
household/member data
SPOS budgets
earned income
unearned income
TALX
IVS
RFI
IEVS/WRS
documents
employment status
benefit status
calculations
review notes
flag decisions
```

Do not create production parsers for the workbook.

The purpose is to understand the QA adjudication process and normalized evidence requirements.

---

## 11. False-positive and flag-raised analysis

Analyze:

```text
False Positives
30 Day Flag Raised
30 Day Decision Counts
```

Determine:

- recurring false-positive causes
- recurring confirmed-flag causes
- rules most frequently involved
- evidence-quality problems
- employer-normalization problems
- stale-data problems
- missing-line problems
- status/timing problems

Produce aggregate categories only.

---

## 12. Compare 30-day and 90-day analyses

The workbook also contains:

```text
90 Day Flag Analysis
90 Day Flags Raised
90 Day Decision Counts
```

Compare the 30-day and 90-day analyses.

Determine:

- which decisions change
- which flags change
- what evidence becomes applicable under 90 days
- whether widening the review window increases false positives
- whether some cases move from Review to Yes/No
- whether any source types become disproportionately influential

The purpose is to show whether the review-window choice materially changes outcomes.

Do not conclude that 30 or 90 days is the correct production policy unless policy explicitly says so.

---

## Required outputs

Write local real-data-derived analysis under:

```text
local-data/generated/qa-rule-analysis/
```

Create:

```text
rules_catalog_extracted.json
flag_definition_map.json
decision_state_analysis.json
evidence_applicability_model.json
date_applicability_model.json
source_applicability_model.json
employer_normalization_requirements.json
nqrf01_linkage_analysis.json
case_sheet_structure_summary.json
false_positive_analysis.json
review_window_comparison.json
qa_model_summary.json
```

All outputs must remain ignored by Git.

---

## `qa_model_summary.json`

Include a privacy-safe executive summary:

```text
rules_extracted
deterministic_rule_candidates
configurable_policy_candidates
manual_review_rule_candidates
advisory_lead_rules
decision_states_observed

flag_1_yes_count
flag_1_review_count
flag_1_no_count

flag_2_yes_count
flag_2_review_count
flag_2_no_count

flag_3_yes_count
flag_3_review_count
flag_3_no_count

30_day_window_start
30_day_window_end

90_day_comparison_available
NQRF01_linkage_status
```

For the current workbook, the existing 30-day summary indicates:

```text
Flag 1: Yes=10, Review=2, No=8
Flag 2: Yes=0, Review=2, No=18
Flag 3: Yes=0, Review=2, No=18
```

Calculate these from the workbook; do not hard-code them.

---

## Required documentation proposal

Create a tracked proposal document:

```text
docs/qa-evidence-model-proposal.md
```

This document must contain no raw case numbers or CINs.

It should summarize:

1. QA decision workflow
2. observed decision states
3. evidence applicability taxonomy
4. source applicability rules
5. date/review-period behavior
6. employer-normalization requirements
7. member-linkage requirements
8. unresolved NQRF01 linkage
9. candidate normalized evidence fields
10. candidate reason-code taxonomy
11. deterministic versus review-required decisions
12. differences between Task 004 assumptions and QA practice
13. recommendations for Task 006 normalized evidence model

Do not treat this proposal as final policy.

Explicitly label it:

```text
Candidate model derived from QA adjudication artifact; requires policy/business validation.
```

---

## Candidate normalized evidence fields

Task 005 should propose, but not implement, fields covering at least:

```text
case_id
member_id
household_status
pipeline_eligibility

source_system
source_record_id
income_category
income_type

employer_raw
employer_normalized
employer_match_status

amount_raw
amount_normalized
frequency_raw
frequency_normalized

employment_status
benefit_status

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

review_window_start
review_window_end
period_applicability

evidence_quality
evidence_completeness
evidence_status

member_linkage_method
member_linkage_confidence

source_provenance
raw_source_path

decision_state
reason_codes
manual_review_required
```

Refine this list based on the workbook.

---

## Candidate reason-code taxonomy

Extract recurring reason concepts and propose normalized codes.

Potential Flag 1 candidates include:

```text
CURRENT_INCOME_MISSING_FROM_SPOS
CURRENT_SP0S_INCOME_UNVERIFIED
STALE_PAY_EVIDENCE
UNDATED_PAY_EVIDENCE
CURRENT_SOURCE_CONFLICT
FINAL_PAY_MISSING
HISTORICAL_WAGE_LEAD_ONLY
QUARTERLY_WAGE_NEEDS_VERIFICATION
EMPLOYMENT_STATUS_UNRESOLVED
```

Potential Flag 2 candidates include:

```text
SAME_MEMBER_SAME_EMPLOYER_DUPLICATE
EMPLOYER_ALIAS_REVIEW
DIFFERENT_MEMBERS_SAME_EMPLOYER
SOURCE_RECORD_DUPLICATE_ONLY
HISTORICAL_EMPLOYER_RECORD
```

Potential Flag 3 candidates include:

```text
CURRENT_BENEFIT_MISSING_FROM_SPOS
BENEFIT_AMOUNT_MISMATCH
BENEFIT_STATUS_MISMATCH
BENEFIT_TYPE_MISMATCH
HISTORICAL_BENEFIT_ONLY
EXPIRED_BENEFIT
ZERO_PAYMENT_BENEFIT
OBLIGATION_WITHOUT_PAYMENT_EVIDENCE
```

These are candidates only.

Use the workbook to refine and consolidate them.

---

## Comparison with Task 004

Explicitly identify where Task 004 is too coarse or unsupported.

In particular investigate:

```text
EXCLUDED_ACTIVE_NO_INCOME
EXCLUDED_ACTIVE_NO_RELEVANT_SOURCE_DATA
PIPELINE_ELIGIBILITY_UNDETERMINED
```

Determine whether QA practice suggests that:

- source presence alone is insufficient
- zero-value records may still be leads
- historical source records should not establish current income
- current external income can exist even when SPOS budget is zero
- a member may need review even without an obvious current SPOS amount

Do not change Task 004 code during Task 005.

Instead provide recommended changes for later approval.

---

## Testing / validation

This task is analysis-focused.

If code is written to extract workbook structure:

- keep it under an analysis utility or script
- use synthetic tests for any committed parser logic
- do not commit real workbook-derived data
- do not alter existing production normalizer behavior

Validate that:

1. all workbook worksheets are inventoried
2. all 20 case worksheets are detected
3. all Rules Catalog rows are accounted for
4. 30-day decisions reconcile with decision-count sheets
5. 90-day decisions reconcile with their count sheets
6. raw identifiers do not appear in tracked proposal documentation
7. all local extracted data remains under `local-data/`
8. `rules-service` is unchanged
9. ground truth is unchanged
10. no LLM or Bedrock call occurs
11. no commit is created

---

## Required final report

At completion report:

### Workbook structure

- sheet count
- case-sheet count
- rule count
- review windows found

### 30-day decisions

- Yes / Review / No counts by flag

### 90-day decisions

- Yes / Review / No counts by flag

### Decision changes

- count of cases whose decisions differ between 30 and 90 days
- change categories by flag

### Evidence applicability

- proposed states
- rules supporting each state

### Source model

- source roles
- linkage methods
- applicability behavior
- unresolved source issues

### Employer normalization

- confirmed alias/normalization patterns
- review-required patterns

### NQRF01

- whether member linkage is resolved
- evidence supporting or rejecting a linkage rule

### Task 004 implications

- which current eligibility assumptions remain valid
- which should be revised
- which cannot yet be determined

### Task 006 proposal

- recommended normalized evidence entities
- required fields
- reason-code taxonomy
- review-state model

### Safeguards

Confirm:

- no raw identifiers in tracked output
- rules-service unchanged
- ground truth unchanged
- no LLM calls
- no commit created

Wait for explicit review and approval before implementing Task 006.
