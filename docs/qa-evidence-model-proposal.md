# QA Evidence Model Proposal

> Candidate model derived from QA adjudication artifact; requires
> policy/business validation.

## Scope and safeguards

This proposal analyzes the QA workbook's human review method. It does not
define production policy, calculate Flags 01–03, change candidate ground
truth, or change Task 004 pipeline eligibility. Case and member identifiers
are omitted; case-level local analysis uses salted surrogate identifiers.

The workbook contains 29 sheets: a Rules Catalog, a flag-definition sheet, 20
case workspaces, and seven aggregate 30-day/90-day analysis sheets. All 122
catalog rules and all 20 detailed rows in each review window were accounted
for. Both decision-count sheets reconcile exactly with their detailed sheets.

## 1. QA decision workflow

The observed workflow is evidence-first rather than source-first:

1. Select the artifact review window and the relevant SNAP budget/reporting
   period.
2. Establish household member, income type, employer/payer, and period.
3. Inventory SPOS and available external/document evidence without assuming
   any source is universally authoritative.
4. Classify each item for attribution, status, period applicability,
   completeness, and conflicts.
5. Compare only records aligned to the same member, source relationship,
   income type, and relevant period.
6. Separate a source-level difference from a reportable/actionable variance.
7. Emit Yes, No, or Review with the evidence and rule rationale retained.

The workbook's 30-day rule is specific to that QA artifact. Core rule
`INC-09` explicitly rejects a universal 30-day document-age rule, while
`DSS30-01` applies the narrower window for this review. Production policy must
keep those scopes separate.

## 2. Decision-state model

Observed workbook values map to the following candidate states:

| Workbook value | Candidate state | Meaning |
|---|---|---|
| Yes | `FLAG` | QA supports raising the flag for the artifact and window. |
| No | `NO_FLAG` | Supplied applicable evidence does not support the flag. |
| Review | `REVIEW` | Worker validation is required; this is neither true nor false. |

Two additional evidence/workflow states are needed:

- `INSUFFICIENT_EVIDENCE`: required identity, date, amount, period, or status
  information is absent.
- `NOT_APPLICABLE`: the evidence or flag family does not apply to the member,
  income type, or period.

The model should preserve an evidence classification separately from the
final decision state. A `REVIEW` may result from incomplete, conflicting,
unattributable, or policy-dependent evidence.

## 3. Evidence-applicability taxonomy

| State | Meaning | Representative rules | Comparison effect |
|---|---|---|---|
| `CURRENT_ACTIONABLE` | Attributable evidence establishes the relevant period and supports an actionable comparison. | `INC-02`, `INC-03`, `EARN-10` | Include after member/source/type/period alignment. |
| `CURRENT_CORROBORATING` | Current evidence supports another current record. | `IVS-08`, `EARN-05` | Use as corroboration; do not double-count. |
| `HISTORICAL_LEAD` | Historical evidence suggests a source but does not establish current income. | `INC-08`, `IVS-04`, `IEVS-02`, `IEVS-09` | Retain for investigation; exclude from a hard current comparison. |
| `STALE_FOR_REVIEW_PERIOD` | Evidence does not prove the artifact's applicable period. | `INC-09`, `DOC-09`, `DSS30-01` | Does not satisfy artifact-specific verification. |
| `FUTURE` | Event or receipt occurs after the applicable period. | `INC-02`, `DOC-08`, `CALC-01` | Do not apply to an earlier received-income period. |
| `ZERO_VALUE` | A record exists but shows no relevant payment or amount. | `INC-10`, `UNEAR-08` | Preserve as status/lead evidence; do not infer current receipt. |
| `INCOMPLETE` | Required identity, amount, date, period, or status is missing. | `DOC-01`, `DOC-02`, `IVS-07`, `UNEAR-09` | No hard discrepancy; review where material. |
| `UNDATED` | No usable event or coverage date establishes applicability. | `DOC-02`, `DOC-08`, `DSS30-01` | Cannot satisfy time-bound verification. |
| `UNATTRIBUTABLE` | Evidence cannot be defensibly linked to a member or source. | `INC-03`, `DOC-05`, `IVS-07` | Exclude from member-level hard comparison. |
| `CONFLICTING` | Current aligned sources materially disagree. | `INC-06`, `EARN-10` | Preserve all facts; apply approved resolution or review. |
| `SUPERSEDED` | A later includable event or amendment replaces an earlier record. | `DOC-09`, `VAR-03`, `VAR-04` | Use the latest includable fact while preserving history. |
| `NOT_APPLICABLE` | Evidence is outside the member/type/period comparison. | `INC-01`–`INC-03` | Exclude without discarding provenance. |
| `NEEDS_VERIFICATION` | A credible lead cannot establish a deterministic current result. | `IVS-04`, `IEVS-06`, `UNEAR-07`, `UNEAR-09` | Produce review, not a Boolean flag. |

Applicability should be derived from explicit attributes, not a single
`is_current` Boolean.

## 4. Source-applicability model

There is no workbook-supported universal source ranking.

| Source | Observed role | Member linkage | Applicability behavior |
|---|---|---|---|
| SPOS | Reported/budgeted baseline and agency-action history | Direct normalized member identity | May be stale, unprocessed, or wrong; it is not automatically authoritative. |
| IVS | Separate external verification | Direct source identity/filename CIN where supplied | Can corroborate current income; historical, quarterly, undated, incomplete, or status-less records are lead/review evidence. |
| IEVS/WRS | Employment lead generation | Member identity must be present or independently established | Updates may lag by up to two quarters; current status requires corroboration. |
| RFI | Program-specific external records | Direct CIN for CIN-bearing groups | Current attributable records may support comparison; historical and zero-payment rows do not establish receipt. NQRF01 remains unresolved. |
| TALX/Work Number | Payroll and employment evidence | Member plus employer/source relationship | Current pay may establish income; historical employment remains a lead. |
| FISA | Agency/payroll evidence | Member attribution required | Applicability depends on attributable current-period payment. |
| OCSE | Child-support payment evidence | Recipient/member and payer linkage | An obligation does not necessarily establish actual receipt. |
| Documents | Primary or corroborating evidence depending on type | Explicit member identifier or defensible multi-member split | Usability depends on identity, gross amount, event dates, period, and completeness. |
| Agency records | Benefit/payment verification | Member and payer/benefit linkage | Current official evidence can establish benefit amount/status for the relevant period. |

Source presence alone is insufficient. Each record needs a role such as
baseline, corroboration, lead, primary verification, or conflicting evidence.

## 5. Date and review-period behavior

The workbook compares two artifact windows:

- 30-day review: `2026-05-21` through `2026-06-20`.
- 90-day comparison: cutoff `2026-03-24`, anchor `2026-06-22`.

Eleven of 20 cases change at least one decision between the two analyses.
Flag 1 accounts for ten changed transitions; Flag 3 accounts for one; Flag 2
does not change. This is strong evidence that period selection materially
affects evidence applicability, but not evidence that either window is the
correct production policy.

Required date roles:

- `pay_date`: receipt event for wages.
- `pay_period_start` / `pay_period_end`: wage coverage.
- `receipt_date`: receipt-month treatment for payments and benefits.
- `employment_start_date` / `employment_end_date`: anticipated, active,
  ended, or historical employment.
- `final_pay_date`: applicability of post-termination payment.
- `benefit_effective_date` / `benefit_end_date`: benefit status and coverage.
- `source_date` / `refresh_date`: source currency metadata, not substitutes
  for event dates.
- `document_date`: document metadata only, never universal recency.
- `quarter`: quarterly/IEVS lead period requiring applicability analysis.
- `budget_period`: target period for comparison.

Evidence just outside a review window, final pay after termination, quarterly
data, and undated documents must retain distinct classifications. They cannot
all be reduced to stale/current.

## 6. Employer normalization

The catalog and case analyses support:

- case, whitespace, and punctuation normalization;
- corporate-suffix normalization;
- curated organization and agency abbreviations;
- known aliases;
- DBA and parent-company relationships;
- payroll-processor relationships;
- location or store suffix handling.

Candidate match states are `EXACT_MATCH`, `NORMALIZED_MATCH`, `KNOWN_ALIAS`,
`POSSIBLE_MATCH_REVIEW`, and `NO_MATCH`. Similar names, unknown abbreviations,
payroll processors, and unverified DBA/parent relationships require review.
An employer shared by different household members is not a same-member
duplicate.

## 7. Member linkage

Every comparison requires a defensible member linkage and must still validate
membership against authoritative `household.json`.

- Direct source CIN: preferred where supplied.
- REFID mapping: acceptable for records lacking direct CIN only when the map
  is unique and retained as provenance.
- Filename CIN: acceptable for IVS when the filename contract is validated.
- Document CIN: acceptable when the document record explicitly attributes the
  evidence to that member.
- Multi-member document split: each amount must be attributed independently.
- Case-only or ambiguous line linkage: `UNATTRIBUTABLE` until an authoritative
  mapping exists.

The model should retain `member_linkage_method`, `member_linkage_confidence`,
and the source mapping references used.

## 8. NQRF01 conclusion

The workbook references NQRF01 in case evidence, but it does not document or
demonstrate a deterministic mapping from `NQRF01.LINE_NO` or
`NQRF01.CASE_SUFFIX` to household `CLNT_LINE_NO`. Similar field names are not
sufficient evidence of a join contract.

The required status is:

```text
NQRF01_MEMBER_LINKAGE_UNRESOLVED
```

Resolution requires an authoritative data dictionary, production join
specification, or reviewed mapping table.

## 9. Candidate normalized evidence entities and fields

### Evaluation context

- `case_id`, `evaluation_date`
- `review_window_start`, `review_window_end`, `review_window_type`
- `budget_period`, `reporting_period`, `reporting_regime`
- `policy_version`, `artifact_rule_scope`

### Household member

- `member_id`, `household_status`, `pipeline_eligibility`
- `member_linkage_method`, `member_linkage_confidence`
- `member_linkage_evidence`, `member_linkage_status`

### Income relationship

- `income_line_id`, `member_id`, `income_category`, `income_type`
- `source_system`, `source_record_id`
- `employer_raw`, `employer_normalized`, `employer_match_status`
- `payer_raw`, `payer_normalized`
- `employment_status`, `benefit_status`
- `amount_raw`, `amount_normalized`
- `frequency_raw`, `frequency_normalized`

### Evidence item

- `evidence_id`, `source_system`, `source_category`, `source_record_id`
- `amount_raw`, `amount_normalized`, `frequency_raw`, `frequency_normalized`
- `pay_date`, `pay_period_start`, `pay_period_end`, `receipt_date`
- `effective_date`, `end_date`, `final_pay_date`
- `source_date`, `refresh_date`, `document_date`, `quarter`
- `period_applicability`, `evidence_quality`, `evidence_completeness`
- `evidence_status`, `source_role`, `manual_review_required`
- `source_provenance`, `raw_source_path`

### Comparison and decision

- `comparison_id`, aligned member/source/type/period keys
- `decision_state`, `reason_codes`, `manual_review_required`
- `accepted_evidence_ids`, `rejected_evidence_ids`, `lead_evidence_ids`
- `calculation_details`, `policy_reference`, `reviewer_outcome`

Raw and normalized values must coexist; provenance may not be discarded.

## 10. Candidate reason-code taxonomy

### Shared evidence codes

- `MEMBER_LINKAGE_UNRESOLVED`
- `EVIDENCE_OUTSIDE_APPLICABLE_PERIOD`
- `EVIDENCE_UNDATED`
- `EVIDENCE_INCOMPLETE`
- `EVIDENCE_UNATTRIBUTABLE`
- `HISTORICAL_SOURCE_LEAD_ONLY`
- `ZERO_VALUE_SOURCE_LEAD_ONLY`
- `CURRENT_SOURCE_CONFLICT`
- `POLICY_CONFIGURATION_REQUIRED`

### Flag 1 candidates

- `CURRENT_INCOME_MISSING_FROM_SPOS`
- `CURRENT_SPOS_INCOME_UNVERIFIED`
- `STALE_PAY_EVIDENCE`
- `UNDATED_PAY_EVIDENCE`
- `FINAL_PAY_APPLICABILITY_UNRESOLVED`
- `QUARTERLY_WAGE_NEEDS_VERIFICATION`
- `EMPLOYMENT_STATUS_UNRESOLVED`
- `REPORTED_CHANGE_NOT_ACTIONED`

### Flag 2 candidates

- `SAME_MEMBER_SAME_EMPLOYER_DUPLICATE`
- `EMPLOYER_ALIAS_REVIEW`
- `DIFFERENT_MEMBERS_SAME_EMPLOYER`
- `SOURCE_RECORD_DUPLICATE_ONLY`
- `HISTORICAL_EMPLOYER_RECORD`
- `SUPERSEDED_EARNED_LINE`

### Flag 3 candidates

- `CURRENT_BENEFIT_MISSING_FROM_SPOS`
- `BENEFIT_AMOUNT_MISMATCH`
- `BENEFIT_STATUS_MISMATCH`
- `BENEFIT_TYPE_MISMATCH`
- `HISTORICAL_BENEFIT_ONLY`
- `EXPIRED_BENEFIT`
- `ZERO_PAYMENT_BENEFIT`
- `OBLIGATION_WITHOUT_PAYMENT_EVIDENCE`

These codes describe evidence and comparison outcomes; they do not implement
flag results.

## 11. Deterministic versus review-required decisions

Deterministic candidates include structural membership, unique validated
identifier joins, date parsing, exact period inclusion, zero/nonzero amounts,
record completeness checks, exact/curated alias matches, and preservation of
source history.

Review remains necessary for ambiguous employer relationships, unattributable
records, conflicting current sources without approved precedence, quarterly
leads lacking current corroboration, undated or incomplete material evidence,
uncertain final-pay applicability, benefit obligations without payment proof,
and policy-dependent reportability/actionability.

## 12. Task 004 implications

Task 004 assumptions that remain valid:

- manifest scope, authoritative household membership, and household activity
  are separate population gates;
- ground truth does not establish pipeline eligibility;
- unresolved pipeline execution remains explicitly excluded;
- REFIDS alone does not establish household membership.

Assumptions requiring later revision or refinement—not in Task 005 code:

- `EXCLUDED_ACTIVE_NO_INCOME` is too coarse if based only on a missing or zero
  SPOS amount. Current external income may exist while SPOS is zero, and a
  zero-value historical record may still be a lead.
- `EXCLUDED_ACTIVE_NO_RELEVANT_SOURCE_DATA` must consider usable,
  member-attributable, period-applicable data rather than file/source presence.
- A source record can be present yet historical, incomplete, zero-value,
  unattributable, or otherwise unable to establish current income.
- `PIPELINE_ELIGIBILITY_UNDETERMINED` remains appropriate where actual
  production processing and authoritative status cannot be reproduced.

No Task 004 behavior is changed by this proposal.

## 13. Recommendations for Task 006

1. Define versioned schemas for evaluation context, member identity, income
   relationship, evidence item, applicability assessment, comparison, and
   decision rationale.
2. Preserve raw and normalized values with field-level provenance.
3. Model review window separately from budget/reporting and event periods.
4. Implement evidence applicability before income comparison.
5. Preserve `REVIEW`, `INSUFFICIENT_EVIDENCE`, and `NOT_APPLICABLE` as
   first-class states.
6. Require explicit member linkage and retain how the join was established.
7. Keep NQRF01 unattributable until its join contract is approved.
8. Make source authority, reporting regimes, thresholds, conversion factors,
   and timeframes versioned policy configuration.
9. Add synthetic boundary, missing-date, conflicting-source, historical-lead,
   zero-value, multi-member, and ambiguous-employer cases before any flag
   implementation.
10. Obtain business validation of the candidate taxonomy before encoding DMN
    decisions.
