package com.pixelforge.domain;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Un juego en construcción: tiene nombre y una o varias escenas (niveles).
 *
 * <p>GRASP Creador: el proyecto agrega sus escenas, así que es él quien las crea.
 * <p>GRASP Experto: buscar y quitar escenas lo hace el proyecto, que tiene la lista.
 */
public class Project implements Serializable {
  private final String id;
  private String name;
  private final Instant createdAt;
  private final List<Scene> scenes = new ArrayList<>();

  public Project(String id, String name) {
    // MongoDB guarda fechas con precisión de milisegundos; se trunca para que guardar no la cambie.
    this(id, name, Instant.now().truncatedTo(ChronoUnit.MILLIS));
  }

  /** Reconstruye un proyecto guardado, con su fecha original (lo usa la persistencia). */
  public Project(String id, String name, Instant createdAt) {
    this.id = id;
    rename(name);
    this.createdAt = createdAt;
  }

  /** GRASP Creador: el proyecto crea la escena porque la contiene. */
  public Scene createScene(String name, int width, int height, double gravity, String background) {
    return restoreScene(Ids.next(), name, width, height, gravity, background);
  }

  /** Igual que createScene pero con un id conocido; sirve para cargar desde la base de datos. */
  public Scene restoreScene(String sceneId, String name, int width, int height, double gravity, String background) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("La escena necesita un nombre");
    Scene scene = new Scene(sceneId, name.strip(), width, height, gravity, background);
    scenes.add(scene);
    return scene;
  }

  public Optional<Scene> findScene(String sceneId) {
    return scenes.stream().filter(scene -> scene.id().equals(sceneId)).findFirst();
  }

  public boolean removeScene(String sceneId) {
    return scenes.removeIf(scene -> scene.id().equals(sceneId));
  }

  public void rename(String newName) {
    if (newName == null || newName.isBlank() || newName.strip().length() > 60)
      throw new IllegalArgumentException("El nombre debe tener entre 1 y 60 caracteres");
    this.name = newName.strip();
  }

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public List<Scene> scenes() {
    return Collections.unmodifiableList(scenes);
  }
}
