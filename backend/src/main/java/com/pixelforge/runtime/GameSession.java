package com.pixelforge.runtime;

import com.pixelforge.domain.GameObject;
import com.pixelforge.domain.Scene;
import com.pixelforge.domain.component.Collectible;
import com.pixelforge.domain.component.Component;
import com.pixelforge.domain.component.FrameContext;
import com.pixelforge.domain.component.Hazard;
import com.pixelforge.domain.component.InputState;
import com.pixelforge.domain.component.PlayerController;
import com.pixelforge.domain.component.RigidBody;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Una partida en curso de una escena. Trabaja sobre copias de los objetos, así que jugar nunca
 * cambia lo que se guardó en el editor.
 *
 * <p>GRASP Experto en información: la sesión tiene los objetos de la partida, el puntaje y el
 * estado, así que es ella la que avanza el juego un cuadro ({@link #step}) y decide si se gana o
 * se pierde.
 *
 * <p>GRASP Polimorfismo: en cada cuadro llama update() en todos los componentes sin preguntar su
 * tipo.
 */
public class GameSession {
  public static final double DT = 1.0 / 30; // 30 cuadros por segundo

  private final String id = UUID.randomUUID().toString();
  private final String sceneName;
  private final int width;
  private final int height;
  private final double gravity;
  private final String background;
  private final List<GameObject> objects;
  private final PhysicsSystem physics;
  private final int totalCollectibles;
  private int score;
  private double elapsed;
  private GameStatus status = GameStatus.RUNNING;

  /** La sesión se arma con una copia de la escena; necesita exactamente un jugador. */
  public GameSession(Scene scene, PhysicsSystem physics) {
    this.sceneName = scene.name();
    this.width = scene.width();
    this.height = scene.height();
    this.gravity = scene.gravity();
    this.background = scene.background();
    this.objects = new ArrayList<>(scene.copyObjects());
    this.physics = physics;
    long players = objects.stream().filter(object -> object.has(PlayerController.class)).count();
    if (players != 1)
      throw new IllegalArgumentException("La escena necesita exactamente un objeto con PlayerController para jugar");
    if (!player().has(RigidBody.class))
      throw new IllegalArgumentException("El jugador necesita un RigidBody para moverse");
    this.totalCollectibles = (int) objects.stream().filter(object -> object.has(Collectible.class)).count();
  }

  /** Avanza la partida un cuadro con las teclas recibidas. */
  public synchronized void step(InputState input) {
    if (status != GameStatus.RUNNING) return;
    FrameContext frame = new FrameContext(input, DT, gravity);

    // 1. Cada componente se actualiza a su manera (polimorfismo).
    for (GameObject object : objects)
      for (Component component : object.components()) component.update(object, frame);

    // 2. La física mueve los cuerpos (delegación a la fabricación pura).
    for (GameObject object : objects)
      if (object.has(RigidBody.class)) physics.move(object, objects, DT);

    // 3. Reglas del juego: monedas, peligros y caídas.
    applyRules();
    elapsed += DT;
  }

  private void applyRules() {
    GameObject player = player();
    List<GameObject> collected = new ArrayList<>();
    for (GameObject other : objects) {
      if (other == player || !player.overlaps(other)) continue;
      other.getComponent(Collectible.class).ifPresent(coin -> {
        score += coin.points();
        collected.add(other);
      });
      if (other.has(Hazard.class)) status = GameStatus.LOST;
    }
    objects.removeAll(collected);
    if (player.top() > height) status = GameStatus.LOST;
    else if (status == GameStatus.RUNNING && totalCollectibles > 0 && remainingCollectibles() == 0)
      status = GameStatus.WON;
  }

  public GameObject player() {
    return objects.stream().filter(object -> object.has(PlayerController.class)).findFirst().orElseThrow();
  }

  public int remainingCollectibles() {
    return (int) objects.stream().filter(object -> object.has(Collectible.class)).count();
  }

  public String id() {
    return id;
  }

  public String sceneName() {
    return sceneName;
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public String background() {
    return background;
  }

  public int score() {
    return score;
  }

  public double elapsed() {
    return elapsed;
  }

  public GameStatus status() {
    return status;
  }

  public List<GameObject> objects() {
    return Collections.unmodifiableList(objects);
  }
}
