package edu.upc.sistemas.tbcreditflow.scoring.domain;

import java.math.BigDecimal;

public record ReglaScoringResponse(
        Long id,
        String nombre,
        String parametro,
        OperadorRegla operador,
        BigDecimal umbral,
        Integer ponderacion,
        Boolean activa
) {
    public static ReglaScoringResponse from(ReglaScoring r) {
        return new ReglaScoringResponse(
                r.getId(),
                r.getNombre(),
                r.getParametro(),
                r.getOperador(),
                r.getUmbral(),
                r.getPonderacion(),
                r.getActiva()
        );
    }
}
