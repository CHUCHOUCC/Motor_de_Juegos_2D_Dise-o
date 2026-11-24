package com.pixelforge.application;

/** Se pidió un proyecto, escena, objeto o partida que no existe (la web responde 404). */
public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) {
    super(message);
  }
}
