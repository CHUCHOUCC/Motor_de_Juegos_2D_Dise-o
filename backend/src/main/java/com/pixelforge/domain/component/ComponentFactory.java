package com.pixelforge.domain.component;

import java.util.List;
import java.util.Map;

/**
 * Crea componentes a partir del nombre del tipo y de los parámetros que llegan del editor.
 *
 * <p>GRASP Fabricación pura: no representa nada del mundo del juego; existe para que la lógica de
 * "qué clase crear y con qué valores por defecto" no quede repartida en el controlador ni en la web.
 *
 * <p>GRASP Variaciones protegidas: el resto del sistema solo conoce el nombre del tipo; si se agrega
 * un componente nuevo, solo cambia esta clase.
 */
public class ComponentFactory {
  public static final List<String> TYPES =
      List.of("Sprite", "RigidBody", "BoxCollider", "PlayerController", "Patrol", "Collectible", "Hazard");
  public static final List<String> PRESETS = List.of("player", "platform", "coin", "enemy", "spikes", "empty");

  public Component create(String type, Map<String, String> params) {
    return switch (type) {
      case "Sprite" -> new Sprite(text(params, "color", "#4c8bf5"),
          Sprite.Shape.valueOf(text(params, "shape", "RECT").toUpperCase()));
      case "RigidBody" -> new RigidBody(number(params, "gravityScale", 1));
      case "BoxCollider" -> new BoxCollider();
      case "PlayerController" -> new PlayerController(number(params, "speed", 260), number(params, "jumpSpeed", 620));
      case "Patrol" -> new Patrol(number(params, "distance", 160), number(params, "speed", 90));
      case "Collectible" -> new Collectible((int) number(params, "points", 10));
      case "Hazard" -> new Hazard();
      default -> throw new IllegalArgumentException("Tipo de componente desconocido: " + type);
    };
  }

  /** Componentes con los que nace un objeto según la plantilla elegida en el editor. */
  public List<Component> preset(String preset) {
    return switch (preset) {
      case "player" -> List.of(new Sprite("#4c8bf5", Sprite.Shape.RECT), new RigidBody(1),
          new BoxCollider(), new PlayerController(260, 620));
      case "platform" -> List.of(new Sprite("#6b4f3a", Sprite.Shape.RECT), new BoxCollider());
      case "coin" -> List.of(new Sprite("#f5c542", Sprite.Shape.CIRCLE), new Collectible(10));
      case "enemy" -> List.of(new Sprite("#d64545", Sprite.Shape.RECT), new Patrol(160, 90), new Hazard());
      case "spikes" -> List.of(new Sprite("#8a8f98", Sprite.Shape.RECT), new Hazard());
      case "empty" -> List.of(new Sprite("#9aa5b1", Sprite.Shape.RECT));
      default -> throw new IllegalArgumentException("Plantilla desconocida: " + preset);
    };
  }

  private static String text(Map<String, String> params, String key, String fallback) {
    String value = params.get(key);
    return value == null || value.isBlank() ? fallback : value.strip();
  }

  private static double number(Map<String, String> params, String key, double fallback) {
    String value = params.get(key);
    if (value == null || value.isBlank()) return fallback;
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("El valor de " + key + " no es un número");
    }
  }
}
