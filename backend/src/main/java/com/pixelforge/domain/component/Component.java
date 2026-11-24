package com.pixelforge.domain.component;

import com.pixelforge.domain.GameObject;
import java.io.Serializable;
import java.util.Map;

/**
 * Comportamiento o dato que se le agrega a un GameObject (sprite, física, control, etc.).
 *
 * <p>GRASP Polimorfismo: cada tipo de componente responde a {@link #update} a su manera. La sesión
 * de juego recorre los componentes y llama update() sin preguntar de qué tipo son, así que no hay
 * un switch gigante por tipo.
 *
 * <p>GRASP Variaciones protegidas: agregar un componente nuevo (por ejemplo, "Trampolín") es crear
 * otra subclase y registrarla en ComponentFactory; ni la sesión ni el editor cambian.
 */
public abstract class Component implements Serializable {

  /** Nombre del tipo, el mismo que usa la API: "Sprite", "RigidBody", ... */
  public abstract String type();

  /** Se llama una vez por cuadro durante el juego. Por defecto no hace nada (componentes de datos). */
  public void update(GameObject owner, FrameContext frame) {}

  /** Propiedades editables, para mostrarlas en el editor (Experto: el componente conoce sus datos). */
  public abstract Map<String, Object> properties();

  /** Copia independiente, usada al empezar una partida. */
  public abstract Component copy();
}
