package com.certifika2.cursos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Si Kinesis no acepta el evento, el cliente recibe 503 y puede reintentar sin riesgo. */
@RestControllerAdvice
public class StreamingExceptionHandler {

    @ExceptionHandler(PublicacionEventoException.class)
    public ProblemDetail handlePublicacion(PublicacionEventoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }
}
