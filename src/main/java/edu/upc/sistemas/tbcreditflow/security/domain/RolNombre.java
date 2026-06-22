package edu.upc.sistemas.tbcreditflow.security.domain;

/**
 * Roles del sistema (§3). El authority en el JWT es {@code ROLE_<RolNombre>}.
 */
public enum RolNombre {
    ASESOR,
    ANALISTA,
    ADMIN_CREDITO,
    COMITE,
    CUMPLIMIENTO,
    AUDITOR,
    GERENTE
}
