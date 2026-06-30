package edu.upc.sistemas.tbcreditflow.common;

import edu.upc.sistemas.tbcreditflow.common.exception.BadRequestException;
import edu.upc.sistemas.tbcreditflow.common.exception.ConflictException;
import edu.upc.sistemas.tbcreditflow.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = new MockHttpServletRequest("GET", "/api/prueba");

    @Test
    void validationErrors_joinFieldMessagesAndFallbackForNullDefaultMessage() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new DummyRequest(), "request");
        bindingResult.addError(new FieldError("request", "nombre", "obligatorio"));
        bindingResult.addError(new FieldError("request", "monto", null));

        ResponseEntity<ApiError> response = handler.handleValidation(validationException(bindingResult), request);

        assertError(response, HttpStatus.BAD_REQUEST, "nombre: obligatorio; monto: inválido");
    }

    @Test
    void validationWithoutFieldErrors_usesGenericMessage() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new DummyRequest(), "request");

        ResponseEntity<ApiError> response = handler.handleValidation(validationException(bindingResult), request);

        assertError(response, HttpStatus.BAD_REQUEST, "Datos de entrada inválidos");
    }

    @Test
    void simpleHandlers_returnExpectedApiError() throws Exception {
        assertError(
                handler.handleConstraintViolation(new ConstraintViolationException("clienteId: inválido", Set.of()), request),
                HttpStatus.BAD_REQUEST,
                "clienteId: inválido");
        assertError(
                handler.handleUnreadable(null, request),
                HttpStatus.BAD_REQUEST,
                "Cuerpo de la petición inválido o ilegible");
        assertError(
                handler.handleTypeMismatch(typeMismatchException("clienteId"), request),
                HttpStatus.BAD_REQUEST,
                "Parámetro 'clienteId' con valor inválido");
        assertError(
                handler.handleBadRequest(new BadRequestException("Monto inválido"), request),
                HttpStatus.BAD_REQUEST,
                "Monto inválido");
        assertError(
                handler.handleMaxUpload(null, request),
                HttpStatus.BAD_REQUEST,
                "El archivo excede el tamaño máximo permitido (5 MB)");
        assertError(
                handler.handleBadCredentials(new BadCredentialsException("bad"), request),
                HttpStatus.UNAUTHORIZED,
                "Credenciales inválidas");
        assertError(
                handler.handleAccessDenied(new AccessDeniedException("denied"), request),
                HttpStatus.FORBIDDEN,
                "Acceso denegado: rol no autorizado");
        assertError(
                handler.handleNotFound(new ResourceNotFoundException("Solicitud no encontrada"), request),
                HttpStatus.NOT_FOUND,
                "Solicitud no encontrada");
        assertError(
                handler.handleConflict(new ConflictException("Estado inválido"), request),
                HttpStatus.CONFLICT,
                "Estado inválido");
        assertError(
                handler.handleGeneric(new RuntimeException("boom"), request),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor");
    }

    private MethodArgumentNotValidException validationException(BeanPropertyBindingResult bindingResult)
            throws NoSuchMethodException {
        Method method = DummyController.class.getDeclaredMethod("crear", DummyRequest.class);
        return new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
    }

    private MethodArgumentTypeMismatchException typeMismatchException(String parameterName)
            throws NoSuchMethodException {
        Method method = DummyController.class.getDeclaredMethod("buscar", Long.class);
        return new MethodArgumentTypeMismatchException(
                "abc",
                Long.class,
                parameterName,
                new MethodParameter(method, 0),
                new NumberFormatException("For input string: abc"));
    }

    private void assertError(ResponseEntity<ApiError> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(status.value());
        assertThat(response.getBody().error()).isEqualTo(status.getReasonPhrase());
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().path()).isEqualTo("/api/prueba");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    private record DummyRequest() {
    }

    private static class DummyController {
        void crear(DummyRequest request) {
        }

        void buscar(Long id) {
        }
    }
}
