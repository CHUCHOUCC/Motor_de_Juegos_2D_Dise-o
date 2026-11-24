package com.pixelforge.persistence;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.pixelforge.domain.Project;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bson.Document;

/**
 * Guarda los proyectos en MongoDB: base de datos "pixelforge", colección "projects", un documento
 * por proyecto.
 *
 * <p>GRASP Polimorfismo / Variaciones protegidas: es otra implementación de ProjectRepository. Los
 * controladores no cambiaron nada para usar MongoDB; solo Main decide cuál repositorio crear.
 *
 * <p>GRASP Fabricación pura: la conversión a documentos la hace ProjectDocumentMapper, así esta
 * clase solo habla con la base de datos (Alta cohesión).
 */
public class MongoProjectRepository implements ProjectRepository, AutoCloseable {
  private final MongoClient client;
  private final MongoCollection<Document> projects;
  private final ProjectDocumentMapper mapper;

  public MongoProjectRepository(String connectionUri, String databaseName, ProjectDocumentMapper mapper) {
    this.client = MongoClients.create(connectionUri);
    MongoDatabase database = client.getDatabase(databaseName);
    // Ping: si MongoDB no está encendido, el servidor falla al arrancar con un mensaje claro.
    try {
      database.runCommand(new Document("ping", 1));
    } catch (RuntimeException e) {
      client.close();
      throw new IllegalStateException("No se pudo conectar a MongoDB en " + connectionUri
          + ". ¿Está instalado y encendido? (" + e.getMessage() + ")", e);
    }
    this.projects = database.getCollection("projects");
    this.mapper = mapper;
  }

  @Override
  public List<Project> findAll() {
    List<Project> result = new ArrayList<>();
    for (Document document : projects.find()) result.add(mapper.toProject(document));
    return result;
  }

  @Override
  public Optional<Project> findById(String id) {
    Document document = projects.find(eq("_id", id)).first();
    return document == null ? Optional.empty() : Optional.of(mapper.toProject(document));
  }

  /** Inserta o reemplaza el documento completo del proyecto (upsert). */
  @Override
  public void save(Project project) {
    projects.replaceOne(eq("_id", project.id()), mapper.toDocument(project), new ReplaceOptions().upsert(true));
  }

  @Override
  public boolean delete(String id) {
    return projects.deleteOne(eq("_id", id)).getDeletedCount() > 0;
  }

  @Override
  public void close() {
    client.close();
  }
}
