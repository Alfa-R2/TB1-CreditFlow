package edu.upc.sistemas.tbcreditflow.scoring.controller;

import edu.upc.sistemas.tbcreditflow.scoring.domain.EvaluacionResponse;
import edu.upc.sistemas.tbcreditflow.scoring.service.ScoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes/{solicitudId}/evaluacion")
public class EvaluacionController {

    private final ScoringService scoringService;

    public EvaluacionController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @PostMapping
    public ResponseEntity<EvaluacionResponse> evaluar(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(EvaluacionResponse.from(scoringService.evaluar(solicitudId)));
    }

    @GetMapping
    public ResponseEntity<EvaluacionResponse> obtener(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(EvaluacionResponse.from(scoringService.obtener(solicitudId)));
    }
}
