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
import com.pixelforge.web.Json;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Pruebas del motor sin librerías externas: cada método lanza un error si algo falla.
 * Se ejecuta con: java -cp out com.pixelforge.EngineTests
 */
public class EngineTests {
  private static int passed;

  public static void main(String[] args) throws Exception {
    run("La escena crea objetos solo dentro de sus límites", EngineTests::sceneCreatesObjectsInsideBounds);
    run("Dos objetos que se tocan se superponen", EngineTests::objectsKnowWhenTheyOverlap);
    run("El jugador cae y queda sobre el suelo", EngineTests::playerLandsOnTheFloor);
    run("Solo se salta estando en el suelo", EngineTests::jumpOnlyFromTheGround);
    run("Recoger todas las monedas gana la partida", EngineTests::collectingEveryCoinWins);
    run("Tocar un peligro pierde la partida", EngineTests::touchingAHazardLoses);
    run("Caer fuera de la escena pierde la partida", EngineTests::fallingOffLoses);
    run("Jugar no cambia la escena del editor", EngineTests::playingDoesNotChangeTheScene);
    run("Una escena sin jugador no se puede jugar", EngineTests::sceneWithoutPlayerCannotStart);
    run("El repositorio de archivos conserva los proyectos", EngineTests::fileRepositoryKeepsProjects);
    run("Un proyecto pasa a documento de MongoDB y vuelve igual", EngineTests::mongoDocumentRoundTrip);
    run("Los errores de búsqueda son NotFoundException", EngineTests::missingThingsAreNotFound);
    run("La fábrica rechaza tipos desconocidos", EngineTests::factoryRejectsUnknownTypes);
    run("El JSON escapa comillas y saltos de línea", EngineTests::jsonEscapesText);
    System.out.println(passed + " pruebas pasaron");
  }

  interface Check {
    void run() throws Exception;
  }

  private static void run(String name, Check check) throws Exception {
    check.run();
    passed++;
    System.out.println("OK  " + name);
  }

  private static void expect(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static void expectThrows(Class<? extends Throwable> kind, Check check) throws Exception {
    try {
      check.run();
    } catch (Throwable error) {
      if (kind.isInstance(error)) return;
      throw new AssertionError("Se esperaba " + kind.getSimpleName() + " y llegó " + error);
    }
    throw new AssertionError("Se esperaba " + kind.getSimpleName());
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

  static void sceneCreatesObjectsInsideBounds() throws Exception {
    Project project = new Project("p", "Juego");
    Scene scene = project.createScene("Nivel", 320, 240, 1000, "#000000");
    GameObject box = scene.createObject("Caja", new Vector2(10, 10), new Vector2(20, 20));
    expect(scene.findObject(box.id()).isPresent(), "La escena debe encontrar el objeto que creó");
    expectThrows(IllegalArgumentException.class, () -> scene.createObject("Fuera", new Vector2(310, 10), new Vector2(20, 20)));
  }

  static void objectsKnowWhenTheyOverlap() {
    Scene scene = new Project("p", "Juego").createScene("Nivel", 320, 240, 1000, "#000000");
    GameObject a = scene.createObject("A", new Vector2(0, 0), new Vector2(20, 20));
    GameObject b = scene.createObject("B", new Vector2(10, 10), new Vector2(20, 20));
    GameObject c = scene.createObject("C", new Vector2(100, 100), new Vector2(20, 20));
    expect(a.overlaps(b), "A y B se tocan");
    expect(!a.overlaps(c), "A y C están lejos");
  }

  static void playerLandsOnTheFloor() {
    EditorController editor = editor();
    GameSession session = play(editor, level(editor));
    frames(session, 60, InputState.NONE);
    GameObject player = session.player();
    expect(Math.abs(player.bottom() - 320) < 0.01, "El jugador debe quedar sobre el suelo (y=320), quedó en " + player.bottom());
    expect(player.getComponent(RigidBody.class).orElseThrow().grounded(), "Debe estar en el suelo");
  }

  static void jumpOnlyFromTheGround() {
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

  static void collectingEveryCoinWins() {
    EditorController editor = editor();
    String[] ids = level(editor);
    editor.createObject(ids[0], ids[1], "Moneda", "coin", 120, 280, 24, 24);
    GameSession session = play(editor, ids);
    frames(session, 90, new InputState(false, true, false));
    expect(session.score() == 10, "Debe sumar 10 puntos, tiene " + session.score());
    expect(session.status() == GameStatus.WON, "Debe ganar al no quedar monedas");
  }

  static void touchingAHazardLoses() {
    EditorController editor = editor();
    String[] ids = level(editor);
    editor.createObject(ids[0], ids[1], "Pinchos", "spikes", 120, 300, 64, 20);
    editor.createObject(ids[0], ids[1], "Moneda", "coin", 500, 280, 24, 24);
    GameSession session = play(editor, ids);
    frames(session, 90, new InputState(false, true, false));
    expect(session.status() == GameStatus.LOST, "Debe perder al tocar los pinchos");
  }

  static void fallingOffLoses() {
    EditorController editor = editor();
    Project project = editor.createProject("Vacío");
    Scene scene = editor.createScene(project.id(), "Sin suelo", 640, 360, 1400, "#87ceeb");
    editor.createObject(project.id(), scene.id(), "Jugador", "player", 40, 40, 32, 48);
    GameSession session = play(editor, new String[] {project.id(), scene.id()});
    frames(session, 120, InputState.NONE);
    expect(session.status() == GameStatus.LOST, "Debe perder al caer fuera de la escena");
  }

  static void playingDoesNotChangeTheScene() {
    EditorController editor = editor();
    String[] ids = level(editor);
    GameSession session = play(editor, ids);
    frames(session, 30, new InputState(false, true, false));
    GameObject saved = editor.scene(ids[0], ids[1]).objects().get(1);
    expect(saved.position().equals(new Vector2(40, 200)), "La escena guardada no debe moverse: " + saved.position());
  }

  static void sceneWithoutPlayerCannotStart() throws Exception {
    EditorController editor = editor();
    Project project = editor.createProject("Sin jugador");
    Scene scene = editor.createScene(project.id(), "Nivel", 640, 360, 1400, "#87ceeb");
    expectThrows(IllegalArgumentException.class, () -> play(editor, new String[] {project.id(), scene.id()}));
  }

  static void fileRepositoryKeepsProjects() throws Exception {
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

  static void mongoDocumentRoundTrip() {
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

  static void missingThingsAreNotFound() throws Exception {
    EditorController editor = editor();
    expectThrows(NotFoundException.class, () -> editor.project("no-existe"));
    String[] ids = level(editor);
    expectThrows(NotFoundException.class, () -> editor.deleteObject(ids[0], ids[1], "no-existe"));
  }

  static void factoryRejectsUnknownTypes() throws Exception {
    ComponentFactory factory = new ComponentFactory();
    expectThrows(IllegalArgumentException.class, () -> factory.create("Volar", Map.of()));
    expect(factory.create("Collectible", Map.of("points", "25")).properties().get("points").equals(25), "Usa los puntos dados");
    expect(factory.preset("player").size() == 4, "El jugador nace con 4 componentes");
  }

  static void jsonEscapesText() {
    String json = Json.write(Map.of("text", "dice \"hola\"\nadiós", "list", List.of(1, 2.5, true)));
    expect(json.contains("\\\"hola\\\"") && json.contains("\\n"), "Debe escapar: " + json);
    expect(json.contains("[1,2.5,true]"), "Debe escribir la lista: " + json);
  }
}
