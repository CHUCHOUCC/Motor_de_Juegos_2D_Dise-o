package com.pixelforge.domain.component;

/** Teclas que el jugador tiene presionadas en un cuadro (frame) del juego. */
public record InputState(boolean left, boolean right, boolean jump) {
  public static final InputState NONE = new InputState(false, false, false);
}
