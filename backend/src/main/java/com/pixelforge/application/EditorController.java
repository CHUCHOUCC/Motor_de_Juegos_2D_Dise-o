package com.pixelforge.application;

import com.pixelforge.domain.GameObject;
import com.pixelforge.domain.Ids;
import com.pixelforge.domain.Project;
import com.pixelforge.domain.Scene;
import com.pixelforge.domain.Vector2;
import com.pixelforge.domain.component.Component;
import com.pixelforge.domain.component.ComponentFactory;
import com.pixelforge.persistence.ProjectRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * GRASP Controlador (de caso de uso "Editar juego"): es el primer objeto, después de la interfaz,
 * que recibe los eventos del sistema del editor (crear proyecto, crear escena, mover objeto...).
 *
 * <p>No hace el trabajo él mismo: busca el proyecto, delega en el objeto experto (Project, Scene,
 * GameObject) y guarda. Así la web no conoce el dominio por dentro ni la forma de guardar
 * (GRASP Bajo acoplamiento).
 *
 * <p>Los métodos son synchronized porque el servidor atiende varias peticiones a la vez.
 */
public class EditorController {
  private final ProjectRepository repository;
  private final ComponentFactory components;

  public EditorController(ProjectRepository repository, ComponentFactory components) {
    this.repository = repository;
    this.components = components;
  }

  // ---------- Proyectos ----------

  public synchronized List<Project> listProjects() {
    return repository.findAll().stream().sorted(Comparator.comparing(Project::createdAt).reversed()).toList();
  }

  /** GRASP Creador: el controlador crea el Project porque luego lo registra en el repositorio. */
  public synchronized Project createProject(String name) {
    Project project = new Project(Ids.next(), name);
    repository.save(project);
    return project;
  }

  public synchronized Project project(String projectId) {
    return repository.findById(projectId).orElseThrow(() -> new NotFoundException("No existe el proyecto"));
  }

  public synchronized Project renameProject(String projectId, String name) {
    Project project = project(projectId);
    project.rename(name);
    repository.save(project);
    return project;
  }

  public synchronized void deleteProject(String projectId) {
    if (!repository.delete(projectId)) throw new NotFoundException("No existe el proyecto");
  }

  // ---------- Escenas ----------

  public synchronized Scene createScene(String projectId, String name, int width, int height, double gravity, String background) {
    Project project = project(projectId);
    Scene scene = project.createScene(name, width, height, gravity, background); // Creador: Project
    repository.save(project);
    return scene;
  }

  public synchronized Scene scene(String projectId, String sceneId) {
    return project(projectId).findScene(sceneId).orElseThrow(() -> new NotFoundException("No existe la escena"));
  }

  public synchronized Scene updateScene(String projectId, String sceneId, String name, double gravity, String background) {
    Project project = project(projectId);
    Scene scene = sceneOf(project, sceneId);
    if (name != null && !name.isBlank()) scene.rename(name.strip());
    scene.setGravity(gravity);
    scene.setBackground(background);
    repository.save(project);
    return scene;
  }

  public synchronized void deleteScene(String projectId, String sceneId) {
    Project project = project(projectId);
    if (!project.removeScene(sceneId)) throw new NotFoundException("No existe la escena");
    repository.save(project);
  }

  // ---------- Objetos ----------

  public synchronized GameObject createObject(String projectId, String sceneId, String name, String preset,
                                              double x, double y, double width, double height) {
    Project project = project(projectId);
    Scene scene = sceneOf(project, sceneId);
    List<Component> initial = components.preset(preset); // Fabricación pura decide los componentes
    GameObject object = scene.createObject(name, new Vector2(x, y), new Vector2(width, height)); // Creador: Scene
    initial.forEach(object::addComponent);
    repository.save(project);
    return object;
  }

  public synchronized GameObject updateObject(String projectId, String sceneId, String objectId,
                                              String name, double x, double y, double width, double height) {
    Project project = project(projectId);
    Scene scene = sceneOf(project, sceneId);
    GameObject object = objectOf(scene, objectId);
    if (x < 0 || y < 0 || x + width > scene.width() || y + height > scene.height())
      throw new IllegalArgumentException("El objeto debe quedar dentro de la escena");
    if (name != null && !name.isBlank()) object.rename(name.strip());
    object.resize(new Vector2(width, height));
    object.moveTo(new Vector2(x, y));
    repository.save(project);
    return object;
  }

  public synchronized void deleteObject(String projectId, String sceneId, String objectId) {
    Project project = project(projectId);
    if (!sceneOf(project, sceneId).removeObject(objectId)) throw new NotFoundException("No existe el objeto");
    repository.save(project);
  }

  // ---------- Componentes ----------

  public synchronized GameObject addComponent(String projectId, String sceneId, String objectId,
                                              String type, Map<String, String> params) {
    Project project = project(projectId);
    GameObject object = objectOf(sceneOf(project, sceneId), objectId);
    object.addComponent(components.create(type, params)); // Fabricación pura crea; Experto agrega
    repository.save(project);
    return object;
  }

  public synchronized GameObject removeComponent(String projectId, String sceneId, String objectId, String type) {
    Project project = project(projectId);
    GameObject object = objectOf(sceneOf(project, sceneId), objectId);
    if (!object.removeComponent(type)) throw new NotFoundException("El objeto no tiene ese componente");
    repository.save(project);
    return object;
  }

  private static Scene sceneOf(Project project, String sceneId) {
    return project.findScene(sceneId).orElseThrow(() -> new NotFoundException("No existe la escena"));
  }

  private static GameObject objectOf(Scene scene, String objectId) {
    return scene.findObject(objectId).orElseThrow(() -> new NotFoundException("No existe el objeto"));
  }
}
