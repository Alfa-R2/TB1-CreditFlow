package edu.upc.sistemas.tbcreditflow.origination.repository;

import edu.upc.sistemas.tbcreditflow.origination.domain.Cliente;
import edu.upc.sistemas.tbcreditflow.origination.domain.TipoDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByTipoDocAndNumDoc(TipoDoc tipoDoc, String numDoc);
}
