package com.pixelforge.domain;

import com.pixelforge.domain.component.Component;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Objeto de una escena (jugador, plataforma, moneda...). Por sí solo solo tiene posición y tamaño;
 * su comportamiento lo dan los componentes que se le agregan.
 *
 * <p>GRASP Experto en información: el objeto conoce su posición y su tamaño, así que es él quien
 * calcula sus bordes y si se superpone con otro objeto. También conoce sus componentes, así que es
 * él quien los busca por tipo.
 */
public class GameObject implements Serializable {
  private final String id;
  private String name;
  private Vector2 position;
  private Vector2 size;
  private final List<Component> components = new ArrayList<>();

  /** Solo la escena crea objetos (ver Scene.createObject), por eso el constructor es de paquete. */
  GameObject(String id, String name, Vector2 position, Vector2 size) {
    this.id = id;
    this.name = name;
    this.position = position;
    this.size = size;
  }

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }

  public Vector2 position() {
    return position;
  }

  public Vector2 size() {
    return size;
  }

  public void rename(String newName) {
    this.name = newName;
  }

  public void moveTo(Vector2 newPosition) {
    this.position = newPosition;
  }

  public void translate(Vector2 delta) {
    this.position = position.plus(delta);
  }

  public void resize(Vector2 newSize) {
    if (newSize.x() <= 0 || newSize.y() <= 0)
      throw new IllegalArgumentException("El tamaño debe ser mayor que cero");
    this.size = newSize;
  }

  // ---- Componentes (Experto: el objeto conoce su propia lista) ----

  /** Agrega un componente; si ya había uno del mismo tipo, lo reemplaza. */
  public void addComponent(Component component) {
    removeComponent(component.type());
    components.add(component);
  }

  public boolean removeComponent(String type) {
    return components.removeIf(component -> component.type().equals(type));
  }

  public <T extends Component> Optional<T> getComponent(Class<T> kind) {
    for (Component component : components)
      if (kind.isInstance(component)) return Optional.of(kind.cast(component));
    return Optional.empty();
  }

  public boolean has(Class<? extends Component> kind) {
    return getComponent(kind).isPresent();
  }

  public List<Component> components() {
    return Collections.unmodifiableList(components);
  }

  // ---- Geometría (Experto: el objeto conoce su caja) ----

  public double left() {
    return position.x();
  }

  public double right() {
    return position.x() + size.x();
  }

  public double top() {
    return position.y();
  }

  public double bottom() {
    return position.y() + size.y();
  }

  /** True si las cajas de los dos objetos se tocan (colisión AABB). */
  public boolean overlaps(GameObject other) {
    return left() < other.right()
        && right() > other.left()
        && top() < other.bottom()
        && bottom() > other.top();
  }

  /**
   * Copia profunda, usada al empezar una partida para que jugar no modifique la escena guardada.
   * Cada componente sabe copiarse a sí mismo (polimorfismo).
   */
  public GameObject copy() {
    GameObject clone = new GameObject(id, name, position, size);
    for (Component component : components) clone.components.add(component.copy());
    return clone;
  }
}
