package com.pixelforge.persistence;

import com.pixelforge.domain.Project;
import java.util.List;
import java.util.Optional;

/**
 * Dónde se guardan los proyectos.
 *
 * <p>GRASP Indirección: los controladores hablan con esta interfaz, no con archivos.
 * <p>GRASP Variaciones protegidas: si mañana se guarda en una base de datos, se crea otra
 * implementación y el resto del código no cambia.
 */
public interface ProjectRepository {
  List<Project> findAll();

  Optional<Project> findById(String id);

  void save(Project project);

  boolean delete(String id);
}
