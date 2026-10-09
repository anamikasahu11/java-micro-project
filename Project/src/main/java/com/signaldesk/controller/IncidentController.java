package com.signaldesk.controller;

import com.signaldesk.model.AnalyzeRequest;
import com.signaldesk.model.Incident;
import com.signaldesk.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@CrossOrigin(origins = "*")
public class IncidentController {
    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public List<Incident> getIncidents() {
        return incidentService.findAll();
    }

    @PostMapping("/analyze")
    public ResponseEntity<Incident> analyze(@Valid @RequestBody AnalyzeRequest request) {
        return ResponseEntity.ok(incidentService.analyze(request));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<Incident> resolve(@PathVariable long id) {
        return ResponseEntity.ok(incidentService.resolve(id));
    }
}
