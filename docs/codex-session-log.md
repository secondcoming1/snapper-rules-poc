# Codex session log

## 2026-07-26 — Task 002-inventory-milestone

Implemented only Milestone 1 case inventory, JSON syntax validation, candidate
ground-truth validation, and privacy-safe reporting.

### Requirement-to-file mapping

- Root hygiene and private-data exclusions:
  - `AGENTS.md`
  - `.gitignore`
- Python 3.12 package configuration and usage/privacy documentation:
  - `case-normalizer/pyproject.toml`
  - `case-normalizer/README.md`
- CLI and deterministic report orchestration:
  - `case-normalizer/src/snapper_normalizer/__main__.py`
  - `case-normalizer/src/snapper_normalizer/cli.py`
  - `case-normalizer/src/snapper_normalizer/reporting.py`
- Privacy-safe models, file classification, JSON syntax/structure inspection,
  inventory aggregation, label validation, household-member joins, and
  salted SHA-256 surrogate IDs:
  - `case-normalizer/src/snapper_normalizer/__init__.py`
  - `case-normalizer/src/snapper_normalizer/models.py`
  - `case-normalizer/src/snapper_normalizer/file_classifier.py`
  - `case-normalizer/src/snapper_normalizer/json_validation.py`
  - `case-normalizer/src/snapper_normalizer/inventory.py`
  - `case-normalizer/src/snapper_normalizer/label_validation.py`
  - `case-normalizer/src/snapper_normalizer/label_join.py`
  - `case-normalizer/src/snapper_normalizer/surrogate_ids.py`
- Synthetic-only tests for classification, parsing, signatures, labels, joins,
  privacy, CLI behavior, and deterministic repeated output:
  - `case-normalizer/tests/conftest.py`
  - `case-normalizer/tests/fixtures/README.md`
  - `case-normalizer/tests/test_file_classifier.py`
  - `case-normalizer/tests/test_inventory.py`
  - `case-normalizer/tests/test_json_validation.py`
  - `case-normalizer/tests/test_label_validation.py`
  - `case-normalizer/tests/test_label_join.py`
  - `case-normalizer/tests/test_surrogate_ids.py`
  - `case-normalizer/tests/test_cli.py`
- Task traceability:
  - `docs/codex-session-log.md`

### Verification

- Python 3.12 pytest: 27 passed.
- Real local inventory: 22 case directories and 206 JSON files inventoried;
  all JSON syntax parsed successfully; 38 files remained
  `UNCLASSIFIED_JSON`.
- Candidate labels: 192 rows, 20 distinct labeled cases, 64 distinct labeled
  CINs, complete three-flag sets for all 64 labeled members, and no duplicate
  or invalid labels.
- Label joins: 20 tranche cases with labels, 2 without labels, 0 label cases
  absent from the tranche, 50 labeled CINs found in household data, and 14
  absent from household data.
- Public-output exact identifier audit: 0 matches.
- Java Maven tests: 1 passed; build successful; no rules-service source files
  changed.
- No commit created.

## 2026-07-27 — Task 004-evaluation-population-and-manifest-alignment

> Superseded by the revised Task 004 entry below. The earlier active-household
> denominator did not represent confirmed production-pipeline execution scope.

Aligned Milestone 1 evaluation eligibility to the authoritative manifest and
active household membership contract. No flag predictions or model metrics
were added.

### Requirement-to-file mapping

- Manifest XLSX parsing, `case_no` validation, worksheet metadata, duplicate
  handling, and privacy-safe manifest issues:
  - `case-normalizer/src/snapper_normalizer/manifest.py`
  - `case-normalizer/tests/test_manifest.py`
- Dynamic household line-number discovery and active/inactive/unknown status:
  - `case-normalizer/src/snapper_normalizer/household_membership.py`
  - `case-normalizer/tests/test_household_membership.py`
- Evaluation population, authoritative household membership, REFIDS-only and
  absent-package exclusions, eligible row counts, and flag/value aggregates:
  - `case-normalizer/src/snapper_normalizer/evaluation_population.py`
  - `case-normalizer/tests/test_evaluation_population.py`
- Manifest-aware stored-case inventory and private package-presence evidence:
  - `case-normalizer/src/snapper_normalizer/inventory.py`
  - `case-normalizer/src/snapper_normalizer/json_validation.py`
  - `case-normalizer/tests/test_inventory.py`
- CLI arguments and required reports:
  - `case-normalizer/src/snapper_normalizer/cli.py`
  - `case-normalizer/tests/test_cli.py`
- Backward-compatible Milestone 1 label join:
  - `case-normalizer/src/snapper_normalizer/label_join.py`
- Synthetic XLSX fixture generation:
  - `case-normalizer/tests/conftest.py`
- Setup, manifest command, output, and membership-contract documentation:
  - `case-normalizer/README.md`
- Task traceability:
  - `docs/codex-session-log.md`

### Verification

- Python 3.12 pytest: 33 passed.
- Manifest: 20 rows and 20 unique cases; no duplicate, blank, malformed, or
  missing-storage cases; 2 non-manifest directories remain inventoried.
- Household: 59 CIN records; 59 active, 0 inactive, and 0 unknown; detected
  line-number path `$.household_members[].CLNT_LINE_NO` with numeric values.
- Labels: 50 active household CINs, 7 non-household CINs, 7 CINs absent from
  the case package, and no inactive, unknown-status, non-manifest, invalid, or
  incomplete label sets.
- Evaluation population: 20 cases, 54 active household members, 50 members
  with complete ground truth, 4 without, and 150 eligible flag rows.
- Java Maven tests: 1 passed; build successful; no rules-service source files
  changed.
- No commit created.

## 2026-07-27 — Revised Task 004 pipeline-validation alignment

Replaced active-household eligibility with a separate, conservative pipeline
eligibility classification. Ground truth remains candidate evidence and does
not establish whether production processed a member. No flags, predictions, or
model metrics were calculated.

### Requirement-to-file mapping

- Per-member income/source-presence evidence, explicit exclusion precedence,
  pipeline eligibility, missing-data dependencies, and de-identified audits:
  - `case-normalizer/src/snapper_normalizer/pipeline_eligibility.py`
  - `case-normalizer/tests/test_pipeline_eligibility.py`
- Pipeline-gated validation denominator and eligible label aggregates:
  - `case-normalizer/src/snapper_normalizer/evaluation_population.py`
  - `case-normalizer/tests/test_evaluation_population.py`
- Pipeline report orchestration and required output generation:
  - `case-normalizer/src/snapper_normalizer/cli.py`
  - `case-normalizer/tests/test_cli.py`
- Population-layer and conservative-undetermined documentation:
  - `case-normalizer/README.md`
- Task traceability:
  - `docs/codex-session-log.md`

### Privacy-safe aggregate verification

- Python 3.12 pytest: 50 passed.
- Manifest: 20 rows and 20 unique cases; all 20 present in storage; 2 stored
  case directories are non-manifest.
- Household: 59 members, all active; 54 active members belong to manifest
  cases.
- Pipeline candidates: 73 total; 0 eligible and 73 excluded. Exclusions were
  32 active members with no income, 14 non-household labeled members, 5 members
  in non-manifest cases, and 22 members whose pipeline eligibility cannot be
  determined from the supplied evidence.
- Validation denominator: 0 eligible members and 0 eligible flag rows; all 192
  ground-truth rows remain excluded from evaluation.
- Java Maven tests: 1 passed; build successful; no rules-service source files
  changed.
- No ground-truth changes and no commit created.

### Unresolved data dependencies

- No authoritative per-CIN WMS/SPOS case or member status is supplied.
- No confirmed production-pipeline processed/skipped indicator is supplied.
- The semantics and household-line join for `NQRF01.CASE_SUFFIX` are not
  established, so it is not used to infer per-member status.
- SPOS absence is provisional when no member-linked income record exists;
  ground truth is never used to fill that gap.

## 2026-07-27 — Task 005 QA rule and evidence-model extraction

Analyzed the QA workbook as an adjudication artifact and produced a candidate
evidence-model proposal. No production normalizer behavior, pipeline
eligibility, flags, ground truth, or rules-service code was changed.

### Requirement-to-file mapping

- QA workflow, decision states, evidence/date/source applicability, employer
  normalization, member linkage, NQRF01 conclusion, Task 004 implications, and
  Task 006 recommendations:
  - `docs/qa-evidence-model-proposal.md`
- Privacy-safe real-workbook-derived analysis (ignored by Git):
  - `local-data/generated/qa-rule-analysis/rules_catalog_extracted.json`
  - `local-data/generated/qa-rule-analysis/flag_definition_map.json`
  - `local-data/generated/qa-rule-analysis/decision_state_analysis.json`
  - `local-data/generated/qa-rule-analysis/evidence_applicability_model.json`
  - `local-data/generated/qa-rule-analysis/date_applicability_model.json`
  - `local-data/generated/qa-rule-analysis/source_applicability_model.json`
  - `local-data/generated/qa-rule-analysis/employer_normalization_requirements.json`
  - `local-data/generated/qa-rule-analysis/nqrf01_linkage_analysis.json`
  - `local-data/generated/qa-rule-analysis/case_sheet_structure_summary.json`
  - `local-data/generated/qa-rule-analysis/false_positive_analysis.json`
  - `local-data/generated/qa-rule-analysis/review_window_comparison.json`
  - `local-data/generated/qa-rule-analysis/qa_model_summary.json`
- Task traceability:
  - `docs/codex-session-log.md`

### Privacy-safe verification

- Workbook: 29 sheets, including 20 case-specific sheets.
- Rules Catalog: 122 unique rules extracted and categorized.
- Both the 30-day and 90-day detailed analyses contain 20 cases and reconcile
  exactly with their decision-count sheets.
- Thirty-day decisions: Flag 1 Yes/Review/No = 10/2/8; Flag 2 = 0/2/18;
  Flag 3 = 0/2/18.
- Ninety-day decisions: Flag 1 Yes/Review/No = 3/1/16; Flag 2 = 0/2/18;
  Flag 3 = 1/1/18.
- Eleven cases change at least one decision between review windows.
- NQRF01 member linkage remains unresolved; the workbook contains no explicit
  line/suffix-to-household mapping rule.
- All 12 local JSON outputs parse successfully.
- Exact raw-identifier audit: 0 matches in the tracked proposal or local
  analysis outputs.
- No LLM or Bedrock calls and no commit created.
