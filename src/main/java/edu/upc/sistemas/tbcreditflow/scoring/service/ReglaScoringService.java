package edu.upc.sistemas.tbcreditflow.scoring.service;

import edu.upc.sistemas.tbcreditflow.common.BadRequestException;
import edu.upc.sistemas.tbcreditflow.common.ResourceNotFoundException;
import edu.upc.sistemas.tbcreditflow.scoring.domain.ReglaScoring;
import edu.upc.sistemas.tbcreditflow.scoring.domain.ReglaScoringRequest;
import edu.upc.sistemas.tbcreditflow.scoring.domain.ReglaScoringResponse;
import edu.upc.sistemas.tbcreditflow.scoring.repository.ReglaScoringRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * CRUD de parametrización de reglas de scoring (HU07). Solo ADMIN_CREDITO (§6).
 * El {@code parametro} debe ser uno que el motor sepa resolver.
 */
@Service
public class ReglaScoringService {

    private static final Set<String> PARAMETROS_VALIDOS =
            Set.of(ScoringEngine.RATIO_ENDEUDAMIENTO, ScoringEngine.RATIO_CUOTA);

    private final ReglaScoringRepository reglaScoringRepository;

    public ReglaScoringService(ReglaScoringRepository reglaScoringRepository) {
        this.reglaScoringRepository = reglaScoringRepository;
    }

    @Transactional(readOnly = true)
    public List<ReglaScoringResponse> listar() {
        return reglaScoringRepository.findAll().stream()
                .map(ReglaScoringResponse::from)
                .toList();
    }

    @Transactional
    public ReglaScoringResponse crear(ReglaScoringRequest request) {
        validarParametro(request.parametro());
        ReglaScoring regla = new ReglaScoring(
                request.nombre(), request.parametro(), request.operador(),
                request.umbral(), request.ponderacion(), request.activa());
        return ReglaScoringResponse.from(reglaScoringRepository.save(regla));
    }

    @Transactional
    public ReglaScoringResponse actualizar(Long id, ReglaScoringRequest request) {
        validarParametro(request.parametro());
        ReglaScoring regla = reglaScoringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regla no encontrada: " + id));
        regla.actualizar(request.nombre(), request.parametro(), request.operador(),
                request.umbral(), request.ponderacion(), request.activa());
        return ReglaScoringResponse.from(reglaScoringRepository.save(regla));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!reglaScoringRepository.existsById(id)) {
            throw new ResourceNotFoundException("Regla no encontrada: " + id);
        }
        reglaScoringRepository.deleteById(id);
    }

    private void validarParametro(String parametro) {
        if (!PARAMETROS_VALIDOS.contains(parametro)) {
            throw new BadRequestException(
                    "parametro inválido: debe ser 'ratioEndeudamiento' o 'ratioCuota'");
        }
    }
}
