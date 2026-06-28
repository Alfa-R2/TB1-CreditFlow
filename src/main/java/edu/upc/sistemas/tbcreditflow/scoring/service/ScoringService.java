package edu.upc.sistemas.tbcreditflow.scoring.service;

import edu.upc.sistemas.tbcreditflow.common.exception.BadRequestException;
import edu.upc.sistemas.tbcreditflow.common.exception.ConflictException;
import edu.upc.sistemas.tbcreditflow.common.exception.ResourceNotFoundException;
import edu.upc.sistemas.tbcreditflow.origination.domain.entity.Cliente;
import edu.upc.sistemas.tbcreditflow.origination.domain.EstadoSolicitud;
import edu.upc.sistemas.tbcreditflow.origination.domain.entity.Solicitud;
import edu.upc.sistemas.tbcreditflow.origination.service.SolicitudService;
import edu.upc.sistemas.tbcreditflow.scoring.domain.entity.EvaluacionRiesgo;
import edu.upc.sistemas.tbcreditflow.scoring.domain.entity.ReglaScoring;
import edu.upc.sistemas.tbcreditflow.scoring.domain.dto.ResultadoEvaluacion;
import edu.upc.sistemas.tbcreditflow.scoring.repository.EvaluacionRiesgoRepository;
import edu.upc.sistemas.tbcreditflow.scoring.repository.ReglaScoringRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Evaluación de riesgo (HU05/HU06). Lee Solicitud/Cliente de origination, ejecuta el motor de
 * scoring, persiste la {@link EvaluacionRiesgo} y solicita la transición a EVALUADA.
 */
@Service
public class ScoringService {

    private final SolicitudService solicitudService;
    private final EvaluacionRiesgoRepository evaluacionRepository;
    private final ReglaScoringRepository reglaScoringRepository;
    private final ScoringEngine scoringEngine;

    public ScoringService(SolicitudService solicitudService,
                          EvaluacionRiesgoRepository evaluacionRepository,
                          ReglaScoringRepository reglaScoringRepository,
                          ScoringEngine scoringEngine) {
        this.solicitudService = solicitudService;
        this.evaluacionRepository = evaluacionRepository;
        this.reglaScoringRepository = reglaScoringRepository;
        this.scoringEngine = scoringEngine;
    }

    @Transactional
    public EvaluacionRiesgo evaluar(Long solicitudId) {
        Solicitud solicitud = solicitudService.obtener(solicitudId); // 404 si no existe

        if (solicitud.getEstado() != EstadoSolicitud.REGISTRADA) {
            throw new ConflictException(
                    "La solicitud no está en estado REGISTRADA (estado actual: " + solicitud.getEstado() + ")");
        }

        Cliente cliente = solicitud.getCliente();
        validarDatos(cliente);

        List<ReglaScoring> reglas = reglaScoringRepository.findByActivaTrue();
        ResultadoEvaluacion resultado = scoringEngine.calcular(
                cliente.getIngresoMensual(),
                cliente.getDeudasActuales(),
                solicitud.getMonto(),
                solicitud.getPlazoMeses(),
                reglas);

        EvaluacionRiesgo evaluacion = new EvaluacionRiesgo(
                solicitud,
                resultado.capacidadPago(),
                resultado.score(),
                resultado.nivelRiesgo(),
                resultado.justificacion(),
                LocalDateTime.now());
        evaluacion = evaluacionRepository.save(evaluacion);

        solicitudService.marcarEvaluada(solicitud);
        return evaluacion;
    }

    @Transactional(readOnly = true)
    public EvaluacionRiesgo obtener(Long solicitudId) {
        return evaluacionRepository.findBySolicitud_Id(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe evaluación para la solicitud: " + solicitudId));
    }

    // §4.2: ingreso 0 o datos faltantes ⇒ 400 (evita división por cero)
    private void validarDatos(Cliente cliente) {
        if (cliente.getIngresoMensual() == null || cliente.getDeudasActuales() == null) {
            throw new BadRequestException("Faltan datos obligatorios del cliente (ingreso/deudas)");
        }
        if (cliente.getIngresoMensual().compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("El ingreso mensual es 0; no se puede calcular la evaluación");
        }
    }
}
