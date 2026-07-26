Read AGENTS.md completely before doing anything else.

Inspect the current repository and the private local-data directory.

Do not create, edit, move, or delete any files yet.
Do not print raw case numbers, CINs, names, addresses, document IDs,
reference IDs, or source JSON contents.

Report only:

1. The current repository structure.
2. The Java rules-service classes, REST endpoints, and DMN models.
3. The number of case directories under local-data/s3-min.
4. Aggregate file counts by filename category:
   - household.json
   - income.json
   - budget.json
   - rfi.json
   - refids.json
   - ivs_*.json
   - parsed document JSON
   - unknown files
5. Whether every JSON file parses successfully.
6. The detected columns in:
   local-data/ground-truth/ground_truth_data.csv
7. Aggregate ground-truth counts:
   - total rows
   - distinct cases
   - distinct CINs
   - counts by flag_id
   - counts by flag_value
   - duplicate case_no + cin + flag_id rows
8. A proposed file plan for the Python case-normalizer package.

Use surrogate values or aggregate counts only.
Do not calculate Flags 1, 2, or 3.
Do not use an LLM on the case contents.
Wait for my approval after presenting the plan.



The proposed case-normalizer plan is approved.

Implement only Milestone 1: inventory and validation.

Requirements:

1. Create the Python package using Python 3.12.
2. Provide this CLI:

   python -m snapper_normalizer inventory \
     --input ../local-data/s3-min \
     --labels ../local-data/ground-truth/ground_truth_data.csv \
     --output ../local-data/generated/inventory

3. Inventory every case directory and classify every file.
4. Parse every JSON file safely.
5. Validate the ground-truth CSV.
6. Create:
   - one case_inventory.json per case
   - tranche_summary.json
   - validation_errors.json
   - ground_truth_summary.json
   - label_join_summary.json
7. Do not place raw case numbers or CINs in aggregate reports.
8. Use deterministic surrogate IDs based on a locally configured salt.
9. Keep any private identifier mapping under local-data/generated/private.
10. Do not calculate any flags.
11. Do not use an LLM.
12. Do not modify rules-service.
13. Add pytest tests using synthetic fixtures only.
14. Update case-normalizer/README.md with setup and run instructions.
15. Run all Python tests and the existing Java tests.
16. Show the changed-file summary and test results.
17. Do not commit anything; I will review first.

Implemented Milestone 1 without modifying rules-service or calculating flags.

  Created:

  - case-normalizer/pyproject.toml
  - case-normalizer/README.md
  - Package modules for CLI, inventory, classification, safe JSON parsing, label validation, joins, reporting, models, and HMAC-SHA-256 surrogate IDs.
  - Synthetic-only pytest fixtures and tests covering CLI output, privacy, invalid JSON, classification, labels, joins, duplicates, missing labels, and deterministic IDs.

  Generated under ignored local-data/generated/:

  - 22 case inventories
  - tranche_summary.json
  - validation_errors.json
  - ground_truth_summary.json
  - label_join_summary.json
  - Permission-restricted private identity mapping
  - Permission-restricted persisted local salt

  Validation results:

  - JSON parse failures: 0
  - Validation errors: 2
  - Both errors: TRANCHE_CASE_WITHOUT_LABELS
  - Warnings: 0
  - Raw identifier matches in public reports: 0
  - Private mapping and salt permissions: 0600

  Tests:

  - Python 3.12: 11 passed
  - Java: 1 passed, Maven BUILD SUCCESS
  - Java emitted existing deprecation/Mockito warnings only.

  Repository status:

  - case-normalizer/ is untracked and ready for review.
  - Generated private data is ignored by local-data/.
  - No commit was created.
  - Existing unrelated .gitignore modification and untracked docs/ directory were preserved.
  - rules-service source files were not changed.

  Assumption documented in the README: household.json and income.json are required for Milestone 1; all other file categories are optional.

