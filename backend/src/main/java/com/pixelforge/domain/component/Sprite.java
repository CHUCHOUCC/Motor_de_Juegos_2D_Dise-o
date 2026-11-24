package com.pixelforge.domain.component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Cómo se dibuja el objeto: un color y una forma. Es un componente de solo datos. */
public class Sprite extends Component {
  public enum Shape { RECT, CIRCLE }

  private final String color;
  private final Shape shape;

  public Sprite(String color, Shape shape) {
    if (color == null || !color.matches("#[0-9a-fA-F]{6}"))
      throw new IllegalArgumentException("El color debe tener el formato #RRGGBB");
    this.color = color;
    this.shape = shape;
  }

  @Override
  public String type() {
    return "Sprite";
  }

  @Override
  public Map<String, Object> properties() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("color", color);
    values.put("shape", shape.name());
    return values;
  }

  @Override
  public Component copy() {
    return new Sprite(color, shape);
  }
}
