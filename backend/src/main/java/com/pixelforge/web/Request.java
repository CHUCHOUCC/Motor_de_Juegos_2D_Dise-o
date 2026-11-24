package com.pixelforge.web;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Lee una petición HTTP: método, partes de la ruta y parámetros (de la URL y del formulario).
 * El frontend envía formularios "application/x-www-form-urlencoded", así no hace falta JSON de entrada.
 */
public class Request {
  private static final int MAX_BODY = 64 * 1024;

  public final String method;
  public final String[] path;
  private final Map<String, String> params = new HashMap<>();

  public Request(HttpExchange exchange) throws IOException {
    this.method = exchange.getRequestMethod();
    String raw = exchange.getRequestURI().getPath();
    this.path = raw.replaceAll("^/+|/+$", "").split("/");
    parse(exchange.getRequestURI().getRawQuery());
    byte[] body = exchange.getRequestBody().readNBytes(MAX_BODY + 1);
    if (body.length > MAX_BODY) throw new IllegalArgumentException("La petición es demasiado grande");
    parse(new String(body, StandardCharsets.UTF_8));
  }

  private void parse(String encoded) {
    if (encoded == null || encoded.isBlank()) return;
    for (String pair : encoded.split("&")) {
      int equals = pair.indexOf('=');
      String key = URLDecoder.decode(equals < 0 ? pair : pair.substring(0, equals), StandardCharsets.UTF_8);
      String value = equals < 0 ? "" : URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8);
      params.put(key, value);
    }
  }

  public Map<String, String> params() {
    return params;
  }

  public String text(String key) {
    String value = params.get(key);
    if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta el campo " + key);
    return value.strip();
  }

  public String text(String key, String fallback) {
    String value = params.get(key);
    return value == null || value.isBlank() ? fallback : value.strip();
  }

  public double number(String key, double fallback) {
    String value = params.get(key);
    if (value == null || value.isBlank()) return fallback;
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("El campo " + key + " debe ser un número");
    }
  }

  public boolean flag(String key) {
    return "true".equals(params.get(key)) || "1".equals(params.get(key));
  }

  /** Compara la ruta con un patrón como "api/projects/*\/scenes"; "*" acepta cualquier valor. */
  public boolean matches(String verb, String pattern) {
    if (!method.equals(verb)) return false;
    String[] parts = pattern.split("/");
    if (parts.length != path.length) return false;
    for (int i = 0; i < parts.length; i++)
      if (!parts[i].equals("*") && !parts[i].equals(path[i])) return false;
    return true;
  }
}
