package br.edu.ifsp.tcc.application.config;

import br.edu.ifsp.tcc.application.dto.MessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleDataIntegrity_shouldReturn409WithoutLeakingSql() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "Duplicate entry 'x' for key 'uk_tags_team_name' [insert into tags ...]");

        ResponseEntity<MessageResponse> response = handler.handleDataIntegrity(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Registro duplicado ou em conflito com dados existentes.", response.getBody().getMessage());
    }

    @Test
    void handleAccessDenied_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<MessageResponse> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("negado"));
    }

    @Test
    void handleAuthentication_shouldReturn401() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<MessageResponse> response = handler.handleAuthentication(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("autenticado"));
    }

    @Test
    void handleValidationException_shouldReturn400WithFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new FieldError("dto", "email", "must not be blank"));
        bindingResult.addError(new FieldError("dto", "name", "must not be blank"));

        MethodParameter param = new MethodParameter(
                this.getClass().getDeclaredMethod("handleValidationException_shouldReturn400WithFieldErrors"), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<MessageResponse> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("must not be blank"));
    }

    private static void assertBody(ResponseEntity<MessageResponse> response, HttpStatus status, String message) {
        assertEquals(status, response.getStatusCode());
        assertEquals(message, response.getBody().getMessage());
    }

    @Test
    void handleUnreadableBody_shouldReturn400WithoutParserText() {
        HttpInputMessage input = mock(HttpInputMessage.class);
        var ex = new HttpMessageNotReadableException("JSON parse error: Unexpected character at tools.jackson", input);

        assertBody(handler.handleUnreadableBody(ex), HttpStatus.BAD_REQUEST, "Corpo da requisição inválido.");
    }

    @Test
    void handleTypeMismatch_shouldNameTheParameter() {
        var ex = new MethodArgumentTypeMismatchException("abc", Long.class, "tagId", null, new NumberFormatException("x"));

        assertBody(handler.handleTypeMismatch(ex), HttpStatus.BAD_REQUEST, "Parâmetro inválido: tagId.");
    }

    @Test
    void handleDateParse_shouldReturn400() {
        var ex = new DateTimeParseException("Text could not be parsed", "2026-02-30", 0);

        assertBody(handler.handleDateParse(ex), HttpStatus.BAD_REQUEST, "Parâmetro inválido: data.");
    }

    @Test
    void handleMissingParameter_shouldReturn400() {
        var ex = new MissingServletRequestParameterException("q", "String");

        assertBody(handler.handleMissingParameter(ex), HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: q.");
    }

    @Test
    void handleNotFound_shouldReturn404ForUnknownRoutes() {
        assertBody(handler.handleNotFound(new NoResourceFoundException(HttpMethod.GET, "/api/v1/nope", "/api/v1/nope")),
                HttpStatus.NOT_FOUND, "Rota não encontrada.");
        assertBody(handler.handleNotFound(new NoHandlerFoundException("GET", "/x", new HttpHeaders())),
                HttpStatus.NOT_FOUND, "Rota não encontrada.");
    }

    @Test
    void handleMethodNotSupported_shouldReturn405() {
        var ex = new HttpRequestMethodNotSupportedException("PATCH");

        assertBody(handler.handleMethodNotSupported(ex), HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido.");
    }

    @Test
    void handleMediaTypeNotSupported_shouldReturn415() {
        var ex = new HttpMediaTypeNotSupportedException("text/plain");

        assertBody(handler.handleMediaTypeNotSupported(ex), HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Tipo de conteúdo não suportado.");
    }

    @Test
    void handleUnexpected_shouldReturn500WithoutExposingTheMessage() {
        ResponseEntity<MessageResponse> response = handler.handleUnexpected(new IllegalStateException("secret internals"));

        assertBody(response, HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente mais tarde.");
    }

    @Test
    void handleUnexpected_shouldKeepTheStatusOfOtherSpringClientErrors() {
        ResponseEntity<MessageResponse> response =
                handler.handleUnexpected(new HttpMediaTypeNotAcceptableException("application/xml"));

        assertBody(response, HttpStatus.NOT_ACCEPTABLE, "Requisição inválida.");
    }
}
