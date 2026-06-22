package edu.upc.sistemas.tbcreditflow.reporting.controller;

import edu.upc.sistemas.tbcreditflow.reporting.domain.ReporteIndicadores;
import edu.upc.sistemas.tbcreditflow.reporting.service.ReporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU13 — {@code GET /api/reportes/indicadores}. Acceso: GERENTE (§6). */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/indicadores")
    public ResponseEntity<ReporteIndicadores> indicadores() {
        return ResponseEntity.ok(reporteService.indicadores());
    }
}
