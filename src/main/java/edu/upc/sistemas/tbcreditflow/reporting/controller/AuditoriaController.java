package edu.upc.sistemas.tbcreditflow.reporting.controller;

import edu.upc.sistemas.tbcreditflow.audit.domain.dto.RegistroAuditoriaResponse;
import edu.upc.sistemas.tbcreditflow.reporting.service.AuditoriaConsultaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** HU10 — {@code GET /api/auditoria}. Acceso: AUDITOR, CUMPLIMIENTO (§6). */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaConsultaService auditoriaConsultaService;

    public AuditoriaController(AuditoriaConsultaService auditoriaConsultaService) {
        this.auditoriaConsultaService = auditoriaConsultaService;
    }

    @GetMapping
    public ResponseEntity<List<RegistroAuditoriaResponse>> consultar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(auditoriaConsultaService.consultar(clienteId, desde, hasta));
    }
}
