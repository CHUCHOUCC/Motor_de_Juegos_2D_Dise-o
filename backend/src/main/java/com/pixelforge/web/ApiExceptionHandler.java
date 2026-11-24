package com.pixelforge.web;

import com.pixelforge.application.NotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Convierte las excepciones en respuestas {"error": "..."} con su código HTTP, que es lo que el
 * frontend muestra al usuario.
 *
 * <p>GRASP Alta cohesión: el manejo de errores HTTP queda en un solo lugar y los controladores
 * REST quedan limpios.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Map<String, String>> notFound(NotFoundException e) {
    return error(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Map<String, String>> unknownRoute(NoResourceFoundException e) {
    return error(HttpStatus.NOT_FOUND, "Ruta no encontrada");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
    return error(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Map<String, String>> notANumber(MethodArgumentTypeMismatchException e) {
    return error(HttpStatus.BAD_REQUEST, "El campo " + e.getName() + " debe ser un número");
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, String>> unexpected(RuntimeException e) {
    e.printStackTrace();
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
  }

  private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(Map.of("error", message == null ? status.getReasonPhrase() : message));
  }
}
