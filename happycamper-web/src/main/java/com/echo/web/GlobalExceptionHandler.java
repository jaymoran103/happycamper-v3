package com.echo.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Global exception handling for the web layer.
 *
 * Lives outside any single controller because the exceptions handled here are raised
 * during request resolution (e.g. multipart parsing), before Spring dispatches to a
 * specific @RequestMapping method. A controller-local @ExceptionHandler does not catch
 * them — Spring's DefaultHandlerExceptionResolver wins and returns an empty body.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Translates Spring's {@link MaxUploadSizeExceededException} (thrown by the multipart
     * parser when a file or request exceeds {@code spring.servlet.multipart.max-file-size}
     * / {@code max-request-size}) into a 413 with the ADR-003 contract-shaped
     * {@code {error: string}} body.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        LOG.warn("Multipart upload rejected: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "Upload exceeds maximum allowed size: " + e.getMessage()));
    }
}
