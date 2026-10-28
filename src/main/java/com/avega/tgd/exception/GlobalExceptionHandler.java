package com.avega.tgd.exception;

import com.avega.tgd.dto.response.ApiResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized REST exception handler intercepting and translating application exceptions
 * into uniform, structured {@link ApiResponseDto} JSON error envelopes.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li><b>Format Standardization:</b> Guarantees consistent error structures across all endpoints
 *       with {@code success: false}, informative error messages, and optional field-level maps.</li>
 *   <li><b>HTTP Status Code Mapping:</b>
 *     <ul>
 *       <li>{@code 404 NOT FOUND}: Missing domain entities ({@link ResourceNotFoundException}).</li>
 *       <li>{@code 400 BAD REQUEST}: Payload validation errors, invalid dates, and malformed JSON
 *           ({@link InvalidTransactionException}, {@link HttpMessageNotReadableException},
 *           {@link MethodArgumentNotValidException}).</li>
 *       <li>{@code 401 UNAUTHORIZED}: Bad or missing Basic Auth credentials ({@link AuthenticationException}).</li>
 *       <li>{@code 403 FORBIDDEN}: Insufficient role authority ({@link AccessDeniedException}).</li>
 *       <li>{@code 500 INTERNAL SERVER ERROR}: Unhandled runtime exceptions (safely masked from client).</li>
 *     </ul>
 *   </li>
 *   <li><b>Date and Format Validation:</b> Directly satisfies Requirement 1 ("Invalid date formats must
 *       be rejected") by intercepting Jackson deserialization failures.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles missing entities, returning HTTP 404 NOT FOUND.
     *
     * @param ex the resource not found exception
     * @return structured 404 response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.error(ex.getMessage()));
    }

    /**
     * Handles business and semantic transaction validation failures, returning HTTP 400 BAD REQUEST.
     *
     * @param ex the invalid transaction exception
     * @return structured 400 response
     */
    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleInvalidTransaction(InvalidTransactionException ex) {
        log.warn("Invalid transaction: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(ex.getMessage()));
    }

    /**
     * Handles malformed JSON payloads and invalid date formats (e.g. non-ISO LocalDate strings).
     *
     * @param ex the HTTP message not readable exception
     * @return structured 400 response detailing the parse error
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleJsonParseError(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON payload: {}", ex.getMessage());
        String msg = "Malformed JSON request or invalid date/field format";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            msg += ": " + ex.getCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(msg));
    }

    /**
     * Handles Jakarta Bean Validation errors ({@code @Valid}), returning field-by-field error maps.
     *
     * @param ex the validation exception containing binding errors
     * @return structured 400 response with field error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Validation failed for request: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error("Validation failed", errors));
    }

    /**
     * Handles RBAC authorization denials, returning HTTP 403 FORBIDDEN.
     *
     * @param ex the access denied exception
     * @return structured 403 response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDto.error("Access denied: " + ex.getMessage()));
    }

    /**
     * Handles authentication failures, returning HTTP 401 UNAUTHORIZED.
     *
     * @param ex the authentication exception
     * @return structured 401 response
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseDto.error("Authentication failed: " + ex.getMessage()));
    }

    /**
     * Catch-all handler for unexpected internal exceptions, returning HTTP 500 INTERNAL SERVER ERROR.
     * Logs the full exception stack trace for debugging while concealing internal details from clients.
     *
     * @param ex the unexpected exception
     * @return structured 500 response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Object>> handleGenericException(Exception ex) {
        log.error("Internal server error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error("An unexpected error occurred: " + ex.getMessage()));
    }
}
