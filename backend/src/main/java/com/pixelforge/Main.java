package com.pixelforge;

import com.pixelforge.application.EditorController;
import com.pixelforge.application.PlayController;
import com.pixelforge.domain.Project;
import com.pixelforge.domain.Scene;
import com.pixelforge.domain.component.ComponentFactory;
import com.pixelforge.persistence.FileProjectRepository;
import com.pixelforge.persistence.MongoProjectRepository;
import com.pixelforge.persistence.ProjectDocumentMapper;
import com.pixelforge.persistence.ProjectRepository;
import com.pixelforge.runtime.PhysicsSystem;
import com.pixelforge.runtime.SessionRegistry;
import com.pixelforge.web.ApiHandler;
import com.pixelforge.web.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Punto de arranque: arma los objetos y los conecta (quién usa a quién) y abre el servidor web.
 * Es el único lugar que conoce las clases concretas; el resto recibe sus dependencias por
 * constructor (Bajo acoplamiento).
 *
 * <p>Uso: java com.pixelforge.Main [puerto] [mongo|file]. Por defecto usa MongoDB en
 * mongodb://localhost:27017 (se puede cambiar con la variable de entorno MONGODB_URI).
 */
public class Main {
  public static void main(String[] args) throws IOException {
    int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
    String storage = args.length > 1 ? args[1] : "mongo";
    ComponentFactory components = new ComponentFactory();

    ProjectRepository repository = createRepository(storage, components);
    EditorController editor = new EditorController(repository, components);
    PlayController play = new PlayController(editor, new SessionRegistry(), new PhysicsSystem());
    if (editor.listProjects().isEmpty()) createDemo(editor);

    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
    server.createContext("/api", new ApiHandler(editor, play));
    server.createContext("/", new StaticFileHandler(Path.of("..", "frontend")));
    server.setExecutor(Executors.newFixedThreadPool(8));
    server.start();
    System.out.println("PixelForge listo en http://localhost:" + port + "  (almacenamiento: " + storage + ")");
  }

  /**
   * GRASP Variaciones protegidas: el resto del sistema solo conoce ProjectRepository; aquí se elige
   * la implementación concreta.
   */
  static ProjectRepository createRepository(String storage, ComponentFactory components) {
    return switch (storage) {
      case "mongo" -> {
        String uri = System.getenv().getOrDefault("MONGODB_URI", "mongodb://localhost:27017/?serverSelectionTimeoutMS=4000");
        yield new MongoProjectRepository(uri, "pixelforge", new ProjectDocumentMapper(components));
      }
      case "file" -> new FileProjectRepository(Path.of("data", "pixelforge.dat"));
      default -> throw new IllegalArgumentException("Almacenamiento desconocido: " + storage + " (usa mongo o file)");
    };
  }

  /** Proyecto de ejemplo para que el editor no arranque vacío. */
  static void createDemo(EditorController editor) {
    Project project = editor.createProject("Demo de plataformas");
    Scene scene = editor.createScene(project.id(), "Nivel 1", 960, 540, 1400, "#87ceeb");
    String p = project.id(), s = scene.id();
    editor.createObject(p, s, "Suelo", "platform", 0, 500, 960, 40);
    editor.createObject(p, s, "Plataforma", "platform", 300, 380, 200, 24);
    editor.createObject(p, s, "Plataforma alta", "platform", 600, 280, 180, 24);
    editor.createObject(p, s, "Jugador", "player", 60, 440, 32, 48);
    editor.createObject(p, s, "Moneda 1", "coin", 390, 340, 24, 24);
    editor.createObject(p, s, "Moneda 2", "coin", 680, 240, 24, 24);
    editor.createObject(p, s, "Moneda 3", "coin", 880, 460, 24, 24);
    editor.createObject(p, s, "Enemigo", "enemy", 520, 468, 32, 32);
  }
}
