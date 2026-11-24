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
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Punto de entrada: arranca Spring Boot y arma el sistema.
 *
 * <p>Las clases del dominio, del juego y los controladores GRASP no llevan anotaciones de Spring;
 * se registran aquí con métodos {@code @Bean}. Así el núcleo no depende del framework (GRASP Bajo
 * acoplamiento) y este es el único lugar que decide qué implementación usar.
 */
@SpringBootApplication
public class PixelForgeApplication {

  public static void main(String[] args) {
    SpringApplication.run(PixelForgeApplication.class, args);
  }

  @Bean
  ComponentFactory componentFactory() {
    return new ComponentFactory();
  }

  /**
   * GRASP Variaciones protegidas: el resto del sistema solo conoce ProjectRepository; aquí se elige
   * la implementación concreta según la propiedad pixelforge.storage.
   */
  @Bean
  ProjectRepository projectRepository(@Value("${pixelforge.storage}") String storage, MongoTemplate mongo,
                                      ComponentFactory components) {
    return switch (storage) {
      case "mongo" -> new MongoProjectRepository(mongo.getDb(), new ProjectDocumentMapper(components));
      case "file" -> new FileProjectRepository(Path.of("data", "pixelforge.dat"));
      default -> throw new IllegalArgumentException("Almacenamiento desconocido: " + storage + " (usa mongo o file)");
    };
  }

  @Bean
  EditorController editorController(ProjectRepository repository, ComponentFactory components) {
    return new EditorController(repository, components);
  }

  @Bean
  PlayController playController(EditorController editor) {
    return new PlayController(editor, new SessionRegistry(), new PhysicsSystem());
  }

  /** Al arrancar, crea el proyecto de ejemplo si no hay ninguno, para que el editor no esté vacío. */
  @Bean
  ApplicationRunner demoProject(EditorController editor) {
    return args -> {
      if (editor.listProjects().isEmpty()) createDemo(editor);
    };
  }

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
