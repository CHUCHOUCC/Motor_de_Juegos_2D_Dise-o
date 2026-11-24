package com.pixelforge.runtime;

import com.pixelforge.domain.GameObject;
import com.pixelforge.domain.Vector2;
import com.pixelforge.domain.component.BoxCollider;
import com.pixelforge.domain.component.RigidBody;
import java.util.List;

/**
 * Mueve los cuerpos según su velocidad y evita que atraviesen los objetos sólidos.
 *
 * <p>GRASP Fabricación pura: "sistema de física" no es un objeto del juego; se inventa para que el
 * cálculo de movimiento y choques no ensucie a GameObject ni a GameSession.
 *
 * <p>GRASP Alta cohesión: solo hace física. No sabe de puntos, teclas ni HTTP.
 */
public class PhysicsSystem {

  /** Mueve un cuerpo en x y luego en y, resolviendo choques en cada eje por separado. */
  public void move(GameObject body, List<GameObject> solids, double dt) {
    RigidBody rigidBody = body.getComponent(RigidBody.class).orElse(null);
    if (rigidBody == null) return;
    Vector2 velocity = rigidBody.velocity();

    // Eje X: si choca con un sólido, se pega a su borde y la velocidad horizontal queda en 0.
    body.translate(new Vector2(velocity.x() * dt, 0));
    for (GameObject solid : solids) {
      if (solid == body || !solid.has(BoxCollider.class) || !body.overlaps(solid)) continue;
      double x = velocity.x() > 0 ? solid.left() - body.size().x() : solid.right();
      body.moveTo(body.position().withX(x));
      velocity = velocity.withX(0);
    }

    // Eje Y: si cae sobre un sólido queda "en el suelo"; si choca con el techo, deja de subir.
    rigidBody.setGrounded(false);
    body.translate(new Vector2(0, velocity.y() * dt));
    for (GameObject solid : solids) {
      if (solid == body || !solid.has(BoxCollider.class) || !body.overlaps(solid)) continue;
      if (velocity.y() > 0) {
        body.moveTo(body.position().withY(solid.top() - body.size().y()));
        rigidBody.setGrounded(true);
      } else {
        body.moveTo(body.position().withY(solid.bottom()));
      }
      velocity = velocity.withY(0);
    }
    rigidBody.setVelocity(velocity);
  }
}
