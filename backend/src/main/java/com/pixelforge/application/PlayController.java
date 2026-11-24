package com.pixelforge.application;

import com.pixelforge.domain.Scene;
import com.pixelforge.domain.component.InputState;
import com.pixelforge.runtime.GameSession;
import com.pixelforge.runtime.PhysicsSystem;
import com.pixelforge.runtime.SessionRegistry;

/**
 * GRASP Controlador (de caso de uso "Jugar escena"): recibe los eventos del modo juego (empezar
 * partida y avanzar un cuadro con las teclas) y los delega en GameSession.
 *
 * <p>GRASP Creador: crea cada GameSession porque tiene los datos para inicializarla (la escena) y
 * la registra en el SessionRegistry.
 */
public class PlayController {
  private final EditorController editor;
  private final SessionRegistry sessions;
  private final PhysicsSystem physics;

  public PlayController(EditorController editor, SessionRegistry sessions, PhysicsSystem physics) {
    this.editor = editor;
    this.sessions = sessions;
    this.physics = physics;
  }

  public GameSession start(String projectId, String sceneId) {
    Scene scene = editor.scene(projectId, sceneId);
    GameSession session = new GameSession(scene, physics);
    sessions.add(session);
    return session;
  }

  public GameSession step(String sessionId, InputState input) {
    GameSession session = session(sessionId);
    session.step(input); // Experto: la sesión sabe avanzar su propia partida
    return session;
  }

  public GameSession session(String sessionId) {
    return sessions.find(sessionId).orElseThrow(() -> new NotFoundException("La partida terminó o no existe"));
  }
}
