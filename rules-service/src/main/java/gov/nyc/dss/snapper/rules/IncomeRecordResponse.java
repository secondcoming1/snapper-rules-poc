package gov.nyc.dss.snapper.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record IncomeRecordResponse(
    String ruleSetId,
    String ruleSetVersion,
    String evidenceId,
    String employerName,
    String documentType,
    BigDecimal amount,
    String frequency,
    YearMonth benefitMonth,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    boolean verified,
    String classification,
    String reasonCode
) {
}