package gov.nyc.dss.snapper.rules;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules/income-records")
public class IncomeRecordController {

    private final IncomeRecordApplicabilityService service;

    public IncomeRecordController(
        IncomeRecordApplicabilityService service
    ) {
        this.service = service;
    }

    @PostMapping("/classify")
    public ResponseEntity<IncomeRecordResponse> classify(
        @Valid @RequestBody IncomeRecordRequest request
    ) {
        return ResponseEntity.ok(
            service.classify(request)
        );
    }
}