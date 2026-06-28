package edu.upc.sistemas.tbcreditflow.scoring.domain.entity;

import edu.upc.sistemas.tbcreditflow.scoring.domain.NivelRiesgo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resultado de la evaluación de riesgo de una solicitud (§3). {@code solicitudId} es único: una sola
 * evaluación vigente por solicitud.
 */
@Entity
@Table(name = "evaluacion_riesgo")
public class EvaluacionRiesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_id", nullable = false, unique = true)
    private Long solicitudId;

    @Column(name = "capacidad_pago", nullable = false, precision = 19, scale = 2)
    private BigDecimal capacidadPago;

    @Column(nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo", nullable = false, length = 5)
    private NivelRiesgo nivelRiesgo;

    @Column(nullable = false, length = 2000)
    private String justificacion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected EvaluacionRiesgo() {
    }

    public EvaluacionRiesgo(Long solicitudId, BigDecimal capacidadPago, Integer score,
                            NivelRiesgo nivelRiesgo, String justificacion, LocalDateTime fecha) {
        this.solicitudId = solicitudId;
        this.capacidadPago = capacidadPago;
        this.score = score;
        this.nivelRiesgo = nivelRiesgo;
        this.justificacion = justificacion;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public Long getSolicitudId() {
        return solicitudId;
    }

    public BigDecimal getCapacidadPago() {
        return capacidadPago;
    }

    public Integer getScore() {
        return score;
    }

    public NivelRiesgo getNivelRiesgo() {
        return nivelRiesgo;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
