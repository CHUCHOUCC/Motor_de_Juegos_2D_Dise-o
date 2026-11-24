package com.pixelforge.domain.component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Objeto que el jugador recoge para sumar puntos (una moneda). */
public class Collectible extends Component {
  private final int points;

  public Collectible(int points) {
    if (points < 1 || points > 1000) throw new IllegalArgumentException("Los puntos deben estar entre 1 y 1000");
    this.points = points;
  }

  @Override
  public String type() {
    return "Collectible";
  }

  public int points() {
    return points;
  }

  @Override
  public Map<String, Object> properties() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("points", points);
    return values;
  }

  @Override
  public Component copy() {
    return new Collectible(points);
  }
}
