package com.pixelforge.persistence;

import com.pixelforge.domain.Project;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;

/**
 * Guarda todos los proyectos en un archivo del disco, para que no se pierdan al apagar el servidor.
 *
 * <p>GRASP Fabricación pura: "repositorio de archivos" no es un concepto del juego; se crea para
 * que Project no tenga que saber escribir archivos (Project queda cohesivo).
 *
 * <p>Reusa la lógica de la versión en memoria y solo agrega leer al inicio y escribir en cada
 * cambio. Escribe primero un archivo temporal y luego lo reemplaza, para no dejarlo a medias.
 */
public class FileProjectRepository extends InMemoryProjectRepository {
  private final Path file;

  public FileProjectRepository(Path file) {
    this.file = file;
    load();
  }

  @Override
  public synchronized void save(Project project) {
    super.save(project);
    write();
  }

  @Override
  public synchronized boolean delete(String id) {
    boolean removed = super.delete(id);
    if (removed) write();
    return removed;
  }

  @SuppressWarnings("unchecked")
  private void load() {
    if (!Files.exists(file)) return;
    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
      projects.putAll((LinkedHashMap<String, Project>) in.readObject());
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalStateException("No se pudo leer " + file + ": " + e.getMessage(), e);
    }
  }

  private void write() {
    try {
      if (file.getParent() != null) Files.createDirectories(file.getParent());
      Path temp = file.resolveSibling(file.getFileName() + ".tmp");
      try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temp))) {
        out.writeObject(new LinkedHashMap<>(projects));
      }
      Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo guardar " + file, e);
    }
  }
}
