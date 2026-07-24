package gov.nyc.dss.snapper.rules;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;
import org.springframework.stereotype.Service;

@Service
public class IncomeRecordApplicabilityService {

    private static final String NAMESPACE =
        "https://dss.nyc.gov/snapper/rules/income-applicability/v1";

    private static final String MODEL_NAME =
        "Income Record Applicability";

    private static final String RULE_SET_ID =
        "INCOME-RECORD-APPLICABILITY";

    private static final String RULE_SET_VERSION =
        "1.0.0";

    private final KieSession kieSession;
    private final DMNRuntime dmnRuntime;
    private final DMNModel dmnModel;

    public IncomeRecordApplicabilityService() {
        KieServices kieServices = KieServices.Factory.get();

        KieContainer kieContainer =
            kieServices.getKieClasspathContainer();

        this.kieSession = kieContainer.newKieSession();

        this.dmnRuntime =
            kieSession.getKieRuntime(DMNRuntime.class);

        this.dmnModel =
            dmnRuntime.getModel(NAMESPACE, MODEL_NAME);

        if (dmnModel == null) {
            throw new IllegalStateException(
                "Could not load DMN model: " + MODEL_NAME
            );
        }

        if (dmnModel.hasErrors()) {
            throw new IllegalStateException(
                "DMN model contains errors: "
                    + formatMessages(dmnModel.getMessages())
            );
        }
    }

    public IncomeRecordResponse classify(
        IncomeRecordRequest request
    ) {
        LocalDate benefitMonthStart =
            request.benefitMonth().atDay(1);

        LocalDate benefitMonthEnd =
            request.benefitMonth().atEndOfMonth();

        DMNContext context = dmnRuntime.newContext();

        context.set(
            "Benefit Month Start",
            benefitMonthStart
        );

        context.set(
            "Benefit Month End",
            benefitMonthEnd
        );

        context.set(
            "Effective From",
            request.effectiveFrom()
        );

        context.set(
            "Effective To",
            request.effectiveTo()
        );

        context.set(
            "Verified",
            request.verified()
        );

        context.set(
            "Document Type",
            request.documentType()
                .trim()
                .toUpperCase()
        );

        DMNResult result =
            dmnRuntime.evaluateAll(dmnModel, context);

        if (result.hasErrors()) {
            throw new IllegalStateException(
                "DMN evaluation failed: "
                    + formatMessages(result.getMessages())
            );
        }

        Object decisionValue = result
            .getContext()
            .get("Applicability Classification");

        if (!(decisionValue instanceof Map<?, ?> decision)) {
            throw new IllegalStateException(
                "Unexpected DMN result: " + decisionValue
            );
        }

        Object classification =
            decision.get("classification");

        Object reasonCode =
            decision.get("reasonCode");

        if (classification == null || reasonCode == null) {
            throw new IllegalStateException(
                "DMN result did not contain classification "
                    + "and reasonCode"
            );
        }

        return new IncomeRecordResponse(
            RULE_SET_ID,
            RULE_SET_VERSION,
            request.evidenceId(),
            request.employerName(),
            request.documentType()
                .trim()
                .toUpperCase(),
            request.amount(),
            request.frequency()
                .trim()
                .toUpperCase(),
            request.benefitMonth(),
            request.effectiveFrom(),
            request.effectiveTo(),
            request.verified(),
            classification.toString(),
            reasonCode.toString()
        );
    }

    private static String formatMessages(
        Collection<DMNMessage> messages
    ) {
        return messages.stream()
            .map(DMNMessage::getMessage)
            .reduce(
                (first, second) -> first + "; " + second
            )
            .orElse("Unknown DMN error");
    }
}