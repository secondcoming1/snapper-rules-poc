package gov.nyc.dss.snapper.rules;

public record IncomeClassificationResponse(
    String ruleSetId,
    String ruleSetVersion,
    String sourceType,
    String classification
) {
}