package edu.upc.sistemas.tbcreditflow.reporting.service;

import edu.upc.sistemas.tbcreditflow.audit.domain.entity.RegistroAuditoria;
import edu.upc.sistemas.tbcreditflow.audit.repository.RegistroAuditoriaRepository;
import edu.upc.sistemas.tbcreditflow.origination.domain.EstadoSolicitud;
import edu.upc.sistemas.tbcreditflow.origination.domain.entity.Solicitud;
import edu.upc.sistemas.tbcreditflow.origination.repository.SolicitudRepository;
import edu.upc.sistemas.tbcreditflow.reporting.domain.ReporteIndicadores;
import edu.upc.sistemas.tbcreditflow.scoring.domain.NivelRiesgo;
import edu.upc.sistemas.tbcreditflow.scoring.repository.EvaluacionRiesgoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HU13 — indicadores agregados. Vive en {@code reporting} (lectura cross-módulo, §2). Fórmulas según
 * la especificación de Fase 2 (§2 de ese documento).
 */
@Service
public class ReporteService {

    private static final BigDecimal CERO_2 = new BigDecimal("0.00");
    private static final double MINUTOS_POR_DIA = 1440.0;
    private static final ZoneId REPORT_TIME_ZONE = ZoneId.systemDefault();

    private final SolicitudRepository solicitudRepository;
    private final EvaluacionRiesgoRepository evaluacionRepository;
    private final RegistroAuditoriaRepository registroAuditoriaRepository;

    public ReporteService(SolicitudRepository solicitudRepository,
                          EvaluacionRiesgoRepository evaluacionRepository,
                          RegistroAuditoriaRepository registroAuditoriaRepository) {
        this.solicitudRepository = solicitudRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.registroAuditoriaRepository = registroAuditoriaRepository;
    }

    @Transactional(readOnly = true)
    public ReporteIndicadores indicadores() {
        long total = solicitudRepository.count();
        long aprobadas = solicitudRepository.countByEstado(EstadoSolicitud.APROBADA);
        long rechazadas = solicitudRepository.countByEstado(EstadoSolicitud.RECHAZADA);

        BigDecimal porcentajeAprobacion = total == 0
                ? CERO_2
                : BigDecimal.valueOf(aprobadas)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        Map<String, Long> distribucionRiesgo = new LinkedHashMap<>();
        distribucionRiesgo.put("BAJO", evaluacionRepository.countByNivelRiesgo(NivelRiesgo.BAJO));
        distribucionRiesgo.put("MEDIO", evaluacionRepository.countByNivelRiesgo(NivelRiesgo.MEDIO));
        distribucionRiesgo.put("ALTO", evaluacionRepository.countByNivelRiesgo(NivelRiesgo.ALTO));

        return new ReporteIndicadores(total, aprobadas, rechazadas,
                porcentajeAprobacion, tiempoPromedioDias(), distribucionRiesgo);
    }

    // Promedio de días entre el registro de la solicitud y la fecha de su decisión (auditoría).
    private BigDecimal tiempoPromedioDias() {
        List<RegistroAuditoria> decisiones = registroAuditoriaRepository.findAll();
        if (decisiones.isEmpty()) {
            return CERO_2;
        }
        Map<Long, LocalDateTime> fechaRegistroPorSolicitud = solicitudRepository.findAll().stream()
                .collect(Collectors.toMap(Solicitud::getId, Solicitud::getFechaRegistro));

        double sumaDias = 0.0;
        long n = 0;
        for (RegistroAuditoria decision : decisiones) {
            LocalDateTime registro = fechaRegistroPorSolicitud.get(decision.getSolicitudId());
            if (registro == null) {
                continue;
            }
            sumaDias += Duration.between(
                    registro.atZone(REPORT_TIME_ZONE).toInstant(),
                    decision.getFecha().atZone(REPORT_TIME_ZONE).toInstant()
            ).toMinutes() / MINUTOS_POR_DIA;
            n++;
        }
        if (n == 0) {
            return CERO_2;
        }
        return BigDecimal.valueOf(sumaDias / n).setScale(2, RoundingMode.HALF_UP);
    }
}
