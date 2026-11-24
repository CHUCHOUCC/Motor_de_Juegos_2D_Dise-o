package com.pixelforge.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Entrega la página HTML del editor (carpeta frontend/ del repositorio). Solo sirve archivos dentro de esa carpeta. */
public class StaticFileHandler implements HttpHandler {
  private final Path root;

  public StaticFileHandler(Path root) {
    this.root = root.toAbsolutePath().normalize();
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String requested = exchange.getRequestURI().getPath();
    if (requested.equals("/")) requested = "/index.html";
    Path file = root.resolve(requested.substring(1)).normalize();
    // Impide salir de la carpeta frontend con rutas como /../secreto.txt
    if (!file.startsWith(root) || !Files.isRegularFile(file)) {
      byte[] missing = "No encontrado".getBytes();
      exchange.sendResponseHeaders(404, missing.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(missing);
      }
      return;
    }
    String type = file.toString().endsWith(".html") ? "text/html; charset=utf-8" : "application/octet-stream";
    byte[] bytes = Files.readAllBytes(file);
    exchange.getResponseHeaders().set("Content-Type", type);
    exchange.sendResponseHeaders(200, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }
}
