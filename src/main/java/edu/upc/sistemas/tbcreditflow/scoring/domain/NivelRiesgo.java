package edu.upc.sistemas.tbcreditflow.scoring.domain;

/** Nivel de riesgo derivado del score (§4.3): ≥70 BAJO · 40–69 MEDIO · &lt;40 ALTO. */
public enum NivelRiesgo {
    BAJO,
    MEDIO,
    ALTO
}
