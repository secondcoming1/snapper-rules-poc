package gov.nyc.dss.snapper.rules;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules/income")
public class IncomeClassificationController {

    private final IncomeClassificationService service;

    public IncomeClassificationController(
        IncomeClassificationService service
    ) {
        this.service = service;
    }

    @PostMapping("/classify")
    public ResponseEntity<IncomeClassificationResponse> classify(
        @Valid @RequestBody IncomeClassificationRequest request
    ) {
        return ResponseEntity.ok(
            service.classify(request)
        );
    }
}