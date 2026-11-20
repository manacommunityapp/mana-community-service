package com.manacommunity.api.exception;

import com.manacommunity.api.homeservice.exception.HomeServiceNotFoundException;
import com.manacommunity.api.homeservice.exception.SlotConflictException;
import com.manacommunity.api.payments.exception.InsufficientWalletBalanceException;
import com.manacommunity.api.payments.exception.TransactionLimitExceededException;
import com.manacommunity.api.workflow.WorkflowTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    @DisplayName("ResponseStatusException with 404 should return 404 NOT_FOUND")
    void testResponseStatusException_NotFound() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment order not found: 99");

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getMessage()).contains("Payment order not found: 99");
    }

    @Test
    @DisplayName("ResponseStatusException with 403 should return 403 FORBIDDEN")
    void testResponseStatusException_Forbidden() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getError()).isEqualTo("FORBIDDEN");
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("Custom exception annotated with @ResponseStatus(PAYMENT_REQUIRED) resolves to 402")
    void testAnnotatedException_PaymentRequired() {
        InsufficientWalletBalanceException ex = new InsufficientWalletBalanceException("Insufficient coins for transfer");

        ResponseEntity<ErrorResponse> response = handler.handleAll(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(402);
        assertThat(response.getBody().getMessage()).isEqualTo("Insufficient coins for transfer");
    }

    @Test
    @DisplayName("Custom exception annotated with @ResponseStatus(UNPROCESSABLE_ENTITY) resolves to 422")
    void testAnnotatedException_UnprocessableEntity() {
        TransactionLimitExceededException ex = new TransactionLimitExceededException("Daily limit exceeded");

        ResponseEntity<ErrorResponse> response = handler.handleAll(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(422);
        assertThat(response.getBody().getMessage()).isEqualTo("Daily limit exceeded");
    }

    @Test
    @DisplayName("WorkflowTransitionException with @ResponseStatus resolves to 422")
    void testAnnotatedException_WorkflowTransition() {
        WorkflowTransitionException ex = new WorkflowTransitionException("Invalid transition from DRAFT to COMPLETED");

        ResponseEntity<ErrorResponse> response = handler.handleAll(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(422);
    }

    @Test
    @DisplayName("HomeServiceNotFoundException annotated with @ResponseStatus resolves to 404")
    void testHomeServiceNotFoundException() {
        HomeServiceNotFoundException ex = new HomeServiceNotFoundException("Service not found");

        ResponseEntity<ErrorResponse> response = handler.handleAll(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("SlotConflictException annotated with @ResponseStatus resolves to 409")
    void testSlotConflictException() {
        SlotConflictException ex = new SlotConflictException("Slot already reserved");

        ResponseEntity<ErrorResponse> response = handler.handleAll(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("MethodArgumentNotValidException includes both field and global errors")
    void testMethodArgumentNotValidException_FieldAndGlobalErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError = new FieldError("userDto", "email", "must be a valid email");
        ObjectError globalError = new ObjectError("userDto", "Passwords do not match");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(bindingResult.getGlobalErrors()).thenReturn(List.of(globalError));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFieldErrors()).hasSize(2);
        assertThat(response.getBody().getFieldErrors().get(0).getField()).isEqualTo("email");
        assertThat(response.getBody().getFieldErrors().get(0).getMessage()).isEqualTo("must be a valid email");
        assertThat(response.getBody().getFieldErrors().get(1).getField()).isEqualTo("userDto");
        assertThat(response.getBody().getFieldErrors().get(1).getMessage()).isEqualTo("Passwords do not match");
    }

    @Test
    @DisplayName("ServletRequestBindingException resolves to 400 BAD_REQUEST")
    void testServletRequestBindingException() {
        ServletRequestBindingException ex = new ServletRequestBindingException("Missing request header 'X-Community-Id'");

        ResponseEntity<ErrorResponse> response = handler.handleServletRequestBinding(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("MISSING_REQUEST_PARAMETER");
        assertThat(response.getBody().getMessage()).contains("X-Community-Id");
    }

    @Test
    @DisplayName("TransactionSystemException unwraps ConstraintViolationException into 400 with field errors")
    void testTransactionSystemException_UnwrapsConstraintViolation() {
        ConstraintViolation<?> cv = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("user.email");
        when(cv.getPropertyPath()).thenReturn(path);
        when(cv.getMessage()).thenReturn("must not be null");

        ConstraintViolationException cve = new ConstraintViolationException(Set.of(cv));
        TransactionSystemException tse = new TransactionSystemException("Commit failed", cve);

        ResponseEntity<ErrorResponse> response = handler.handleTransactionSystem(tse, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("CONSTRAINT_VIOLATION");
        assertThat(response.getBody().getFieldErrors()).isNotEmpty();
        assertThat(response.getBody().getFieldErrors().get(0).getField()).isEqualTo("email");
        assertThat(response.getBody().getFieldErrors().get(0).getMessage()).isEqualTo("must not be null");
    }

    @Test
    @DisplayName("DataIntegrityViolationException with SQLSTATE 22001 maps to 400 VALUE_TOO_LONG")
    void testDataIntegrity_ValueTooLong() {
        DataIntegrityViolationException dive = new DataIntegrityViolationException(
                "Statement execution failed",
                new RuntimeException("ERROR: value too long for type character varying(20) [SQLSTATE 22001]")
        );

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(dive, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("VALUE_TOO_LONG");
    }

    @Test
    @DisplayName("DataIntegrityViolationException with unique constraint maps to 409 CONFLICT")
    void testDataIntegrity_UniqueConstraint() {
        DataIntegrityViolationException dive = new DataIntegrityViolationException(
                "Statement execution failed",
                new RuntimeException("duplicate key value violates unique constraint \"uq_reg_event_player\"")
        );

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(dive, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("DATA_CONFLICT");
        assertThat(response.getBody().getMessage()).contains("already registered for this event");
    }

    @Test
    @DisplayName("Generic unhandled exception maps to 500 without leaking stack traces")
    void testGenericFallbackException() {
        RuntimeException ex = new RuntimeException("Unexpected internal null reference");

        ResponseEntity<ErrorResponse> response = handler.handleAll(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later or contact support.");
    }

    @Test
    @DisplayName("Data integrity violation never leaks [db: ...] or table constraint names to client")
    void testDataIntegrityNeverLeaksConstraintNameOrDbHints() {
        DataIntegrityViolationException dive = new DataIntegrityViolationException(
                "Execution error",
                new RuntimeException("duplicate key value violates unique constraint \"uq_users_phone_number\" on table \"tbl_users\"")
        );

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(dive, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).doesNotContain("[db:");
        assertThat(response.getBody().getMessage()).doesNotContain("uq_users_phone_number");
        assertThat(response.getBody().getMessage()).doesNotContain("tbl_users");
    }

    @Test
    @DisplayName("ResourceNotFoundException cleans table names into friendly entity names")
    void testResourceNotFoundSanitizesTableNames() {
        ResourceNotFoundException ex = new ResourceNotFoundException("tbl_property_tenants", 42L);

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Property Tenant not found with id: 42");
        assertThat(response.getBody().getMessage()).doesNotContain("tbl_property_tenants");
    }

    @Test
    @DisplayName("DuplicateResourceException cleans table and field names and never leaks raw values")
    void testDuplicateResourceSanitizesTableAndFieldName() {
        DuplicateResourceException ex = new DuplicateResourceException("tbl_residents", "col_phone_number", "9876543210");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicate(ex, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("A Resident with that phone number already exists.");
        assertThat(response.getBody().getMessage()).doesNotContain("tbl_residents");
        assertThat(response.getBody().getMessage()).doesNotContain("9876543210");
        assertThat(response.getBody().getMessage()).doesNotContain("col_phone_number");
    }

    @Test
    @DisplayName("InvalidPhoneNumberException never leaks raw phone numbers")
    void testInvalidPhoneNumberNeverLeaksPhoneNumber() {
        InvalidPhoneNumberException ex = new InvalidPhoneNumberException("+91 9876543210");

        ResponseEntity<ErrorResponse> response = handler.handleManaCommunity(ex, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).doesNotContain("9876543210");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid phone number format. Please provide a valid 10-digit mobile number.");
    }

    @Test
    @DisplayName("SmsRateLimitException never leaks phone number or user identifiers")
    void testSmsRateLimitNeverLeaksPhoneNumber() {
        SmsRateLimitException ex = new SmsRateLimitException("phone:+919876543210");

        ResponseEntity<ErrorResponse> response = handler.handleManaCommunity(ex, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).doesNotContain("9876543210");
        assertThat(response.getBody().getMessage()).isEqualTo("SMS rate limit exceeded. Please wait a few minutes before trying again.");
    }
}
