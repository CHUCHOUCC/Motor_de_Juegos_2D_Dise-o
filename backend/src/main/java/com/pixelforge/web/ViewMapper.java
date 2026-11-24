package com.pixelforge.web;

import com.pixelforge.domain.GameObject;
import com.pixelforge.domain.Project;
import com.pixelforge.domain.Scene;
import com.pixelforge.domain.component.Component;
import com.pixelforge.runtime.GameSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduce objetos del dominio a mapas que luego se escriben como JSON.
 *
 * <p>GRASP Fabricación pura / Bajo acoplamiento: el dominio no sabe que existe una web ni JSON;
 * toda la traducción hacia la interfaz queda aquí.
 */
public final class ViewMapper {
  private ViewMapper() {}

  public static Map<String, Object> projectSummary(Project project) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", project.id());
    view.put("name", project.name());
    view.put("createdAt", project.createdAt().toString());
    view.put("scenes", project.scenes().size());
    return view;
  }

  public static Map<String, Object> project(Project project) {
    Map<String, Object> view = projectSummary(project);
    view.put("scenes", project.scenes().stream().map(ViewMapper::sceneSummary).toList());
    return view;
  }

  public static Map<String, Object> sceneSummary(Scene scene) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", scene.id());
    view.put("name", scene.name());
    view.put("width", scene.width());
    view.put("height", scene.height());
    view.put("gravity", scene.gravity());
    view.put("background", scene.background());
    view.put("objects", scene.objects().size());
    return view;
  }

  public static Map<String, Object> scene(Scene scene) {
    Map<String, Object> view = sceneSummary(scene);
    view.put("objects", objects(scene.objects()));
    return view;
  }

  public static List<Map<String, Object>> objects(List<GameObject> objects) {
    return objects.stream().map(ViewMapper::object).toList();
  }

  public static Map<String, Object> object(GameObject object) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", object.id());
    view.put("name", object.name());
    view.put("x", object.position().x());
    view.put("y", object.position().y());
    view.put("width", object.size().x());
    view.put("height", object.size().y());
    view.put("components", object.components().stream().map(ViewMapper::component).toList());
    return view;
  }

  public static Map<String, Object> component(Component component) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("type", component.type());
    view.put("properties", component.properties());
    return view;
  }

  public static Map<String, Object> session(GameSession session) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", session.id());
    view.put("scene", session.sceneName());
    view.put("width", session.width());
    view.put("height", session.height());
    view.put("background", session.background());
    view.put("status", session.status().name());
    view.put("score", session.score());
    view.put("remaining", session.remainingCollectibles());
    view.put("elapsed", session.elapsed());
    view.put("objects", objects(session.objects()));
    return view;
  }
}
