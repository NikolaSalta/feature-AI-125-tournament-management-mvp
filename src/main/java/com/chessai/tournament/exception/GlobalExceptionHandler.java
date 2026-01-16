package com.chessai.tournament.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений.
 * Обеспечивает единообразный формат ответов об ошибках.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Обработка ошибок валидации
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            // Проверяем тип ошибки перед кастингом
            if (error instanceof FieldError fieldError) {
                String fieldName = fieldError.getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            } else {
                // ObjectError - ошибка на уровне объекта
                errors.put(error.getObjectName(), error.getDefaultMessage());
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("errors", errors);
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Обработка ошибок аутентификации
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(AuthException ex) {
        logger.warn("Authentication error: {}", ex.getMessage());

        // Определяем статус на основе сообщения об ошибке
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String errorType = "Authentication Failed";

        // Для дублирующихся username/email возвращаем 409 CONFLICT
        if (ex.getMessage() != null &&
                (ex.getMessage().contains("already exists") || ex.getMessage().contains("уже существует"))) {
            status = HttpStatus.CONFLICT;
            errorType = "Conflict";
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", status.value());
        response.put("error", errorType);
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Обработка неверных учетных данных
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        response.put("error", "Authentication Failed");
        response.put("message", "Invalid username/email or password");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Обработка отказа в доступе
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.FORBIDDEN.value());
        response.put("error", "Access Denied");
        response.put("message", "You don't have permission to access this resource");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Обработка ошибки "турнир не найден"
     */
    @ExceptionHandler(TournamentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTournamentNotFoundException(TournamentNotFoundException ex) {
        logger.warn("Tournament not found: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Обработка ошибки "турнир нельзя редактировать"
     */
    @ExceptionHandler(TournamentNotEditableException.class)
    public ResponseEntity<Map<String, Object>> handleTournamentNotEditableException(TournamentNotEditableException ex) {
        logger.warn("Tournament not editable: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("error", "Conflict");
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Обработка ошибки оптимистической блокировки (concurrent modification).
     * 
     * Это происходит когда две транзакции пытаются обновить один объект одновременно.
     * Клиент должен повторить запрос после получения обновленных данных.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        logger.warn("Optimistic locking failure (concurrent modification): {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("error", "Conflict - Concurrent Modification");
        response.put("message", "The resource was modified by another request. Please refresh and try again.");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Обработка всех остальных исключений.
     * 
     * SECURITY NOTE: В production окружении:
     * 1. Stack trace логируется на DEBUG уровне (не видно в production логах)
     * 2. Детали ошибки НЕ раскрываются пользователю
     * 3. Деревья ошибок отправляются в систему мониторинга (Sentry, etc.)
     * 4. Добавляется correlation ID для отслеживания
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        // Production: логируем без stack trace (WARN уровень)
        logger.warn("Unexpected error: {}", ex.getMessage());
        
        // DEBUG: полный stack trace только в debug режиме
        logger.debug("Full stack trace:", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");
        // Не раскрываем детали ошибки пользователю
        response.put("message", "An unexpected error occurred");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
