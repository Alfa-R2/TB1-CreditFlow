package edu.upc.sistemas.tbcreditflow.reporting.service;

import edu.upc.sistemas.tbcreditflow.audit.domain.RegistroAuditoria;
import edu.upc.sistemas.tbcreditflow.audit.domain.RegistroAuditoriaResponse;
import edu.upc.sistemas.tbcreditflow.audit.repository.RegistroAuditoriaRepository;
import edu.upc.sistemas.tbcreditflow.origination.domain.Solicitud;
import edu.upc.sistemas.tbcreditflow.origination.repository.SolicitudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HU10 — consulta del historial de auditoría con filtros opcionales por cliente y rango de fechas.
 * Vive en {@code reporting} (lectura cross-módulo permitida por §2) para que {@code audit} siga
 * aislado: el rango de fechas lo resuelve el repositorio de auditoría y el filtro por cliente se
 * aplica resolviendo las solicitudes del cliente en origination.
 */
@Service
public class AuditoriaConsultaService {

    private final RegistroAuditoriaRepository registroAuditoriaRepository;
    private final SolicitudRepository solicitudRepository;

    public AuditoriaConsultaService(RegistroAuditoriaRepository registroAuditoriaRepository,
                                    SolicitudRepository solicitudRepository) {
        this.registroAuditoriaRepository = registroAuditoriaRepository;
        this.solicitudRepository = solicitudRepository;
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoriaResponse> consultar(Long clienteId, LocalDate desde, LocalDate hasta) {
        LocalDateTime desdeInicio = desde == null ? null : desde.atStartOfDay();
        LocalDateTime hastaExclusivo = hasta == null ? null : hasta.plusDays(1).atStartOfDay();

        List<RegistroAuditoria> registros = registroAuditoriaRepository.buscarPorRango(desdeInicio, hastaExclusivo);

        if (clienteId != null) {
            Set<Long> solicitudIds = solicitudRepository.buscar(null, clienteId).stream()
                    .map(Solicitud::getId)
                    .collect(Collectors.toSet());
            registros = registros.stream()
                    .filter(r -> solicitudIds.contains(r.getSolicitudId()))
                    .toList();
        }

        return registros.stream()
                .map(RegistroAuditoriaResponse::from)
                .toList();
    }
}
