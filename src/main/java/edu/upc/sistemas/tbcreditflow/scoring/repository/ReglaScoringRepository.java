package edu.upc.sistemas.tbcreditflow.scoring.repository;

import edu.upc.sistemas.tbcreditflow.scoring.domain.ReglaScoring;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReglaScoringRepository extends JpaRepository<ReglaScoring, Long> {
    List<ReglaScoring> findByActivaTrue();
}
