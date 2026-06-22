package edu.upc.sistemas.tbcreditflow.origination.repository;

import edu.upc.sistemas.tbcreditflow.origination.domain.Documento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findBySolicitudId(Long solicitudId);
}
