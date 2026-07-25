# SNAPPER Rules POC

## 1. Project purpose

This repository implements a proof of concept for deterministic, auditable, and maintainable SNAPPER flag classification.

The project must support:

- Flag 1 — Earned-income discrepancy
- Flag 2 — Duplicate earned-income risk
- Flag 3 — Unearned-income discrepancy
- ingestion and normalization of real SNAPPER case packages
- evaluation against candidate ground-truth labels
- generation of synthetic and hybrid test cases
- versioned Apache KIE / Drools DMN rules
- LLM-assisted extraction and explanation
- golden-dataset creation
- regression testing
- metrics and impact analysis

The immediate objective is to build a reliable evaluation platform for a tranche of real test cases before expanding the rule set.

## 2. Architecture

The intended architecture is:

```text
Raw SNAPPER case packages
        ↓
Python ingestion and normalization
        ↓
Normalized Evidence Model
        ↓
Record matching and reconciliation
        ↓
Java 21 Spring Boot rules service
        ↓
Apache KIE / Drools DMN
        ↓
Structured flag decisions and reason codes
        ↓
LLM explanation service
        ↓
Evaluation, adjudication, and golden dataset
```

Technology boundaries:

- Java 21 hosts Spring Boot and Drools DMN.
- Python handles ingestion, normalization, synthetic generation, evaluation, and dataset utilities.
- PostgreSQL may later store evaluations, rule versions, and adjudications.
- DMN is authoritative for deterministic flag decisions.
- LLMs may extract facts, suggest ambiguous matches, and explain results.
- LLMs must not alter authoritative flag results.

## 3. Expected repository structure

```text
snapper-rules-poc/
├── AGENTS.md
├── README.md
├── docs/
├── rules-service/
├── case-normalizer/
├── evaluation-service/
├── synthetic-generator/
├── schemas/
├── datasets/
├── scripts/
└── local-data/
    ├── s3-min/
    ├── SNAPPER_Flag_Scenarios.xlsx
    └── ground-truth/
        └── ground_truth_data.csv
```

The exact package structure may evolve, but the architectural boundaries must remain intact.

## 4. Private data rules

`local-data/` contains sensitive or potentially sensitive real case data.

Mandatory controls:

- Never commit anything under `local-data/`.
- Never add real case numbers, CINs, names, addresses, document IDs, or source reference IDs to Git.
- Never copy raw real-case content into committed unit tests.
- Never print raw case numbers or CINs in console logs.
- Never include raw identifiers in generated summaries outside `local-data/`.
- Never send raw case data to an external LLM unless explicitly approved.
- Never store credentials, AWS keys, Bedrock tokens, GitHub tokens, certificates, or secrets in the repository.
- Never include real PII in screenshots, examples, documentation, or test output.

The root `.gitignore` must include:

```gitignore
local-data/
.env
.env.*
*.pem
*.key
target/
.venv/
__pycache__/
.pytest_cache/
```

Before committing, always check:

```bash
git status
git diff --cached --name-only
```

## 5. Local private data

Expected local files:

```text
local-data/s3-min/
local-data/SNAPPER_Flag_Scenarios.xlsx
local-data/ground-truth/ground_truth_data.csv
```

Each case folder may contain:

```text
household.json
income.json
budget.json
rfi.json
refids.json
ivs_<CIN>.json
parsed document JSON files
manifest-related files
```

Do not assume every case contains every file.

The code must tolerate:

- missing optional files
- null sections
- inconsistent capitalization
- strings used for numeric values
- strings used for Boolean values
- duplicate source records
- stale records
- malformed or incomplete evidence
- multiple household members
- multiple CINs
- multiple external source records
- repeated employer names

## 6. Candidate ground-truth labels

The candidate ground-truth CSV is located at:

```text
local-data/ground-truth/ground_truth_data.csv
```

Expected columns:

```text
case_no
cin
flag_id
flag_value
```

The label grain is:

```text
case number + CIN + flag
```

Valid flag identifiers:

```text
FLAG_01
FLAG_02
FLAG_03
```

Valid values normalize to Boolean:

```text
TRUE
FALSE
```

The ground-truth file is a candidate label source. It must not automatically be treated as final golden truth.

The evaluation pipeline must validate:

- total rows
- distinct cases
- distinct CINs
- counts by flag
- counts by Boolean value
- duplicate `case_no + cin + flag_id` combinations
- missing labels
- invalid flag identifiers
- invalid values
- labels for cases absent from the case tranche
- tranche cases with no labels
- household members with incomplete label sets

Do not expose raw identifiers in reports. Use hashes or surrogate identifiers.

## 7. Identity handling

Keep private source identity separate from de-identified working identity.

Private source identity:

```text
case_no
cin
source reference IDs
document IDs
```

De-identified working identity:

```text
case_id
member_id
evidence_id
income_line_id
```

Requirements:

- use deterministic hashing when stable joins are required
- never use Python's built-in `hash()` for persistent IDs
- use SHA-256 or another stable cryptographic hash
- use a locally configured salt when appropriate
- never commit salts or reversible mappings
- keep private-to-surrogate mappings only under `local-data/`

## 8. Normalized Evidence Model

Raw source JSON must not be passed directly to DMN.

Python must create a stable normalized evidence model.

The normalized case should support:

```json
{
  "caseId": "CASE-0001",
  "evaluationDate": "2026-07-24",
  "householdMembers": [],
  "earnedIncomeLines": [],
  "unearnedIncomeLines": [],
  "externalIncomeEvidence": [],
  "documentEvidence": [],
  "budget": {},
  "provenance": []
}
```

Each normalized field must preserve provenance.

Example:

```json
{
  "normalizedPath": "earnedIncomeLines[0].frequency",
  "sourceFileType": "income",
  "sourcePath": "$.WSAL[0].EVAL_UNITS",
  "rawValue": "03",
  "normalizedValue": "BIWEEKLY",
  "transformation": "INCOME_FREQUENCY_CODE_MAP"
}
```

Never discard raw values simply because a normalized value was produced.

## 9. Monetary calculations

Mandatory requirements:

- Python must use `Decimal`.
- Java must use `BigDecimal`.
- PostgreSQL must use `NUMERIC`.
- Never use binary floating-point values for money.
- Preserve original amount, frequency, and normalized monthly amount.
- Rounding rules must be explicit.
- Intermediate rounding must not occur unless policy requires it.
- Conversion rules must be versioned and tested.

Frequency normalization may include:

```text
WEEKLY
BIWEEKLY
SEMIMONTHLY
MONTHLY
QUARTERLY
ANNUAL
UNKNOWN
```

Every threshold requires tests for:

```text
below boundary
exactly at boundary
above boundary
```

For a $50 threshold, tests must include:

```text
$49.99
$50.00
$50.01
```

## 10. Date handling

- Use ISO 8601 dates.
- Python should use `date` and `datetime`.
- Java should use `LocalDate`, `YearMonth`, and `Instant`.
- Do not compare dates as raw strings.
- Clearly distinguish evidence date, pay date, pay-period dates, employment dates, benefit dates, benefit month, and evaluation date.
- Missing dates must produce an explicit classification.
- Future-dated records must not silently override current records.
- Stale evidence must be classified explicitly.
- Date ranges and tolerances must be versioned rules.

## 11. Source-system treatment

Supported source categories may include:

```text
SPOS / WMS self-reported income
TALX
RFI
IVS
parsed documents
manual evidence
other approved structured sources
```

Source records must retain:

```text
source system
source category
source reference
evidence date
member identity
employer or benefit name
effective dates
amount
frequency
status
provenance
```

Do not assume that an external source is always correct.

Do not assume that lack of external evidence means reported income is false.

Do not assume that repeated external records represent separate income sources.

## 12. Employer normalization and matching

Employer matching is a separate decision from flag determination.

Employer normalization may include:

- trim whitespace
- normalize case
- normalize punctuation
- normalize corporate suffixes
- normalize location or store-number suffixes
- normalize known aliases
- preserve original text
- retain match score and rationale

Do not merge employers merely because names are similar.

Possible match classifications:

```text
EXACT_MATCH
NORMALIZED_MATCH
KNOWN_ALIAS_MATCH
POSSIBLE_MATCH
NO_MATCH
AMBIGUOUS_MATCH
```

If an LLM suggests employer matching, its output is advisory only.

## 13. Flag definitions

### Flag 1 — Earned-income discrepancy

Flag 1 concerns earned income that is:

- missing from self-reported/SPOS data
- unsupported by available evidence
- materially inconsistent with evidence
- stale
- incorrectly classified
- associated with a cadence mismatch
- associated with an amount mismatch
- associated with employment-status mismatch
- associated with timing mismatch
- associated with missing required records
- associated with an unactioned reported change

Candidate classifications:

```text
EXTERNAL_EARNED_INCOME_NOT_REPORTED
SELF_REPORTED_INCOME_NOT_EXTERNALLY_CORROBORATED
AMOUNT_MISMATCH
FREQUENCY_MISMATCH
EMPLOYMENT_STATUS_MISMATCH
DATE_APPLICABILITY_MISMATCH
MISSING_REQUIRED_PAY_PERIODS
INSUFFICIENT_EVIDENCE_COVERAGE
STALE_EARNED_INCOME_EVIDENCE
INCOMPLETE_EXTERNAL_EVIDENCE
MATCHED_EARNED_INCOME
```

A lack of external corroboration is not automatically fraud or an error.

### Flag 2 — Duplicate earned-income risk

Flag 2 primarily concerns the same active earned-income relationship appearing more than once in SPOS.

The core decision grain is:

```text
same case
+ same household member
+ same normalized employer
+ overlapping active periods
+ multiple active SPOS earned-income lines
```

Flag 2 should consider:

- duplicate active self-reported lines
- one external employment matched to multiple SPOS lines
- duplicate lines with differing amounts
- duplicate lines with differing frequencies
- lines that appear to be superseded
- ended lines
- zero-payment history
- employer normalization
- record identity
- effective-period overlap

Flag 2 must not trigger solely because:

- two different household members work for the same employer
- the same document was uploaded more than once
- TALX returns repeated historical records
- employer names match across different CINs
- one line is clearly ended or superseded

Candidate classifications:

```text
NO_DUPLICATE_EARNED_INCOME
MULTIPLE_ACTIVE_LINES_SAME_MEMBER_EMPLOYER
LIKELY_DUPLICATE_REPORTED_LINES
VALID_MULTIPLE_REPORTED_LINES
SUPERSEDED_REPORTED_LINE
DUPLICATE_DOCUMENT_ONLY
CROSS_MEMBER_SAME_EMPLOYER
SAME_EXTERNAL_RECORD_MATCHED_TO_MULTIPLE_LINES
```

### Flag 3 — Unearned-income discrepancy

Flag 3 concerns supported unearned income or benefits that are:

- missing from SPOS
- materially inconsistent with evidence
- stale
- incorrectly classified
- assigned the wrong benefit type
- associated with a frequency mismatch
- associated with an effective-date mismatch
- associated with overlapping or duplicate benefit records
- active in SPOS after termination
- absent despite current external evidence

Candidate benefit types:

```text
UNEMPLOYMENT
SOCIAL_SECURITY
SSI
PENSION
WORKERS_COMPENSATION
EMPLOYER_DISABILITY
CHILD_SUPPORT
CHILDCARE_PROVIDER_SUBSIDY
OTHER_SUPPORTED_UNEARNED_INCOME
```

Candidate classifications:

```text
EXTERNAL_UNEARNED_INCOME_NOT_REPORTED
UNEARNED_AMOUNT_MISMATCH
UNEARNED_FREQUENCY_MISMATCH
BENEFIT_STATUS_MISMATCH
BENEFIT_TYPE_MISMATCH
BENEFIT_EFFECTIVE_DATE_MISMATCH
STALE_BENEFIT_EVIDENCE
OVERLAPPING_BENEFITS
MATCHED_UNEARNED_INCOME
```

## 14. Separation between classification and workflow

The rules engine performs classification.

It must not directly perform operational routing.

Avoid outputs such as:

```text
ROUTE_FOR_REVIEW
SEND_TO_SUPERVISOR
BLOCK_CASE
DENY_CASE
```

Prefer explicit classifications such as:

```text
CONFLICTING_CURRENT_INCOME
INSUFFICIENT_DATE_INFORMATION
UNCLASSIFIED_SOURCE
AMBIGUOUS_EMPLOYER_MATCH
```

A separate downstream workflow may decide what action to take.

## 15. Rules-service responsibilities

The Java rules service should:

- host versioned DMN models
- validate rule-model loading
- accept normalized evidence
- return deterministic classifications
- return reason codes
- return rule-set ID and version
- return evidence references
- return calculation details
- remain stateless where practical
- fail clearly when a rule model cannot load
- never silently default unknown input to a valid category

Controllers must remain thin.

Business logic must live in service classes, DMN models, shared components, and validation layers.

## 16. DMN design principles

Prefer small composable decisions over one enormous decision table.

Shared decisions may include:

```text
Income Source Classification
Frequency Normalization
Monthly Conversion
Evidence Applicability
Active Status
Evidence Precedence
Employer Match Classification
Amount Comparison
Coverage Sufficiency
Duplicate-Line Classification
Benefit-Type Classification
```

Every DMN model must include:

```text
model name
namespace
rule-set ID
semantic version
effective date
policy reference
change description
```

Unknown or unsupported input must remain explicit.

## 17. Rule maintainability

The project must demonstrate that meaningful rule changes can occur without rewriting controllers or orchestration code.

A valid maintainability demonstration should include:

- at least two rule-set versions
- same input evaluated against both versions
- changed classification
- impact report across regression cases
- no unexplained outcome changes
- historical reproducibility

Prefer meaningful changes such as:

- evidence precedence
- benefit-month applicability
- active/ended classification
- employer normalization
- duplicate-line treatment
- required evidence coverage
- source-selection rules

## 18. LLM responsibilities

LLMs may be used in three controlled areas.

### A. Evidence extraction

The LLM may extract structured facts from documents:

```text
employer name
gross amount
pay frequency
pay date
pay-period dates
benefit type
benefit amount
employment dates
supporting excerpt
confidence
```

The output must be validated against JSON Schema.

### B. Ambiguous entity matching

The LLM may suggest whether two employers or benefit sources refer to the same entity.

Its result must be advisory.

### C. Explanation generation

The LLM may convert deterministic results into user-friendly language.

The LLM must not:

- change a flag result
- recalculate values
- alter classifications
- invent evidence
- infer fraud or intent
- introduce unsupported policy
- omit material reasons
- expose internal identifiers
- expose CINs, case numbers, or document IDs
- make eligibility determinations beyond the structured result

The deterministic decision result remains authoritative.

## 19. LLM explanation contract

The rules service should produce canonical structured explanations.

Example:

```json
{
  "flagCode": "FLAG_02",
  "result": true,
  "classification": "MULTIPLE_ACTIVE_LINES_SAME_MEMBER_EMPLOYER",
  "reasonCode": "TWO_ACTIVE_SPOS_LINES_MATCH_SAME_MEMBER_EMPLOYER",
  "facts": {
    "activeLineCount": 2,
    "employerDisplayName": "Example Employer"
  }
}
```

The system must validate:

- every monetary amount against the deterministic result
- every evidence reference against the input
- every classification against the rule response
- absence of prohibited identifiers
- absence of unsupported conclusions

A deterministic fallback explanation must always be available.

## 20. Ingestion milestone

The first Python milestone is inventory and validation only.

It must:

1. inventory every case folder
2. classify files by source type
3. parse every JSON file safely
4. produce one inventory per case
5. produce a tranche summary
6. produce validation errors
7. validate candidate labels
8. avoid raw PII in logs
9. avoid rule calculation
10. avoid LLM use

The initial CLI should resemble:

```bash
python -m snapper_normalizer inventory   --input local-data/s3-min   --labels local-data/ground-truth/ground_truth_data.csv   --output local-data/generated/inventory
```

Generated real-data artifacts must remain under `local-data/`.

## 21. Scenario workbook handling

The workbook:

```text
local-data/SNAPPER_Flag_Scenarios.xlsx
```

contains rule scenarios for Flags 1–3.

The initial scenario importer should extract:

```text
scenario_id
flag
category
scenario text
expected result
notes
automation status
```

Scenarios should be categorized as:

```text
AUTOMATABLE
MESSAGE_VALIDATION
CONFIDENCE_VALIDATION
NEEDS_RULE_CLARIFICATION
```

Do not invent structured inputs when the workbook does not contain enough detail.

## 22. Synthetic data generator

Synthetic generation must be deterministic.

Input:

```text
scenario template
integer seed
optional de-identified base case
mutation list
```

Output:

```text
canonical_case.json
expected_result.json
generation_manifest.json
source-style rendered JSON
```

The same template and seed must always produce identical output.

Initial supported mutations:

```text
REMOVE_SPOS_EARNED_LINE
ADD_DUPLICATE_ACTIVE_SPOS_LINE
ADD_DUPLICATE_ENDED_SPOS_LINE
CHANGE_AMOUNT_BY_49_99
CHANGE_AMOUNT_BY_50_00
CHANGE_AMOUNT_BY_50_01
CHANGE_FREQUENCY
REMOVE_PAY_PERIOD
MAKE_EVIDENCE_STALE
MAKE_EVIDENCE_FUTURE_DATED
MOVE_EMPLOYER_TO_DIFFERENT_MEMBER
DUPLICATE_DOCUMENT_ONLY
ADD_ZERO_PAYMENT_HISTORY
ADD_CONFLICTING_EXTERNAL_RECORD
```

## 23. Hybrid synthetic cases

Real cases may be used as templates only after de-identification.

Workflow:

```text
real case
→ normalize
→ de-identify
→ approve base case
→ apply controlled synthetic mutation
→ generate known expected outcome
```

Never mutate raw real files in place.

Label hybrid cases as:

```text
HYBRID_SYNTHETIC
```

## 24. Golden dataset governance

A case does not become golden merely because it is real or labeled.

A golden case must have:

- frozen input
- successful normalization
- reviewed expected flags
- reviewed reason codes
- adjudication status
- reviewer role
- review date
- policy or scenario reference
- documented ambiguity
- dataset version

Adjudication states:

```text
UNREVIEWED
NORMALIZATION_ERROR
NEEDS_POLICY_CLARIFICATION
APPROVED_GOLDEN
EXCLUDED_DATA_QUALITY
```

The current prompt-based SNAPPER result is evidence, not authoritative truth.

The candidate ground-truth CSV is evidence, not automatically final truth.

## 25. Evaluation metrics

Calculate metrics independently for each flag:

```text
true positives
false positives
true negatives
false negatives
precision
recall
F1
false-positive rate
accuracy
```

Also calculate:

```text
reason-code accuracy
normalization failure rate
unknown classification rate
ambiguous match rate
order invariance
repeat-run consistency
explanation factual consistency
latency p50
latency p95
latency p99
```

Metrics must be sliced by:

```text
real versus synthetic
flag
source type
income type
single-member versus multi-member
boundary cases
date scenarios
missing-data scenarios
employer-normalization scenarios
structured versus document-derived evidence
```

## 26. Error analysis

Every false positive or false negative must be assigned to a failure category:

```text
NORMALIZATION_ERROR
MATCHING_ERROR
POLICY_INTERPRETATION_ERROR
RULE_IMPLEMENTATION_ERROR
SOURCE_DATA_QUALITY
GROUND_TRUTH_ERROR
MISSING_POLICY
LLM_EXTRACTION_ERROR
LLM_EXPLANATION_ERROR
```

Do not fix a rule merely to make a single unexplained case pass.

Every confirmed defect should become a regression test.

## 27. Testing standards

Every rule or transformation requires:

- positive test
- negative test
- exact-boundary test
- below-boundary test
- above-boundary test
- missing-data test
- null-data test
- order-independence test
- repeatability test
- unsupported-value test

Java tests:

```bash
cd rules-service
./mvnw clean test
```

Python tests:

```bash
pytest
```

Committed tests must use synthetic data only.

Real case data may be used for local evaluation but not committed.

## 28. Property-based testing

Use property-based tests where useful.

Examples:

- reordering records does not change the result
- monthly conversion is deterministic
- duplicate-document count alone does not affect Flag 2
- moving an otherwise identical employer record to another member prevents same-member duplicate classification
- an ended line does not count as active
- a threshold behaves correctly around $50
- identical seed and template produce identical synthetic output
- de-identification produces stable surrogate IDs
- no generated fixture contains real identifiers

## 29. Code-quality standards

### Java

- Java 21
- constructor injection
- immutable records where practical
- `BigDecimal` for money
- `LocalDate`, `YearMonth`, and `Instant` for dates
- thin controllers
- service-layer orchestration
- clear exceptions
- no silent defaults
- no business logic embedded in REST controllers

### Python

- Python 3.12
- type hints
- `Decimal` for money
- dataclasses or Pydantic models
- explicit error handling
- `pathlib`
- deterministic outputs
- structured logging
- no PII in logs
- pytest tests
- JSON Schema validation

## 30. Logging rules

Logs may include:

```text
surrogate case ID
file counts
validation count
error category
rule-set version
run ID
elapsed time
```

Logs must not include:

```text
case number
CIN
name
address
document ID
source reference ID
raw document text
full source JSON
```

## 31. Generated output rules

Generated outputs that contain real or reversible identifiers must remain under:

```text
local-data/
```

Committed generated outputs must be:

- synthetic
- de-identified
- reviewed
- schema validated
- free of secrets and PII

## 32. Codex operating instructions

Codex must:

1. read this file before making changes
2. inspect existing code before proposing changes
3. provide a concise file plan before broad modifications
4. make small, reviewable changes
5. run relevant tests
6. summarize changed files
7. report unresolved assumptions
8. preserve architectural boundaries
9. avoid unrelated refactoring
10. avoid modifying private local data
11. never add `local-data/` to Git
12. never include raw PII in responses
13. never weaken tests merely to pass a build
14. never change golden labels without explicit approval
15. never silently reinterpret rule definitions
16. never make an LLM authoritative for a deterministic flag
17. never generate production policy from inference
18. never delete source provenance

## 33. Codex approval boundaries

Codex may proceed without asking for approval when:

- adding tests for already agreed behavior
- fixing compilation errors
- adding validation
- adding typing
- adding documentation
- adding a small isolated utility
- formatting code
- implementing an explicitly approved file plan

Codex must stop and ask before:

- changing a flag definition
- changing a threshold
- changing source precedence
- changing active/ended logic
- changing ground-truth labels
- changing de-identification strategy
- introducing a new external service
- sending real data to an LLM
- changing repository-wide architecture
- deleting files
- changing schema fields used by multiple services
- committing generated real-case artifacts
- adding a database dependency
- modifying security or privacy controls

## 34. Initial development sequence

Follow this order unless explicitly changed.

### Iteration 1

- inventory real case tranche
- parse JSON safely
- validate file structure
- validate candidate labels
- generate de-identified summaries
- no flags
- no LLM

### Iteration 2

- define normalized evidence schema
- normalize household members
- normalize earned-income lines
- normalize unearned-income lines
- normalize TALX/RFI/IVS evidence
- preserve provenance
- no final flags yet

### Iteration 3

- implement Flag 2 first
- active/ended classification
- employer normalization
- same-member grouping
- duplicate-line classification
- different-member exclusion
- duplicate-document exclusion

### Iteration 4

- implement core Flag 1
- missing reported income
- missing external corroboration
- amount comparison
- frequency normalization
- evidence currency
- status and timing mismatch

### Iteration 5

- implement core Flag 3
- benefit classification
- amount comparison
- frequency conversion
- status and timing mismatch
- missing benefit detection

### Iteration 6

- cross-flag scenarios
- simultaneous flags
- flag independence
- ordering invariance
- repeatability

### Iteration 7

- LLM extraction
- explanation generation
- grounding checks
- PII checks
- deterministic fallback explanations

### Iteration 8

- synthetic generator
- hybrid generator
- golden-dataset adjudication
- regression suite
- rule-version impact analysis

## 35. Immediate first task

Create a Python `case-normalizer` package that inventories and validates:

```text
local-data/s3-min
local-data/ground-truth/ground_truth_data.csv
```

It must produce:

```text
one case inventory per case
tranche_summary.json
validation_errors.json
ground_truth_summary.json
label_join_summary.json
```

It must not:

```text
calculate Flags 1, 2, or 3
call an LLM
modify the Java rules service
print raw case numbers or CINs
commit real-data outputs
```

All outputs derived from real data must remain under:

```text
local-data/generated/
```

## 36. Definition of done for the first milestone

The first milestone is complete when:

- every case folder is inventoried
- every JSON file is parsed or reported as invalid
- every file is assigned a source type or marked unknown
- required and optional files are identified
- candidate labels are validated
- duplicate labels are reported
- missing labels are reported
- labels with no matching case are reported
- summaries contain no raw identifiers
- all unit tests use synthetic fixtures
- `pytest` passes
- Java tests still pass
- Git status contains no private data
- documentation explains how to run the inventory command
