package edu.upc.sistemas.tbcreditflow.origination.domain.entity;

import edu.upc.sistemas.tbcreditflow.origination.domain.EstadoSolicitud;
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

@Entity
@Table(name = "solicitud")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "asesor_id", nullable = false)
    private Long asesorId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "plazo_meses", nullable = false)
    private Integer plazoMeses;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoSolicitud estado;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    protected Solicitud() {
    }

    public Solicitud(Long clienteId, Long asesorId, BigDecimal monto, Integer plazoMeses,
                     LocalDateTime fechaRegistro) {
        this.clienteId = clienteId;
        this.asesorId = asesorId;
        this.monto = monto;
        this.plazoMeses = plazoMeses;
        this.estado = EstadoSolicitud.REGISTRADA;
        this.fechaRegistro = fechaRegistro;
    }

    public void cambiarEstado(EstadoSolicitud nuevoEstado) {
        this.estado = nuevoEstado;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getAsesorId() {
        return asesorId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public Integer getPlazoMeses() {
        return plazoMeses;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
