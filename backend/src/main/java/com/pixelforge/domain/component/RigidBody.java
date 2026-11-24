package com.pixelforge.domain.component;

import com.pixelforge.domain.GameObject;
import com.pixelforge.domain.Vector2;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hace que el objeto tenga velocidad y le afecte la gravedad.
 * Experto: guarda su velocidad y sabe si está apoyado en el suelo.
 */
public class RigidBody extends Component {
  private final double gravityScale;
  private Vector2 velocity = Vector2.ZERO;
  private boolean grounded;

  public RigidBody(double gravityScale) {
    if (gravityScale < 0 || gravityScale > 5) throw new IllegalArgumentException("gravityScale debe estar entre 0 y 5");
    this.gravityScale = gravityScale;
  }

  @Override
  public String type() {
    return "RigidBody";
  }

  /** Polimorfismo: un cuerpo rígido acelera hacia abajo según la gravedad de la escena. */
  @Override
  public void update(GameObject owner, FrameContext frame) {
    velocity = velocity.plus(new Vector2(0, frame.gravity() * gravityScale * frame.dt()));
  }

  public Vector2 velocity() {
    return velocity;
  }

  public void setVelocity(Vector2 newVelocity) {
    this.velocity = newVelocity;
  }

  public boolean grounded() {
    return grounded;
  }

  public void setGrounded(boolean value) {
    this.grounded = value;
  }

  @Override
  public Map<String, Object> properties() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("gravityScale", gravityScale);
    return values;
  }

  @Override
  public Component copy() {
    return new RigidBody(gravityScale);
  }
}
