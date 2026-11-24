package com.pixelforge.web;

import com.pixelforge.application.PlayController;
import com.pixelforge.domain.component.InputState;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del modo juego: empezar una partida y avanzarla un cuadro con las teclas.
 *
 * <p>El navegador no calcula nada del juego: envía las teclas y dibuja el estado que devuelve.
 */
@RestController
@RequestMapping("/api")
public class PlayApiController {
  private final PlayController play;

  public PlayApiController(PlayController play) {
    this.play = play;
  }

  @PostMapping("/projects/{p}/scenes/{s}/play")
  public Map<String, Object> start(@PathVariable String p, @PathVariable String s) {
    return ViewMapper.session(play.start(p, s));
  }

  @PostMapping("/sessions/{id}/step")
  public Map<String, Object> step(@PathVariable String id,
                                  @RequestParam(defaultValue = "false") String left,
                                  @RequestParam(defaultValue = "false") String right,
                                  @RequestParam(defaultValue = "false") String jump) {
    return ViewMapper.session(play.step(id, new InputState(pressed(left), pressed(right), pressed(jump))));
  }

  private static boolean pressed(String value) {
    return "true".equals(value) || "1".equals(value);
  }
}
