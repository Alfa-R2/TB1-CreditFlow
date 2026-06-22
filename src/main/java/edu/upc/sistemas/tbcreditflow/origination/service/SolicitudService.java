package edu.upc.sistemas.tbcreditflow.origination.service;

import edu.upc.sistemas.tbcreditflow.audit.domain.AccionAuditoria;
import edu.upc.sistemas.tbcreditflow.audit.service.AuditService;
import edu.upc.sistemas.tbcreditflow.common.ConflictException;
import edu.upc.sistemas.tbcreditflow.common.ResourceNotFoundException;
import edu.upc.sistemas.tbcreditflow.origination.domain.AccionDecision;
import edu.upc.sistemas.tbcreditflow.origination.domain.Cliente;
import edu.upc.sistemas.tbcreditflow.origination.domain.ClienteRequest;
import edu.upc.sistemas.tbcreditflow.origination.domain.CrearSolicitudRequest;
import edu.upc.sistemas.tbcreditflow.origination.domain.EstadoSolicitud;
import edu.upc.sistemas.tbcreditflow.origination.domain.Solicitud;
import edu.upc.sistemas.tbcreditflow.origination.domain.SolicitudResponse;
import edu.upc.sistemas.tbcreditflow.origination.repository.ClienteRepository;
import edu.upc.sistemas.tbcreditflow.origination.repository.SolicitudRepository;
import edu.upc.sistemas.tbcreditflow.security.service.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Solicitudes y ciclo de vida de la Solicitud (HU01, HU03, HU08). Es el único módulo que escribe
 * la entidad {@link Solicitud}; scoring solicita la transición a EVALUADA vía {@link #marcarEvaluada}.
 */
@Service
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioService usuarioService;
    private final AuditService auditService;

    public SolicitudService(SolicitudRepository solicitudRepository,
                            ClienteRepository clienteRepository,
                            UsuarioService usuarioService,
                            AuditService auditService) {
        this.solicitudRepository = solicitudRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioService = usuarioService;
        this.auditService = auditService;
    }

    /** HU01/HU03: crea la solicitud en estado REGISTRADA, creando o reutilizando el Cliente. */
    @Transactional
    public SolicitudResponse crear(CrearSolicitudRequest request) {
        Cliente cliente = obtenerOReutilizarCliente(request.cliente());
        Long asesorId = usuarioService.currentUsuario().getId();
        Solicitud solicitud = new Solicitud(
                cliente.getId(), asesorId, request.monto(), request.plazoMeses(), LocalDateTime.now());
        return SolicitudResponse.from(solicitudRepository.save(solicitud));
    }

    private Cliente obtenerOReutilizarCliente(ClienteRequest c) {
        return clienteRepository.findByTipoDocAndNumDoc(c.tipoDoc(), c.numDoc())
                .orElseGet(() -> clienteRepository.save(new Cliente(
                        c.tipoDoc(), c.numDoc(), c.nombres(), c.apellidos(),
                        c.ingresoMensual(), c.deudasActuales())));
    }

    @Transactional(readOnly = true)
    public Solicitud obtener(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public SolicitudResponse obtenerResponse(Long id) {
        return SolicitudResponse.from(obtener(id));
    }

    /** HU04: estado actual de la solicitud (404 si no existe). */
    @Transactional(readOnly = true)
    public EstadoSolicitud consultarEstado(Long id) {
        return obtener(id).getEstado();
    }

    /** Cliente asociado a la solicitud (lectura usada por el módulo de scoring). */
    @Transactional(readOnly = true)
    public Cliente obtenerCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + clienteId));
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponse> listar(EstadoSolicitud estado, Long clienteId) {
        return solicitudRepository.buscar(estado, clienteId).stream()
                .map(SolicitudResponse::from)
                .toList();
    }

    /** Transición REGISTRADA → EVALUADA solicitada por scoring tras persistir la evaluación. */
    @Transactional
    public void marcarEvaluada(Solicitud solicitud) {
        solicitud.cambiarEstado(EstadoSolicitud.EVALUADA);
        solicitudRepository.save(solicitud);
    }

    /**
     * HU08: el comité aprueba/rechaza. Solo válido desde EVALUADA (si no ⇒ 409). Pasar a estado
     * terminal genera un RegistroAuditoria (HU09) con el usuario que decide.
     */
    @Transactional
    public EstadoSolicitud decidir(Long id, AccionDecision accion) {
        Solicitud solicitud = obtener(id);
        if (solicitud.getEstado() != EstadoSolicitud.EVALUADA) {
            throw new ConflictException(
                    "La solicitud no está en estado EVALUADA (estado actual: " + solicitud.getEstado() + ")");
        }

        EstadoSolicitud nuevoEstado;
        AccionAuditoria accionAuditoria;
        if (accion == AccionDecision.APROBAR) {
            nuevoEstado = EstadoSolicitud.APROBADA;
            accionAuditoria = AccionAuditoria.APROBADA;
        } else {
            nuevoEstado = EstadoSolicitud.RECHAZADA;
            accionAuditoria = AccionAuditoria.RECHAZADA;
        }

        solicitud.cambiarEstado(nuevoEstado);
        solicitudRepository.save(solicitud);

        auditService.registrar(solicitud.getId(), accionAuditoria, usuarioService.currentUsername());
        return nuevoEstado;
    }
}
