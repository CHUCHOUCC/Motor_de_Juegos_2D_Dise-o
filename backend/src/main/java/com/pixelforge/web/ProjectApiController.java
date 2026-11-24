package com.pixelforge.web;

import com.pixelforge.application.EditorController;
import com.pixelforge.domain.component.ComponentFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del editor: proyectos, escenas, objetos y componentes.
 *
 * <p>Capa de presentación: solo traduce HTTP a llamadas del EditorController y su respuesta a
 * mapas (que Spring escribe como JSON). No contiene reglas del juego.
 *
 * <p>GRASP Indirección: entre el navegador y el dominio están esta clase y el controlador GRASP,
 * así que cambiar la interfaz no obliga a tocar el dominio.
 */
@RestController
@RequestMapping("/api")
public class ProjectApiController {
  private final EditorController editor;

  public ProjectApiController(EditorController editor) {
    this.editor = editor;
  }

  // ----- Catálogo para el editor -----

  @GetMapping("/catalog")
  public Map<String, Object> catalog() {
    Map<String, Object> catalog = new LinkedHashMap<>();
    catalog.put("componentTypes", ComponentFactory.TYPES);
    catalog.put("presets", ComponentFactory.PRESETS);
    return catalog;
  }

  // ----- Proyectos -----

  @GetMapping("/projects")
  public List<Map<String, Object>> listProjects() {
    return editor.listProjects().stream().map(ViewMapper::projectSummary).toList();
  }

  @PostMapping("/projects")
  public Map<String, Object> createProject(@RequestParam(required = false) String name) {
    return ViewMapper.project(editor.createProject(required("name", name)));
  }

  @GetMapping("/projects/{p}")
  public Map<String, Object> project(@PathVariable String p) {
    return ViewMapper.project(editor.project(p));
  }

  @PostMapping("/projects/{p}/rename")
  public Map<String, Object> renameProject(@PathVariable String p, @RequestParam(required = false) String name) {
    return ViewMapper.project(editor.renameProject(p, required("name", name)));
  }

  @PostMapping("/projects/{p}/delete")
  public Map<String, Object> deleteProject(@PathVariable String p) {
    editor.deleteProject(p);
    return Map.of("ok", true);
  }

  // ----- Escenas -----

  @PostMapping("/projects/{p}/scenes")
  public Map<String, Object> createScene(@PathVariable String p,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(defaultValue = "960") double width,
                                         @RequestParam(defaultValue = "540") double height,
                                         @RequestParam(defaultValue = "1400") double gravity,
                                         @RequestParam(defaultValue = "#87ceeb") String background) {
    return ViewMapper.scene(editor.createScene(p, required("name", name), (int) width, (int) height, gravity,
        background.strip()));
  }

  @GetMapping("/projects/{p}/scenes/{s}")
  public Map<String, Object> scene(@PathVariable String p, @PathVariable String s) {
    return ViewMapper.scene(editor.scene(p, s));
  }

  @PostMapping("/projects/{p}/scenes/{s}/update")
  public Map<String, Object> updateScene(@PathVariable String p, @PathVariable String s,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(defaultValue = "1400") double gravity,
                                         @RequestParam(defaultValue = "#87ceeb") String background) {
    return ViewMapper.scene(editor.updateScene(p, s, optional(name), gravity, background.strip()));
  }

  @PostMapping("/projects/{p}/scenes/{s}/delete")
  public Map<String, Object> deleteScene(@PathVariable String p, @PathVariable String s) {
    editor.deleteScene(p, s);
    return Map.of("ok", true);
  }

  // ----- Objetos -----

  @PostMapping("/projects/{p}/scenes/{s}/objects")
  public Map<String, Object> createObject(@PathVariable String p, @PathVariable String s,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(defaultValue = "empty") String preset,
                                          @RequestParam(defaultValue = "0") double x,
                                          @RequestParam(defaultValue = "0") double y,
                                          @RequestParam(defaultValue = "48") double width,
                                          @RequestParam(defaultValue = "48") double height) {
    return ViewMapper.object(editor.createObject(p, s, required("name", name), preset.strip(), x, y, width, height));
  }

  @PostMapping("/projects/{p}/scenes/{s}/objects/{o}/update")
  public Map<String, Object> updateObject(@PathVariable String p, @PathVariable String s, @PathVariable String o,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(defaultValue = "0") double x,
                                          @RequestParam(defaultValue = "0") double y,
                                          @RequestParam(defaultValue = "48") double width,
                                          @RequestParam(defaultValue = "48") double height) {
    return ViewMapper.object(editor.updateObject(p, s, o, optional(name), x, y, width, height));
  }

  @PostMapping("/projects/{p}/scenes/{s}/objects/{o}/delete")
  public Map<String, Object> deleteObject(@PathVariable String p, @PathVariable String s, @PathVariable String o) {
    editor.deleteObject(p, s, o);
    return Map.of("ok", true);
  }

  /** Recibe el tipo y todos los parámetros del formulario (color, speed, points...). */
  @PostMapping("/projects/{p}/scenes/{s}/objects/{o}/components")
  public Map<String, Object> addComponent(@PathVariable String p, @PathVariable String s, @PathVariable String o,
                                          @RequestParam Map<String, String> params) {
    return ViewMapper.object(editor.addComponent(p, s, o, required("type", params.get("type")), params));
  }

  @PostMapping("/projects/{p}/scenes/{s}/objects/{o}/components/{type}/delete")
  public Map<String, Object> removeComponent(@PathVariable String p, @PathVariable String s, @PathVariable String o,
                                             @PathVariable String type) {
    return ViewMapper.object(editor.removeComponent(p, s, o, type));
  }

  // ----- Ayudas para leer el formulario -----

  private static String required(String field, String value) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta el campo " + field);
    return value.strip();
  }

  private static String optional(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
