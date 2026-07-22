package gov.nyc.dss.snapper.rules;

import jakarta.validation.constraints.NotBlank;

public record IncomeClassificationRequest(
    @NotBlank(message = "sourceType is required")
    String sourceType
) {
}