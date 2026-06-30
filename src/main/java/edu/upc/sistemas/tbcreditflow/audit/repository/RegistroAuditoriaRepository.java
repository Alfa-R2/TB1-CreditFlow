package edu.upc.sistemas.tbcreditflow.audit.repository;

import edu.upc.sistemas.tbcreditflow.audit.domain.entity.RegistroAuditoria;
import org.springframework.data.repository.Repository;

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

    List<RegistroAuditoria> findAllByOrderByIdAsc();

    List<RegistroAuditoria> findByFechaGreaterThanEqualOrderByIdAsc(LocalDateTime desde);

    List<RegistroAuditoria> findByFechaLessThanOrderByIdAsc(LocalDateTime hasta);

    List<RegistroAuditoria> findByFechaGreaterThanEqualAndFechaLessThanOrderByIdAsc(LocalDateTime desde,
                                                                                    LocalDateTime hasta);
}
