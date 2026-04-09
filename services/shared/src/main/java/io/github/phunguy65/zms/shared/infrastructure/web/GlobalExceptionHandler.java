package io.github.phunguy65.zms.shared.infrastructure.web;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maps Bean Validation failures to a JSend {@code fail} response with field-level violations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<JsendResponse<FailData>> handleValidation(
            MethodArgumentNotValidException ex) {
        List<Violation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new Violation(
                        fe.getField(), fe.getDefaultMessage(), resolveViolationCode(fe.getCode())))
                .toList();

        FailData body =
                new FailData("Validation failed", CommonErrorCode.VALIDATION_ERROR, violations);
        return ResponseEntity.badRequest().body(JsendResponse.fail(body));
    }

    /** Catches all unhandled exceptions and returns a generic 500 error response. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<JsendResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(JsendResponse.error("An unexpected error occurred"));
    }

    private ViolationCode resolveViolationCode(String constraintCode) {
        if (constraintCode == null) return ViolationCode.INVALID_VALUE;
        return switch (constraintCode) {
            case "NotBlank", "NotNull", "NotEmpty" -> ViolationCode.REQUIRED;
            case "Email", "Pattern" -> ViolationCode.INVALID_FORMAT;
            case "Size", "Min" -> ViolationCode.TOO_SHORT;
            case "Max" -> ViolationCode.TOO_LONG;
            default -> ViolationCode.INVALID_VALUE;
        };
    }
}
