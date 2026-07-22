package gov.nyc.dss.snapper.rules;

import java.util.Collection;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;
import org.springframework.stereotype.Service;

@Service
public class IncomeClassificationService {

    private static final String NAMESPACE =
        "https://dss.nyc.gov/snapper/rules/income/v1";

    private static final String MODEL_NAME =
        "Income Source Classification";

    private static final String RULE_SET_ID =
        "INCOME-SOURCE-CLASSIFICATION";

    private static final String RULE_SET_VERSION =
        "1.0.0";

    private final DMNRuntime dmnRuntime;
    private final DMNModel dmnModel;

    public IncomeClassificationService() {
        KieServices kieServices = KieServices.Factory.get();

        KieContainer kieContainer =
            kieServices.getKieClasspathContainer();

        this.dmnRuntime =
            kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);

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

    public IncomeClassificationResponse classify(
        IncomeClassificationRequest request
    ) {
        String normalizedSourceType =
            request.sourceType().trim().toUpperCase();

        DMNContext context = dmnRuntime.newContext();
        context.set("Source Type", normalizedSourceType);

        DMNResult result =
            dmnRuntime.evaluateAll(dmnModel, context);

        if (result.hasErrors()) {
            throw new IllegalStateException(
                "DMN evaluation failed: "
                    + formatMessages(result.getMessages())
            );
        }

        Object classification =
            result.getContext().get("Income Category");

        if (classification == null) {
            throw new IllegalStateException(
                "DMN returned no Income Category"
            );
        }

        return new IncomeClassificationResponse(
            RULE_SET_ID,
            RULE_SET_VERSION,
            normalizedSourceType,
            classification.toString()
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