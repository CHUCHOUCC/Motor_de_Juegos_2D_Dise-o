package com.pixelforge.domain.component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Marca el objeto como sólido: los cuerpos con RigidBody chocan con él y no lo atraviesan.
 * La prueba de superposición la hace GameObject.overlaps (Experto en su propia caja).
 */
public class BoxCollider extends Component {
  @Override
  public String type() {
    return "BoxCollider";
  }

  @Override
  public Map<String, Object> properties() {
    return new LinkedHashMap<>();
  }

  @Override
  public Component copy() {
    return new BoxCollider();
  }
}
