package edu.upc.sistemas.tbcreditflow.audit.domain.entity;

import edu.upc.sistemas.tbcreditflow.audit.domain.AccionAuditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registro de auditoría <b>append-only</b> (§4.6). Módulo <b>aislado</b> (§2): referencia la
 * solicitud solo por su {@code solicitudId} (Long), sin asociación JPA a origination, para no
 * adquirir dependencias salientes. No tiene setters de negocio: una vez creado no se modifica. El
 * {@code hashIntegridad} encadenado ({@link #sellar(String)}) lo asigna el servicio durante la
 * creación, una sola vez, cuando ya se conoce el {@code id} generado.
 */
@Entity
@Table(name = "registro_auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_id", nullable = false)
    private Long solicitudId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AccionAuditoria accion;

    @Column(nullable = false, length = 60)
    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "hash_integridad", nullable = false, length = 64)
    private String hashIntegridad;

    @Column(name = "hash_previo", nullable = false, length = 64)
    private String hashPrevio;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(Long solicitudId, AccionAuditoria accion, String usuario,
                             LocalDateTime fecha, String hashPrevio) {
        this.solicitudId = solicitudId;
        this.accion = accion;
        this.usuario = usuario;
        this.fecha = fecha;
        this.hashPrevio = hashPrevio;
        this.hashIntegridad = ""; // se sella al conocer el id (ver AuditService)
    }

    /** Sella el registro con su hash de integridad encadenado. Solo durante la creación. */
    public void sellar(String hashIntegridad) {
        this.hashIntegridad = hashIntegridad;
    }

    public Long getId() {
        return id;
    }

    public Long getSolicitudId() {
        return solicitudId;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public String getUsuario() {
        return usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getHashIntegridad() {
        return hashIntegridad;
    }

    public String getHashPrevio() {
        return hashPrevio;
    }
}
