package com.pixelforge.domain;

import java.util.UUID;

/** Genera identificadores cortos para proyectos, escenas y objetos. */
public final class Ids {
  private Ids() {}

  public static String next() {
    return UUID.randomUUID().toString().substring(0, 8);
  }
}
