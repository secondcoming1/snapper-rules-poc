package gov.nyc.dss.snapper.rules;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules")
public class RulesHealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "service", "snapper-rules-service",
            "status", "UP",
            "rulesEngine", "NOT_CONFIGURED",
            "timestamp", Instant.now().toString()
        );
    }
}