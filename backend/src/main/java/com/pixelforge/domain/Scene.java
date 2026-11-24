package com.pixelforge.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Un nivel del juego: un área de ancho x alto con gravedad, color de fondo y sus objetos.
 *
 * <p>GRASP Creador: la escena contiene y agrega sus objetos, así que es ella quien los crea
 * ({@link #createObject}). Así también valida que queden dentro de sus límites.
 *
 * <p>GRASP Experto en información: buscar, quitar y copiar objetos lo hace la escena, que es quien
 * tiene la lista.
 */
public class Scene implements Serializable {
  private final String id;
  private String name;
  private final int width;
  private final int height;
  private double gravity;
  private String background;
  private final List<GameObject> objects = new ArrayList<>();

  /** Solo el proyecto crea escenas (ver Project.createScene). */
  Scene(String id, String name, int width, int height, double gravity, String background) {
    if (width < 160 || width > 4000 || height < 120 || height > 3000)
      throw new IllegalArgumentException("La escena debe medir entre 160x120 y 4000x3000");
    this.id = id;
    this.name = name;
    this.width = width;
    this.height = height;
    setGravity(gravity);
    setBackground(background);
  }

  /** GRASP Creador: la escena crea el objeto porque lo contiene y conoce sus límites. */
  public GameObject createObject(String name, Vector2 position, Vector2 size) {
    return restoreObject(Ids.next(), name, position, size);
  }

  /** Igual que createObject pero con un id conocido; sirve para cargar desde la base de datos. */
  public GameObject restoreObject(String objectId, String name, Vector2 position, Vector2 size) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("El objeto necesita un nombre");
    if (size.x() <= 0 || size.y() <= 0) throw new IllegalArgumentException("El tamaño debe ser mayor que cero");
    if (position.x() < 0 || position.y() < 0 || position.x() + size.x() > width || position.y() + size.y() > height)
      throw new IllegalArgumentException("El objeto debe quedar dentro de la escena");
    GameObject object = new GameObject(objectId, name.strip(), position, size);
    objects.add(object);
    return object;
  }

  public Optional<GameObject> findObject(String objectId) {
    return objects.stream().filter(object -> object.id().equals(objectId)).findFirst();
  }

  public boolean removeObject(String objectId) {
    return objects.removeIf(object -> object.id().equals(objectId));
  }

  /** Copia de todos los objetos, para jugar sin tocar la escena del editor. */
  public List<GameObject> copyObjects() {
    List<GameObject> copies = new ArrayList<>();
    for (GameObject object : objects) copies.add(object.copy());
    return copies;
  }

  public List<GameObject> objects() {
    return Collections.unmodifiableList(objects);
  }

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public double gravity() {
    return gravity;
  }

  public String background() {
    return background;
  }

  public void rename(String newName) {
    this.name = newName;
  }

  public void setGravity(double newGravity) {
    if (newGravity < 0 || newGravity > 5000) throw new IllegalArgumentException("Gravedad fuera de rango (0 a 5000)");
    this.gravity = newGravity;
  }

  public void setBackground(String color) {
    if (color == null || !color.matches("#[0-9a-fA-F]{6}"))
      throw new IllegalArgumentException("El color debe tener el formato #RRGGBB");
    this.background = color;
  }
}
