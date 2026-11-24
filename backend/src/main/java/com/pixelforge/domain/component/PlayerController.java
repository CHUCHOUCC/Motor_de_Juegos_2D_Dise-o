package com.pixelforge.domain.component;

import com.pixelforge.domain.GameObject;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mueve el objeto con las flechas y lo hace saltar. Necesita un RigidBody en el mismo objeto. */
public class PlayerController extends Component {
  private final double speed;
  private final double jumpSpeed;

  public PlayerController(double speed, double jumpSpeed) {
    if (speed <= 0 || speed > 2000 || jumpSpeed <= 0 || jumpSpeed > 3000)
      throw new IllegalArgumentException("Velocidad o salto fuera de rango");
    this.speed = speed;
    this.jumpSpeed = jumpSpeed;
  }

  @Override
  public String type() {
    return "PlayerController";
  }

  /** Polimorfismo: traduce las teclas en velocidad; solo salta si está en el suelo. */
  @Override
  public void update(GameObject owner, FrameContext frame) {
    owner.getComponent(RigidBody.class).ifPresent(body -> {
      double direction = (frame.input().right() ? 1 : 0) - (frame.input().left() ? 1 : 0);
      double vertical = body.velocity().y();
      if (frame.input().jump() && body.grounded()) vertical = -jumpSpeed;
      body.setVelocity(body.velocity().withX(direction * speed).withY(vertical));
    });
  }

  @Override
  public Map<String, Object> properties() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("speed", speed);
    values.put("jumpSpeed", jumpSpeed);
    return values;
  }

  @Override
  public Component copy() {
    return new PlayerController(speed, jumpSpeed);
  }
}
