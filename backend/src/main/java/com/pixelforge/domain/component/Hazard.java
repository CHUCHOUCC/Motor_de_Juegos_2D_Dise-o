package com.pixelforge.domain.component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Si el jugador lo toca, pierde la partida (pinchos, enemigos). */
public class Hazard extends Component {
  @Override
  public String type() {
    return "Hazard";
  }

  @Override
  public Map<String, Object> properties() {
    return new LinkedHashMap<>();
  }

  @Override
  public Component copy() {
    return new Hazard();
  }
}
