Read AGENTS.md completely before making any changes.

The repository discovery plan is approved with the corrections and requirements below.

Do not modify the Java rules-service during this milestone.

Implement only Milestone 1: case inventory, JSON validation, and candidate ground-truth validation.

Before implementation:

1. Ensure the root instruction file is named exactly:

   AGENTS.md

2. Ensure the root .gitignore contains:

   local-data/
   .DS_Store
   .env
   .env.*
   *.pem
   *.key
   target/
   .venv/
   __pycache__/
   .pytest_cache/

3. Remove any local .DS_Store file if present.

4. Do not commit anything.

Create this package structure:

case-normalizer/
├── pyproject.toml
├── README.md
├── src/
│   └── snapper_normalizer/
│       ├── __init__.py
│       ├── __main__.py
│       ├── cli.py
│       ├── models.py
│       ├── file_classifier.py
│       ├── inventory.py
│       ├── json_validation.py
│       ├── label_validation.py
│       ├── label_join.py
│       ├── surrogate_ids.py
│       └── reporting.py
└── tests/
    ├── fixtures/
    ├── test_file_classifier.py
    ├── test_inventory.py
    ├── test_json_validation.py
    ├── test_label_validation.py
    ├── test_label_join.py
    ├── test_surrogate_ids.py
    └── test_cli.py

Use Python 3.12.

Use:

- type hints
- pathlib
- dataclasses or Pydantic models
- structured error handling
- deterministic output ordering
- SHA-256 for persistent surrogate identifiers
- pytest for tests

Do not use Python's built-in hash() for persistent identifiers.

Required CLI:

python -m snapper_normalizer inventory \
  --input ../local-data/s3-min \
  --labels ../local-data/ground-truth/ground_truth_data.csv \
  --output ../local-data/generated/inventory

Required input locations:

local-data/s3-min
local-data/ground-truth/ground_truth_data.csv

The current tranche contains:

- 22 case directories
- 206 JSON files
- 192 candidate ground-truth rows
- 20 distinct labeled cases
- 64 distinct labeled CINs
- 64 rows for each of FLAG_01, FLAG_02, and FLAG_03

Treat these counts as discovery expectations only. The implementation must calculate them rather than hard-code them.

File classification rules for Milestone 1:

Known filename-based categories:

- household.json → HOUSEHOLD
- income.json → INCOME
- budget.json → BUDGET
- rfi.json → RFI
- refids.json → REFIDS
- ivs_*.json → IVS

All other JSON files must initially be classified as:

UNCLASSIFIED_JSON

Do not call them "parsed document JSON" merely because they do not match the known filenames.

For UNCLASSIFIED_JSON files, record only privacy-safe structural metadata:

- filename pattern
- top-level JSON type: object, array, scalar, or null
- top-level key names only when the key names themselves contain no PII
- structural signature
- count by structural signature

Do not print or report raw values.

Do not assign a stronger source category unless the file structure provides sufficient evidence.

If the structure is ambiguous, keep the category:

UNCLASSIFIED_JSON

Non-JSON files inside case directories must be classified as:

NON_JSON

Required inventory behavior:

1. Discover every case directory under local-data/s3-min.
2. Assign a deterministic surrogate case ID.
3. Inventory every file in each case directory.
4. Classify each file using the categories above.
5. Parse every JSON file safely.
6. Record JSON syntax success or failure.
7. Distinguish JSON syntax validation from source-schema validation.
8. Do not claim source-schema validity unless an explicit schema exists and is applied.
9. Produce one case inventory file per case.
10. Produce aggregate tranche summaries.
11. Use deterministic output ordering.
12. Do not print raw case directory names.

Required candidate ground-truth validation:

Expected CSV columns:

- case_no
- cin
- flag_id
- flag_value

Valid flag IDs:

- FLAG_01
- FLAG_02
- FLAG_03

Valid flag values must normalize to Boolean:

- TRUE
- FALSE

Validate and report:

- total rows
- distinct cases
- distinct CINs
- counts by flag_id
- counts by flag_value
- missing required columns
- blank required fields
- invalid flag IDs
- invalid flag values
- duplicate case_no + cin + flag_id rows
- rows participating in duplicate groups
- duplicate excess rows
- household members with incomplete FLAG_01, FLAG_02, FLAG_03 label sets
- tranche cases with no labels
- labels whose cases are absent from the tranche
- labeled CINs absent from the corresponding household data

Do not print raw case numbers or CINs in console output or aggregate reports.

For mismatches, report only:

- surrogate case ID
- surrogate member ID
- error category
- count
- non-sensitive diagnostic code

Identity and privacy requirements:

1. Keep source identifiers private:

   - case_no
   - CIN
   - document IDs
   - source reference IDs
   - case folder names

2. Use de-identified working identifiers:

   - case_id
   - member_id
   - file_id
   - evidence_id

3. Use SHA-256 for deterministic surrogates.

4. If a salt is used:

   - read it from a local environment variable
   - do not hard-code it
   - do not commit it
   - do not write it to logs

5. Any private source-to-surrogate mapping must remain only under:

   local-data/generated/private

6. No committed output may contain real identifiers.

7. Do not print:

   - case numbers
   - CINs
   - names
   - addresses
   - phone numbers
   - document IDs
   - reference IDs
   - source JSON values
   - raw document text

Required outputs under:

local-data/generated/inventory

Create:

- cases/<surrogate_case_id>/case_inventory.json
- tranche_summary.json
- validation_errors.json
- ground_truth_summary.json
- label_join_summary.json

The case inventory should include only privacy-safe metadata such as:

- surrogate case ID
- file count
- counts by file category
- JSON parse status
- structural signatures
- validation issue categories
- presence or absence of known expected files

The tranche summary should include:

- number of case directories
- total files
- total JSON files
- total non-JSON files
- counts by file category
- JSON parse success count
- JSON parse failure count
- number of UNCLASSIFIED_JSON files
- counts by structural signature
- cases missing known expected files
- aggregate validation error counts

The ground-truth summary should include:

- row counts
- distinct case count
- distinct CIN count
- counts by flag
- counts by value
- duplicate statistics
- invalid-value statistics
- incomplete label-set statistics

The label-join summary should include:

- tranche cases with labels
- tranche cases without labels
- labels whose cases are absent from the tranche
- labeled CINs found in the corresponding household data
- labeled CINs absent from the corresponding household data
- complete and incomplete three-flag label sets

The validation errors file should contain only privacy-safe entries.

For each error, use fields such as:

- error_code
- error_category
- surrogate_case_id
- surrogate_member_id
- file_category
- safe_message

Do not include raw identifiers or source values.

Testing requirements:

1. All committed tests must use synthetic fixtures only.
2. Do not copy any real case file into tests.
3. Add tests for:

   - known filename classification
   - IVS filename classification
   - unknown JSON classification as UNCLASSIFIED_JSON
   - non-JSON classification
   - valid JSON parsing
   - malformed JSON handling
   - top-level object detection
   - top-level array detection
   - deterministic structural signature
   - deterministic surrogate IDs
   - required label-column validation
   - valid and invalid flag IDs
   - valid and invalid flag values
   - duplicate label detection
   - incomplete three-flag label-set detection
   - tranche case without labels
   - label for missing tranche case
   - labeled CIN missing from household data
   - CLI success path
   - no raw identifiers in generated aggregate reports

4. Tests must confirm deterministic output across repeated runs.

README requirements:

Document:

- Python 3.12 setup
- virtual-environment creation
- dependency installation
- pytest command
- CLI usage
- output locations
- privacy controls
- the distinction between JSON syntax validation and source-schema validation
- the provisional meaning of UNCLASSIFIED_JSON
- the fact that no flag calculation occurs in Milestone 1

Do not:

- calculate FLAG_01, FLAG_02, or FLAG_03
- infer labels
- normalize income or benefit evidence yet
- match employers
- perform evidence reconciliation
- use an LLM
- call Bedrock
- modify rules-service
- add PostgreSQL
- add Docker or containers
- add a web UI
- commit files
- alter candidate ground-truth labels

After implementation:

1. Create and use a Python 3.12 virtual environment if needed.
2. Run:

   pytest

3. Run the inventory CLI against the real local-data inputs.
4. Run:

   cd rules-service
   ./mvnw clean test

5. Report only:

   - changed files
   - Python test results
   - Java test results
   - aggregate inventory counts
   - aggregate ground-truth validation counts
   - aggregate label-join counts
   - unresolved assumptions
   - any files still classified as UNCLASSIFIED_JSON

6. Do not display raw case numbers, CINs, or source values.

7. Do not commit anything.

8. Wait for my review and approval.