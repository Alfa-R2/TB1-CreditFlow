package edu.upc.sistemas.tbcreditflow.origination.repository;

import edu.upc.sistemas.tbcreditflow.origination.domain.EstadoSolicitud;
import edu.upc.sistemas.tbcreditflow.origination.domain.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    /** Búsqueda con filtros opcionales por estado y/o cliente (§5: GET /api/solicitudes). */
    @Query("""
            SELECT s FROM Solicitud s
            WHERE (:estado IS NULL OR s.estado = :estado)
              AND (:clienteId IS NULL OR s.cliente.id = :clienteId)
            ORDER BY s.id
            """)
    List<Solicitud> buscar(@Param("estado") EstadoSolicitud estado,
                           @Param("clienteId") Long clienteId);

    long countByEstado(EstadoSolicitud estado);
}
