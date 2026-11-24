package com.pixelforge.persistence;

import com.pixelforge.domain.Project;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Implementación en memoria: sirve para las pruebas (GRASP Polimorfismo sobre ProjectRepository). */
public class InMemoryProjectRepository implements ProjectRepository {
  protected final Map<String, Project> projects = new LinkedHashMap<>();

  @Override
  public synchronized List<Project> findAll() {
    return new ArrayList<>(projects.values());
  }

  @Override
  public synchronized Optional<Project> findById(String id) {
    return Optional.ofNullable(projects.get(id));
  }

  @Override
  public synchronized void save(Project project) {
    projects.put(project.id(), project);
  }

  @Override
  public synchronized boolean delete(String id) {
    return projects.remove(id) != null;
  }
}
