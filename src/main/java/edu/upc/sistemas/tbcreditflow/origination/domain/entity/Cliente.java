package edu.upc.sistemas.tbcreditflow.origination.domain.entity;

import edu.upc.sistemas.tbcreditflow.origination.domain.TipoDoc;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * Cliente solicitante. Restricción única {@code (tipoDoc, numDoc)} (§3).
 */
@Entity
@Table(name = "cliente",
        uniqueConstraints = @UniqueConstraint(name = "uk_cliente_tipo_num_doc",
                columnNames = {"tipo_doc", "num_doc"}))
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_doc", nullable = false, length = 5)
    private TipoDoc tipoDoc;

    @Column(name = "num_doc", nullable = false, length = 20)
    private String numDoc;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(name = "ingreso_mensual", nullable = false, precision = 19, scale = 2)
    private BigDecimal ingresoMensual;

    @Column(name = "deudas_actuales", nullable = false, precision = 19, scale = 2)
    private BigDecimal deudasActuales;

    protected Cliente() {
    }

    public Cliente(TipoDoc tipoDoc, String numDoc, String nombres, String apellidos,
                   BigDecimal ingresoMensual, BigDecimal deudasActuales) {
        this.tipoDoc = tipoDoc;
        this.numDoc = numDoc;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.ingresoMensual = ingresoMensual;
        this.deudasActuales = deudasActuales;
    }

    public Long getId() {
        return id;
    }

    public TipoDoc getTipoDoc() {
        return tipoDoc;
    }

    public String getNumDoc() {
        return numDoc;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public BigDecimal getIngresoMensual() {
        return ingresoMensual;
    }

    public BigDecimal getDeudasActuales() {
        return deudasActuales;
    }
}
