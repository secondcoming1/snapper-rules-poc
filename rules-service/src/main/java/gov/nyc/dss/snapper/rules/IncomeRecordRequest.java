package gov.nyc.dss.snapper.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record IncomeRecordRequest(

    @NotBlank(message = "evidenceId is required")
    String evidenceId,

    @NotBlank(message = "employerName is required")
    String employerName,

    @NotBlank(message = "documentType is required")
    String documentType,

    @NotNull(message = "amount is required")
    @PositiveOrZero(message = "amount cannot be negative")
    BigDecimal amount,

    @NotBlank(message = "frequency is required")
    String frequency,

    @NotNull(message = "benefitMonth is required")
    YearMonth benefitMonth,

    LocalDate effectiveFrom,

    LocalDate effectiveTo,

    boolean verified
) {
}