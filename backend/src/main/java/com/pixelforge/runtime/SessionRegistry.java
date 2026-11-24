package com.pixelforge.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Guarda en memoria las partidas abiertas para poder seguirlas por su id.
 *
 * <p>GRASP Fabricación pura: no es parte del juego; existe para no cargarle esta tarea al
 * controlador. Limita cuántas partidas guarda para no llenar la memoria.
 */
public class SessionRegistry {
  private static final int MAX_SESSIONS = 50;
  private final Map<String, GameSession> sessions = new LinkedHashMap<>();

  public synchronized void add(GameSession session) {
    if (sessions.size() >= MAX_SESSIONS) {
      String oldest = sessions.keySet().iterator().next();
      sessions.remove(oldest);
    }
    sessions.put(session.id(), session);
  }

  public synchronized Optional<GameSession> find(String id) {
    return Optional.ofNullable(sessions.get(id));
  }
}
