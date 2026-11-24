package com.pixelforge;

import com.pixelforge.application.EditorController;
import com.pixelforge.application.NotFoundException;
import com.pixelforge.application.PlayController;
import com.pixelforge.domain.GameObject;
import com.pixelforge.domain.Project;
import com.pixelforge.domain.Scene;
import com.pixelforge.domain.Vector2;
import com.pixelforge.domain.component.ComponentFactory;
import com.pixelforge.domain.component.InputState;
import com.pixelforge.domain.component.RigidBody;
import com.pixelforge.persistence.FileProjectRepository;
import com.pixelforge.persistence.InMemoryProjectRepository;
import com.pixelforge.persistence.ProjectDocumentMapper;
import org.bson.Document;
import com.pixelforge.runtime.GameSession;
import com.pixelforge.runtime.GameStatus;
import com.pixelforge.runtime.PhysicsSystem;
import com.pixelforge.runtime.SessionRegistry;
import com.pixelforge.web.ProjectApiController;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del motor con JUnit 5. Se ejecutan con: .\mvnw.cmd test (no necesitan MongoDB encendido).
 */
class EngineTests {

  private static void expect(boolean condition, String message) {
    assertTrue(condition, message);
  }

  private static void expectThrows(Class<? extends Throwable> kind, Executable check) {
    assertThrows(kind, check);
  }

  // ---------- Ayudas ----------

  private static EditorController editor() {
    return new EditorController(new InMemoryProjectRepository(), new ComponentFactory());
  }

  /** Escena con suelo y jugador parado encima; devuelve [proyecto, escena]. */
  private static String[] level(EditorController editor) {
    Project project = editor.createProject("Prueba");
    Scene scene = editor.createScene(project.id(), "Nivel", 640, 360, 1400, "#87ceeb");
    editor.createObject(project.id(), scene.id(), "Suelo", "platform", 0, 320, 640, 40);
    editor.createObject(project.id(), scene.id(), "Jugador", "player", 40, 200, 32, 48);
    return new String[] {project.id(), scene.id()};
  }

  private static GameSession play(EditorController editor, String[] ids) {
    return new PlayController(editor, new SessionRegistry(), new PhysicsSystem()).start(ids[0], ids[1]);
  }

  private static void frames(GameSession session, int count, InputState input) {
    for (int i = 0; i < count; i++) session.step(input);
  }

  // ---------- Pruebas ----------

  @Test
  @DisplayName("La escena crea objetos solo dentro de sus límites")
  void sceneCreatesObjectsInsideBounds() {
    Project project = new Project("p", "Juego");
    Scene scene = project.createScene("Nivel", 320, 240, 1000, "#000000");
    GameObject box = scene.createObject("Caja", new Vector2(10, 10), new Vector2(20, 20));
    expect(scene.findObject(box.id()).isPresent(), "La escena debe encontrar el objeto que creó");
    expectThrows(IllegalArgumentException.class, () -> scene.createObject("Fuera", new Vector2(310, 10), new Vector2(20, 20)));
  }

  @Test
  @DisplayName("Dos objetos que se tocan se superponen")
  void objectsKnowWhenTheyOverlap() {
    Scene scene = new Project("p", "Juego").createScene("Nivel", 320, 240, 1000, "#000000");
    GameObject a = scene.createObject("A", new Vector2(0, 0), new Vector2(20, 20));
    GameObject b = scene.createObject("B", new Vector2(10, 10), new Vector2(20, 20));
    GameObject c = scene.createObject("C", new Vector2(100, 100), new Vector2(20, 20));
    expect(a.overlaps(b), "A y B se tocan");
    expect(!a.overlaps(c), "A y C están lejos");
  }

  @Test
  @DisplayName("El jugador cae y queda sobre el suelo")
  void playerLandsOnTheFloor() {
    EditorController editor = editor();
    GameSession session = play(editor, level(editor));
    frames(session, 60, InputState.NONE);
    GameObject player = session.player();
    expect(Math.abs(player.bottom() - 320) < 0.01, "El jugador debe quedar sobre el suelo (y=320), quedó en " + player.bottom());
    expect(player.getComponent(RigidBody.class).orElseThrow().grounded(), "Debe estar en el suelo");
  }

  @Test
  @DisplayName("Solo se salta estando en el suelo")
  void jumpOnlyFromTheGround() {
    EditorController editor = editor();
    GameSession session = play(editor, level(editor));
    frames(session, 60, InputState.NONE);
    GameObject player = session.player();
    double floorY = player.top();
    session.step(new InputState(false, false, true));
    expect(player.top() < floorY, "Debe subir al saltar desde el suelo");
    double during = player.top();
    session.step(new InputState(false, false, true));
    double velocity = player.getComponent(RigidBody.class).orElseThrow().velocity().y();
    expect(velocity > -620, "No debe volver a impulsarse en el aire (velocidad " + velocity + ")");
    expect(player.top() < during + 50, "Sigue en el aire");
  }

  @Test
  @DisplayName("Recoger todas las monedas gana la partida")
  void collectingEveryCoinWins() {
    EditorController editor = editor();
    String[] ids = level(editor);
    editor.createObject(ids[0], ids[1], "Moneda", "coin", 120, 280, 24, 24);
    GameSession session = play(editor, ids);
    frames(session, 90, new InputState(false, true, false));
    expect(session.score() == 10, "Debe sumar 10 puntos, tiene " + session.score());
    expect(session.status() == GameStatus.WON, "Debe ganar al no quedar monedas");
  }

  @Test
  @DisplayName("Tocar un peligro pierde la partida")
  void touchingAHazardLoses() {
    EditorController editor = editor();
    String[] ids = level(editor);
    editor.createObject(ids[0], ids[1], "Pinchos", "spikes", 120, 300, 64, 20);
    editor.createObject(ids[0], ids[1], "Moneda", "coin", 500, 280, 24, 24);
    GameSession session = play(editor, ids);
    frames(session, 90, new InputState(false, true, false));
    expect(session.status() == GameStatus.LOST, "Debe perder al tocar los pinchos");
  }

  @Test
  @DisplayName("Caer fuera de la escena pierde la partida")
  void fallingOffLoses() {
    EditorController editor = editor();
    Project project = editor.createProject("Vacío");
    Scene scene = editor.createScene(project.id(), "Sin suelo", 640, 360, 1400, "#87ceeb");
    editor.createObject(project.id(), scene.id(), "Jugador", "player", 40, 40, 32, 48);
    GameSession session = play(editor, new String[] {project.id(), scene.id()});
    frames(session, 120, InputState.NONE);
    expect(session.status() == GameStatus.LOST, "Debe perder al caer fuera de la escena");
  }

  @Test
  @DisplayName("Jugar no cambia la escena del editor")
  void playingDoesNotChangeTheScene() {
    EditorController editor = editor();
    String[] ids = level(editor);
    GameSession session = play(editor, ids);
    frames(session, 30, new InputState(false, true, false));
    GameObject saved = editor.scene(ids[0], ids[1]).objects().get(1);
    expect(saved.position().equals(new Vector2(40, 200)), "La escena guardada no debe moverse: " + saved.position());
  }

  @Test
  @DisplayName("Una escena sin jugador no se puede jugar")
  void sceneWithoutPlayerCannotStart() {
    EditorController editor = editor();
    Project project = editor.createProject("Sin jugador");
    Scene scene = editor.createScene(project.id(), "Nivel", 640, 360, 1400, "#87ceeb");
    expectThrows(IllegalArgumentException.class, () -> play(editor, new String[] {project.id(), scene.id()}));
  }

  @Test
  @DisplayName("El repositorio de archivos conserva los proyectos")
  void fileRepositoryKeepsProjects() throws Exception {
    Path file = Files.createTempDirectory("pixelforge").resolve("data.dat");
    EditorController first = new EditorController(new FileProjectRepository(file), new ComponentFactory());
    String[] ids = level(first);
    first.addComponent(ids[0], ids[1], first.scene(ids[0], ids[1]).objects().get(0).id(), "Sprite",
        Map.of("color", "#123456", "shape", "RECT"));
    EditorController second = new EditorController(new FileProjectRepository(file), new ComponentFactory());
    Scene reloaded = second.scene(ids[0], ids[1]);
    expect(reloaded.objects().size() == 2, "Debe recuperar los dos objetos");
    expect(reloaded.objects().get(0).components().stream().anyMatch(c -> "#123456".equals(c.properties().get("color"))),
        "Debe recuperar el color cambiado");
  }

  @Test
  @DisplayName("Un proyecto pasa a documento de MongoDB y vuelve igual")
  void mongoDocumentRoundTrip() {
    EditorController editor = editor();
    String[] ids = level(editor);
    editor.createObject(ids[0], ids[1], "Enemigo", "enemy", 200, 288, 32, 32);
    Project original = editor.project(ids[0]);
    ProjectDocumentMapper mapper = new ProjectDocumentMapper(new ComponentFactory());

    Document document = mapper.toDocument(original);
    expect(document.get("_id").equals(original.id()), "El _id del documento es el id del proyecto");
    expect(((List<?>) document.get("scenes")).size() == 1, "Las escenas van anidadas en el mismo documento");

    Project copy = mapper.toProject(document);
    expect(copy.name().equals(original.name()) && copy.createdAt().equals(original.createdAt()), "Nombre y fecha iguales");
    Scene a = original.scenes().get(0), b = copy.scenes().get(0);
    expect(a.id().equals(b.id()) && a.gravity() == b.gravity() && a.background().equals(b.background()), "Escena igual");
    expect(a.objects().size() == b.objects().size(), "Mismos objetos");
    for (int i = 0; i < a.objects().size(); i++) {
      GameObject x = a.objects().get(i), y = b.objects().get(i);
      expect(x.id().equals(y.id()) && x.position().equals(y.position()) && x.size().equals(y.size()), "Objeto igual: " + x.name());
      expect(x.components().size() == y.components().size(), "Mismos componentes en " + x.name());
      for (int k = 0; k < x.components().size(); k++)
        expect(x.components().get(k).properties().equals(y.components().get(k).properties()),
            "Propiedades iguales en " + x.name() + "." + x.components().get(k).type());
    }
  }

  @Test
  @DisplayName("Los errores de búsqueda son NotFoundException")
  void missingThingsAreNotFound() {
    EditorController editor = editor();
    expectThrows(NotFoundException.class, () -> editor.project("no-existe"));
    String[] ids = level(editor);
    expectThrows(NotFoundException.class, () -> editor.deleteObject(ids[0], ids[1], "no-existe"));
  }

  @Test
  @DisplayName("La fábrica rechaza tipos desconocidos")
  void factoryRejectsUnknownTypes() {
    ComponentFactory factory = new ComponentFactory();
    expectThrows(IllegalArgumentException.class, () -> factory.create("Volar", Map.of()));
    expect(factory.create("Collectible", Map.of("points", "25")).properties().get("points").equals(25), "Usa los puntos dados");
    expect(factory.preset("player").size() == 4, "El jugador nace con 4 componentes");
  }

  @Test
  @DisplayName("La API devuelve vistas y rechaza campos vacíos")
  void apiReturnsViewsAndRejectsMissingFields() {
    EditorController editor = editor();
    ProjectApiController api = new ProjectApiController(editor);
    Map<String, Object> project = api.createProject("  Mi juego  ");
    expect("Mi juego".equals(project.get("name")), "Debe guardar el nombre sin espacios: " + project.get("name"));
    expect(api.listProjects().size() == 1, "La lista debe tener el proyecto creado");
    expectThrows(IllegalArgumentException.class, () -> api.createProject(" "));
  }
}
