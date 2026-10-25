package org.example.queryoptimizer.knowledge.Controller;


import org.example.queryoptimizer.knowledge.Ingestion.PlanLogIngestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
public class PlanLogIngestionController {

    private final PlanLogIngestionService service;

    public PlanLogIngestionController(PlanLogIngestionService service) {
        this.service = service;
    }

    /**
     * Example: POST /api/admin/ingest-plan-log?file=plan-log1.jsonl&exclude=Q32,Q40,Q105
     * Put your held-out test query IDs in "exclude" so the RAG never sees them.
     */
    @PostMapping("/ingest-plan-log")
    public PlanLogIngestionService.Result ingest(@RequestParam String file,
                                                 @RequestParam(required = false) String label,
                                                 @RequestParam(defaultValue = "") Set<String> exclude)
            throws IOException {
        return service.ingest(file, label, exclude);
    }
}
