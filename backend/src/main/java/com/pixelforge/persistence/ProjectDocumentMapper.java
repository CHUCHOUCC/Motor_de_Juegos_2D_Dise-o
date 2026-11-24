package com.pixelforge.persistence;

import com.pixelforge.domain.GameObject;
import com.pixelforge.domain.Project;
import com.pixelforge.domain.Scene;
import com.pixelforge.domain.Vector2;
import com.pixelforge.domain.component.Component;
import com.pixelforge.domain.component.ComponentFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;

/**
 * Convierte un Project en un documento de MongoDB y al revés.
 *
 * <p>Un proyecto es un agregado: sus escenas, objetos y componentes no existen por separado. Por
 * eso se guarda como UN solo documento, con las escenas y los objetos anidados:
 *
 * <pre>
 * { _id, name, createdAt,
 *   scenes: [ { id, name, width, height, gravity, background,
 *               objects: [ { id, name, x, y, width, height,
 *                            components: [ { type, properties: {...} } ] } ] } ] }
 * </pre>
 *
 * <p>GRASP Fabricación pura y Alta cohesión: solo traduce; ni el dominio sabe de BSON ni el
 * repositorio sabe cómo es un GameObject por dentro. Al leer, los componentes se vuelven a crear con
 * la misma ComponentFactory del editor, así que las reglas de validación son las mismas.
 */
public class ProjectDocumentMapper {
  private final ComponentFactory components;

  public ProjectDocumentMapper(ComponentFactory components) {
    this.components = components;
  }

  // ---------- Dominio -> documento ----------

  public Document toDocument(Project project) {
    List<Document> scenes = new ArrayList<>();
    for (Scene scene : project.scenes()) scenes.add(scene(scene));
    return new Document("_id", project.id())
        .append("name", project.name())
        .append("createdAt", Date.from(project.createdAt()))
        .append("scenes", scenes);
  }

  private Document scene(Scene scene) {
    List<Document> objects = new ArrayList<>();
    for (GameObject object : scene.objects()) objects.add(object(object));
    return new Document("id", scene.id())
        .append("name", scene.name())
        .append("width", scene.width())
        .append("height", scene.height())
        .append("gravity", scene.gravity())
        .append("background", scene.background())
        .append("objects", objects);
  }

  private Document object(GameObject object) {
    List<Document> parts = new ArrayList<>();
    for (Component component : object.components())
      parts.add(new Document("type", component.type()).append("properties", new Document(component.properties())));
    return new Document("id", object.id())
        .append("name", object.name())
        .append("x", object.position().x())
        .append("y", object.position().y())
        .append("width", object.size().x())
        .append("height", object.size().y())
        .append("components", parts);
  }

  // ---------- Documento -> dominio ----------

  public Project toProject(Document document) {
    Project project = new Project(document.get("_id").toString(), (String) document.get("name"),
        ((Date) document.get("createdAt")).toInstant());
    for (Document sceneDoc : documents(document, "scenes")) {
      Scene scene = project.restoreScene((String) sceneDoc.get("id"), (String) sceneDoc.get("name"),
          number(sceneDoc, "width").intValue(), number(sceneDoc, "height").intValue(),
          number(sceneDoc, "gravity").doubleValue(), (String) sceneDoc.get("background"));
      for (Document objectDoc : documents(sceneDoc, "objects")) {
        GameObject object = scene.restoreObject((String) objectDoc.get("id"), (String) objectDoc.get("name"),
            new Vector2(number(objectDoc, "x").doubleValue(), number(objectDoc, "y").doubleValue()),
            new Vector2(number(objectDoc, "width").doubleValue(), number(objectDoc, "height").doubleValue()));
        for (Document componentDoc : documents(objectDoc, "components"))
          object.addComponent(components.create((String) componentDoc.get("type"), params(componentDoc)));
      }
    }
    return project;
  }

  /** Las propiedades guardadas pasan a texto, el mismo formato que llega desde el editor. */
  private static Map<String, String> params(Document componentDoc) {
    Map<String, String> params = new HashMap<>();
    Object properties = componentDoc.get("properties");
    if (properties instanceof Map<?, ?> map)
      for (Map.Entry<?, ?> entry : map.entrySet()) params.put(entry.getKey().toString(), String.valueOf(entry.getValue()));
    return params;
  }

  private static List<Document> documents(Document parent, String key) {
    List<Document> result = new ArrayList<>();
    Object value = parent.get(key);
    if (value instanceof List<?> list)
      for (Object item : list) if (item instanceof Document document) result.add(document);
    return result;
  }

  private static Number number(Document document, String key) {
    Object value = document.get(key);
    if (!(value instanceof Number number)) throw new IllegalStateException("Documento sin el campo numérico " + key);
    return number;
  }
}
