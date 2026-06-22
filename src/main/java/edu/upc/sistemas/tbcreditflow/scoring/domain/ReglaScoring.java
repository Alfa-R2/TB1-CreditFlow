package edu.upc.sistemas.tbcreditflow.scoring.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Regla de scoring parametrizable (§3, §4.4). {@code parametro} es {@code ratioEndeudamiento} o
 * {@code ratioCuota}; {@code ponderacion} lleva signo (penaliza/bonifica el score).
 */
@Entity
@Table(name = "regla_scoring")
public class ReglaScoring {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String parametro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private OperadorRegla operador;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal umbral;

    @Column(nullable = false)
    private Integer ponderacion;

    @Column(nullable = false)
    private Boolean activa;

    protected ReglaScoring() {
    }

    public ReglaScoring(String nombre, String parametro, OperadorRegla operador, BigDecimal umbral,
                        Integer ponderacion, Boolean activa) {
        this.nombre = nombre;
        this.parametro = parametro;
        this.operador = operador;
        this.umbral = umbral;
        this.ponderacion = ponderacion;
        this.activa = activa;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getParametro() {
        return parametro;
    }

    public OperadorRegla getOperador() {
        return operador;
    }

    public BigDecimal getUmbral() {
        return umbral;
    }

    public Integer getPonderacion() {
        return ponderacion;
    }

    public Boolean getActiva() {
        return activa;
    }
}
