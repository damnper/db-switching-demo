package com.example.dbswitchingdemo.exception;

import com.example.dbswitchingdemo.dto.response.CommonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * <p>Глобальный обработчик исключений для приложения. Используется для централизованной
 * обработки исключений, возникающих в контроллерах.</p>
 *
 * <p>Методы этого класса обрабатывают специфические и общие исключения,
 * возвращая соответствующие HTTP статусы и сообщения об ошибках.</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * <p>Обработчик исключений {@link DataSourceFailedConnectionException}.</p>
     *
     * <p>Этот метод вызывается, когда возникает исключение, связанное с ошибкой
     * подключения к базе данных. Возвращает HTTP статус 500 (INTERNAL_SERVER_ERROR) и сообщение об ошибке.</p>
     *
     * @param ex исключение {@link DataSourceFailedConnectionException}, содержащее информацию об ошибке
     * @return объект {@link ResponseEntity}, содержащий {@link CommonResponse} с сообщением и статусом ошибки
     */
    @ExceptionHandler(DataSourceFailedConnectionException.class)
    public ResponseEntity<CommonResponse> handleDataSourceFailedConnectionException(DataSourceFailedConnectionException ex) {
        CommonResponse response = CommonResponse.builder().status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    /**
     * <p>Обработчик исключений {@link ResourceNotFound}.</p>
     *
     * <p>Этот метод вызывается, когда возникает исключение, связанное с отсутствием какого-либо ресурса.
     * Возвращает HTTP статус 404 (NOT_FOUND) и сообщение об ошибке.</p>
     *
     * @param ex исключение {@link ResourceNotFound}, содержащее информацию об ошибке
     * @return объект {@link ResponseEntity}, содержащий {@link CommonResponse} с сообщением и статусом ошибки
     */
    @ExceptionHandler(ResourceNotFound.class)
    public ResponseEntity<CommonResponse> handleResourceNotFound(ResourceNotFound ex) {
        CommonResponse response = CommonResponse.builder().status(HttpStatus.NOT_FOUND.name())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @ExceptionHandler(LogSwitchFailedException.class)
    public ResponseEntity<CommonResponse> handleLogSwitchFailedException(LogSwitchFailedException ex) {
        CommonResponse response = CommonResponse.builder().status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    /**
     * <p>Обработчик общих исключений {@link Exception}.</p>
     *
     * <p>Этот метод обрабатывает все остальные исключения, не указанные явно в других
     * методах. Возвращает HTTP статус 500 (INTERNAL_SERVER_ERROR) и сообщение об ошибке.</p>
     *
     * @param ex исключение {@link Exception}, содержащее информацию об ошибке
     * @return объект {@link ResponseEntity}, содержащий {@link CommonResponse} с сообщением об ошибке и статусом
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse> handleGenericException(Exception ex) {
        CommonResponse response = CommonResponse.builder().status(HttpStatus.INTERNAL_SERVER_ERROR .name())
                .message("An unexpected error occurred: " + ex.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
}
