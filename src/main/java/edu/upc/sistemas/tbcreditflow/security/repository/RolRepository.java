package edu.upc.sistemas.tbcreditflow.security.repository;

import edu.upc.sistemas.tbcreditflow.security.domain.Rol;
import edu.upc.sistemas.tbcreditflow.security.domain.RolNombre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(RolNombre nombre);
}
