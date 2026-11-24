package com.pixelforge.domain.component;

import com.pixelforge.domain.GameObject;
import com.pixelforge.domain.Vector2;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mueve el objeto de lado a lado (por ejemplo, un enemigo que patrulla una plataforma). */
public class Patrol extends Component {
  private final double distance;
  private final double speed;
  private Double originX;
  private int direction = 1;

  public Patrol(double distance, double speed) {
    if (distance <= 0 || distance > 4000 || speed <= 0 || speed > 2000)
      throw new IllegalArgumentException("Distancia o velocidad de patrulla fuera de rango");
    this.distance = distance;
    this.speed = speed;
  }

  @Override
  public String type() {
    return "Patrol";
  }

  /** Polimorfismo: avanza y da la vuelta al llegar al límite del recorrido. */
  @Override
  public void update(GameObject owner, FrameContext frame) {
    if (originX == null) originX = owner.position().x();
    owner.translate(new Vector2(direction * speed * frame.dt(), 0));
    if (owner.position().x() > originX + distance) direction = -1;
    if (owner.position().x() < originX) direction = 1;
  }

  @Override
  public Map<String, Object> properties() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("distance", distance);
    values.put("speed", speed);
    return values;
  }

  @Override
  public Component copy() {
    return new Patrol(distance, speed);
  }
}
