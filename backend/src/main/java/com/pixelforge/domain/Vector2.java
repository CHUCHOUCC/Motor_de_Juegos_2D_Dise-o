package com.pixelforge.domain;

import java.io.Serializable;

/**
 * Par (x, y) inmutable: sirve para posiciones, tamaños y velocidades.
 * Al ser inmutable se puede compartir sin miedo a que otro objeto lo cambie.
 */
public record Vector2(double x, double y) implements Serializable {
  public static final Vector2 ZERO = new Vector2(0, 0);

  public Vector2 plus(Vector2 other) {
    return new Vector2(x + other.x, y + other.y);
  }

  public Vector2 times(double factor) {
    return new Vector2(x * factor, y * factor);
  }

  public Vector2 withX(double newX) {
    return new Vector2(newX, y);
  }

  public Vector2 withY(double newY) {
    return new Vector2(x, newY);
  }
}
