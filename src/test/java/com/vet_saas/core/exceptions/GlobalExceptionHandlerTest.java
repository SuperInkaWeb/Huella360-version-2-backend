package com.vet_saas.core.exceptions;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ForbiddenException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.core.exceptions.types.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        lenient().when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    void handleResourceNotFound_returns404() {
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("Usuario not found"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Usuario not found", response.getBody().getMessage());
        assertEquals("/api/v1/test", response.getBody().getPath());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    void handleAccessDenied_returns403() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("Denied"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("No tienes permiso para acceder a este recurso", response.getBody().getMessage());
    }

    @Test
    void handleForbidden_returns403() {
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(
                new ForbiddenException("Custom forbidden"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Custom forbidden", response.getBody().getMessage());
    }

    @Test
    void handleBusinessException_returns400() {
        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(
                new BusinessException("Invalid order"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid order", response.getBody().getMessage());
    }

    @Test
    void handleUnauthorizedException_returns401() {
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorizedException(
                new UnauthorizedException("Token expired"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Token expired", response.getBody().getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleValidationException_returns400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "nombre", "El nombre es obligatorio");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error de validación en los datos enviados", response.getBody().getMessage());
        assertNotNull(response.getBody().getValidationErrors());
        assertEquals("El nombre es obligatorio", response.getBody().getValidationErrors().get("nombre"));
    }

    @Test
    void handleGlobalException_returns500() {
        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(
                new RuntimeException("Something broke"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error interno del servidor", response.getBody().getMessage());
    }
}
