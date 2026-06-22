package edu.upc.sistemas.tbcreditflow.origination.controller;

import edu.upc.sistemas.tbcreditflow.origination.domain.CrearSolicitudRequest;
import edu.upc.sistemas.tbcreditflow.origination.domain.DecisionRequest;
import edu.upc.sistemas.tbcreditflow.origination.domain.DocumentoResponse;
import edu.upc.sistemas.tbcreditflow.origination.domain.EstadoResponse;
import edu.upc.sistemas.tbcreditflow.origination.domain.EstadoSolicitud;
import edu.upc.sistemas.tbcreditflow.origination.domain.SolicitudResponse;
import edu.upc.sistemas.tbcreditflow.origination.domain.TipoDocumento;
import edu.upc.sistemas.tbcreditflow.origination.service.DocumentoService;
import edu.upc.sistemas.tbcreditflow.origination.service.SolicitudService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final DocumentoService documentoService;

    public SolicitudController(SolicitudService solicitudService, DocumentoService documentoService) {
        this.solicitudService = solicitudService;
        this.documentoService = documentoService;
    }

    @PostMapping
    public ResponseEntity<SolicitudResponse> crear(@Valid @RequestBody CrearSolicitudRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.crear(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.obtenerResponse(id));
    }

    @GetMapping
    public ResponseEntity<List<SolicitudResponse>> listar(
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(solicitudService.listar(estado, clienteId));
    }

    @PostMapping("/{id}/documentos")
    public ResponseEntity<DocumentoResponse> subirDocumento(
            @PathVariable Long id,
            @RequestParam("tipo") TipoDocumento tipo,
            @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentoService.subir(id, tipo, archivo));
    }

    @PostMapping("/{id}/decision")
    public ResponseEntity<EstadoResponse> decidir(
            @PathVariable Long id,
            @Valid @RequestBody DecisionRequest request) {
        EstadoSolicitud estado = solicitudService.decidir(id, request.accion());
        return ResponseEntity.ok(new EstadoResponse(estado));
    }
}
