package com.pixelforge.domain.component;

/**
 * Lo que un componente necesita saber para actualizarse en un cuadro: las teclas, cuánto tiempo
 * pasó (dt, en segundos) y la gravedad de la escena. Pasar este objeto en vez de la sesión entera
 * mantiene bajo el acoplamiento (GRASP Bajo acoplamiento): los componentes no conocen GameSession.
 */
public record FrameContext(InputState input, double dt, double gravity) {}
