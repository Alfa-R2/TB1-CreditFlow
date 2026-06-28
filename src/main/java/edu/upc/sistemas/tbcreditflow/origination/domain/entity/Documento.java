package edu.upc.sistemas.tbcreditflow.origination.domain.entity;

import edu.upc.sistemas.tbcreditflow.origination.domain.TipoDocumento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "documento")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_documento_solicitud"))
    private Solicitud solicitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDocumento tipo;

    @Column(name = "url_archivo", nullable = false, length = 500)
    private String urlArchivo;

    @Column(nullable = false, length = 64)
    private String hash;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    protected Documento() {
    }

    public Documento(Solicitud solicitud, TipoDocumento tipo, String urlArchivo, String hash,
                     LocalDateTime fechaCarga) {
        this.solicitud = solicitud;
        this.tipo = tipo;
        this.urlArchivo = urlArchivo;
        this.hash = hash;
        this.fechaCarga = fechaCarga;
    }

    public Long getId() {
        return id;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public String getUrlArchivo() {
        return urlArchivo;
    }

    public String getHash() {
        return hash;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }
}
