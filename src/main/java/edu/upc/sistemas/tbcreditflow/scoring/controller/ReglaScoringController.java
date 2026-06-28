package edu.upc.sistemas.tbcreditflow.scoring.controller;

import edu.upc.sistemas.tbcreditflow.scoring.domain.dto.ReglaScoringRequest;
import edu.upc.sistemas.tbcreditflow.scoring.domain.dto.ReglaScoringResponse;
import edu.upc.sistemas.tbcreditflow.scoring.service.ReglaScoringService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reglas")
public class ReglaScoringController {

    private final ReglaScoringService reglaScoringService;

    public ReglaScoringController(ReglaScoringService reglaScoringService) {
        this.reglaScoringService = reglaScoringService;
    }

    @GetMapping
    public ResponseEntity<List<ReglaScoringResponse>> listar() {
        return ResponseEntity.ok(reglaScoringService.listar());
    }

    @PostMapping
    public ResponseEntity<ReglaScoringResponse> crear(@Valid @RequestBody ReglaScoringRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reglaScoringService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReglaScoringResponse> actualizar(@PathVariable Long id,
                                                           @Valid @RequestBody ReglaScoringRequest request) {
        return ResponseEntity.ok(reglaScoringService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        reglaScoringService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
