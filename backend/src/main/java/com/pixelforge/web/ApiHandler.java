package com.pixelforge.web;

import com.pixelforge.application.EditorController;
import com.pixelforge.application.NotFoundException;
import com.pixelforge.application.PlayController;
import com.pixelforge.domain.component.ComponentFactory;
import com.pixelforge.domain.component.InputState;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Recibe todas las peticiones /api/... y las reparte.
 *
 * <p>Esta clase pertenece a la capa de interfaz: solo traduce HTTP a llamadas de los controladores
 * GRASP y su respuesta a JSON. No contiene reglas del juego.
 *
 * <p>GRASP Indirección: entre el navegador y el dominio están ApiHandler y los controladores, así
 * que cambiar la interfaz (por ejemplo, a una app de escritorio) no obliga a tocar el dominio.
 */
public class ApiHandler implements HttpHandler {
  private final EditorController editor;
  private final PlayController play;

  public ApiHandler(EditorController editor, PlayController play) {
    this.editor = editor;
    this.play = play;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    int status = 200;
    Object body;
    try {
      body = route(new Request(exchange));
    } catch (NotFoundException e) {
      status = 404;
      body = Map.of("error", e.getMessage());
    } catch (IllegalArgumentException e) {
      status = 400;
      body = Map.of("error", e.getMessage());
    } catch (RuntimeException e) {
      status = 500;
      body = Map.of("error", "Error interno del servidor");
      e.printStackTrace();
    }
    send(exchange, status, Json.write(body));
  }

  private Object route(Request r) {
    String[] p = r.path; // p[0] = "api"

    // ----- Catálogo para el editor -----
    if (r.matches("GET", "api/catalog")) {
      Map<String, Object> catalog = new LinkedHashMap<>();
      catalog.put("componentTypes", ComponentFactory.TYPES);
      catalog.put("presets", ComponentFactory.PRESETS);
      return catalog;
    }

    // ----- Proyectos -----
    if (r.matches("GET", "api/projects"))
      return editor.listProjects().stream().map(ViewMapper::projectSummary).toList();
    if (r.matches("POST", "api/projects"))
      return ViewMapper.project(editor.createProject(r.text("name")));
    if (r.matches("GET", "api/projects/*"))
      return ViewMapper.project(editor.project(p[2]));
    if (r.matches("POST", "api/projects/*/rename"))
      return ViewMapper.project(editor.renameProject(p[2], r.text("name")));
    if (r.matches("POST", "api/projects/*/delete")) {
      editor.deleteProject(p[2]);
      return Map.of("ok", true);
    }

    // ----- Escenas -----
    if (r.matches("POST", "api/projects/*/scenes"))
      return ViewMapper.scene(editor.createScene(p[2], r.text("name"), (int) r.number("width", 960),
          (int) r.number("height", 540), r.number("gravity", 1400), r.text("background", "#87ceeb")));
    if (r.matches("GET", "api/projects/*/scenes/*"))
      return ViewMapper.scene(editor.scene(p[2], p[4]));
    if (r.matches("POST", "api/projects/*/scenes/*/update"))
      return ViewMapper.scene(editor.updateScene(p[2], p[4], r.text("name", null), r.number("gravity", 1400),
          r.text("background", "#87ceeb")));
    if (r.matches("POST", "api/projects/*/scenes/*/delete")) {
      editor.deleteScene(p[2], p[4]);
      return Map.of("ok", true);
    }

    // ----- Objetos -----
    if (r.matches("POST", "api/projects/*/scenes/*/objects"))
      return ViewMapper.object(editor.createObject(p[2], p[4], r.text("name"), r.text("preset", "empty"),
          r.number("x", 0), r.number("y", 0), r.number("width", 48), r.number("height", 48)));
    if (r.matches("POST", "api/projects/*/scenes/*/objects/*/update"))
      return ViewMapper.object(editor.updateObject(p[2], p[4], p[6], r.text("name", null),
          r.number("x", 0), r.number("y", 0), r.number("width", 48), r.number("height", 48)));
    if (r.matches("POST", "api/projects/*/scenes/*/objects/*/delete")) {
      editor.deleteObject(p[2], p[4], p[6]);
      return Map.of("ok", true);
    }
    if (r.matches("POST", "api/projects/*/scenes/*/objects/*/components"))
      return ViewMapper.object(editor.addComponent(p[2], p[4], p[6], r.text("type"), r.params()));
    if (r.matches("POST", "api/projects/*/scenes/*/objects/*/components/*/delete"))
      return ViewMapper.object(editor.removeComponent(p[2], p[4], p[6], p[8]));

    // ----- Modo juego -----
    if (r.matches("POST", "api/projects/*/scenes/*/play"))
      return ViewMapper.session(play.start(p[2], p[4]));
    if (r.matches("POST", "api/sessions/*/step"))
      return ViewMapper.session(play.step(p[2], new InputState(r.flag("left"), r.flag("right"), r.flag("jump"))));

    throw new NotFoundException("Ruta no encontrada");
  }

  private static void send(HttpExchange exchange, int status, String json) throws IOException {
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }
}
