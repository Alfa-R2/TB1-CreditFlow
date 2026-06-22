package edu.upc.sistemas.tbcreditflow.scoring.repository;

import edu.upc.sistemas.tbcreditflow.scoring.domain.EvaluacionRiesgo;
import edu.upc.sistemas.tbcreditflow.scoring.domain.NivelRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EvaluacionRiesgoRepository extends JpaRepository<EvaluacionRiesgo, Long> {
    Optional<EvaluacionRiesgo> findBySolicitudId(Long solicitudId);

    boolean existsBySolicitudId(Long solicitudId);

    long countByNivelRiesgo(NivelRiesgo nivelRiesgo);
}
