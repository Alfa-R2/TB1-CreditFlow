package edu.upc.sistemas.tbcreditflow.audit.repository;

import edu.upc.sistemas.tbcreditflow.audit.domain.RegistroAuditoria;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio <b>append-only</b> (§4.6): extiende {@link Repository} (no {@code JpaRepository}) para
 * NO exponer operaciones de borrado. Solo se permite insertar y consultar; nunca borrar.
 */
public interface RegistroAuditoriaRepository extends Repository<RegistroAuditoria, Long> {

    RegistroAuditoria save(RegistroAuditoria registro);

    Optional<RegistroAuditoria> findById(Long id);

    List<RegistroAuditoria> findAll();

    long count();

    /** Último registro insertado, usado para encadenar el hash. */
    Optional<RegistroAuditoria> findTopByOrderByIdDesc();

    /**
     * Consulta por rango de fechas (HU10), solo sobre RegistroAuditoria — sin referencias a otros
     * módulos, para mantener {@code audit} aislado. El filtro por cliente se aplica fuera (reporting).
     */
    @Query("""
            SELECT r FROM RegistroAuditoria r
            WHERE (:desde IS NULL OR r.fecha >= :desde)
              AND (:hasta IS NULL OR r.fecha < :hasta)
            ORDER BY r.id
            """)
    List<RegistroAuditoria> buscarPorRango(@Param("desde") LocalDateTime desde,
                                           @Param("hasta") LocalDateTime hasta);
}
