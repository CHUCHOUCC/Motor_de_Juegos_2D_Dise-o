# PixelForge · Motor de juegos 2D (Diseño de Software)

Editor de juegos de plataformas en el navegador. El backend es **Java 21**, sin Maven ni frameworks, y guarda los proyectos en **MongoDB**. El frontend es un solo archivo HTML.

## Requisitos

1. **JDK 21.**
2. **MongoDB.** Tienes dos opciones:
   - Instalar MongoDB Community Server en tu PC (https://www.mongodb.com/try/download/community). En Windows queda como servicio en `localhost:27017`, así que no hay que configurar nada.
   - Usar MongoDB Atlas (gratis, en la nube) y poner su cadena de conexión antes de arrancar: `$env:MONGODB_URI = "mongodb+srv://usuario:clave@cluster.../"`
3. Internet la primera vez: `run.ps1` descarga el driver oficial de MongoDB para Java (4 jars) en `backend/lib/`.

## Cómo ejecutarlo (Windows, PowerShell)

```powershell
cd PixelForge\backend
.\run.ps1                 # MongoDB en localhost:27017 · abre http://localhost:8080
.\run.ps1 -Storage file   # sin MongoDB: guarda en backend\data\pixelforge.dat
.\test.ps1                # 14 pruebas (no necesitan MongoDB encendido)
```

Si PowerShell bloquea los scripts, ejecuta primero: `Set-ExecutionPolicy -Scope Process Bypass`.

Al arrancar por primera vez se crea el proyecto de ejemplo "Demo de plataformas". Para ver los datos, abre **MongoDB Compass** y conéctate a `mongodb://localhost:27017`: base `pixelforge`, colección `projects`.

## Cómo se guarda en MongoDB

Un proyecto es un agregado: sus escenas, objetos y componentes no existen por separado. Por eso se guarda como **un documento por proyecto**, con todo anidado:

```json
{ "_id": "a1b2c3d4", "name": "Demo de plataformas", "createdAt": ISODate("..."),
  "scenes": [ { "id": "...", "name": "Nivel 1", "width": 960, "height": 540, "gravity": 1400, "background": "#87ceeb",
                "objects": [ { "id": "...", "name": "Jugador", "x": 60, "y": 440, "width": 32, "height": 48,
                               "components": [ { "type": "RigidBody", "properties": { "gravityScale": 1 } } ] } ] } ] }
```

Guardar es un `replaceOne` con `upsert` (inserta o reemplaza el documento completo). Leer reconstruye los objetos del dominio usando la misma `ComponentFactory` del editor.

## Qué hace

- **Editor:** proyectos, escenas y objetos. Los objetos se crean a partir de plantillas (jugador, plataforma, moneda, enemigo, pinchos), se arrastran con el ratón y se les agregan o quitan componentes.
- **Modo juego:** el navegador solo manda las teclas, y el backend calcula la gravedad, los choques, los puntos y si se gana o se pierde. Se gana recogiendo todas las monedas y se pierde al tocar un peligro o al caer fuera de la escena.

## Estructura del repositorio

```
backend/                        servidor en Java 21 (sin frameworks)
  run.ps1 · test.ps1 · lib.ps1  compilar y arrancar / pruebas / descarga del driver (Windows)
  run.sh                        compilar y arrancar (Linux/macOS)
  src/main/java/com/pixelforge/
    Main.java                   arma el sistema y abre el servidor
    domain/                     Project, Scene, GameObject, Vector2, Ids
    domain/component/           Component y sus subclases, ComponentFactory, FrameContext, InputState
    runtime/                    GameSession, PhysicsSystem, SessionRegistry, GameStatus
    application/                EditorController, PlayController, NotFoundException
    persistence/                ProjectRepository, MongoProjectRepository, ProjectDocumentMapper,
                                InMemoryProjectRepository, FileProjectRepository
    web/                        ApiHandler, StaticFileHandler, Request, Json, ViewMapper
  src/test/java/com/pixelforge/EngineTests.java
frontend/
  index.html                    interfaz del editor y del modo juego (HTML + JavaScript)
diagramas/
  visual-paradigm/              las vistas de arquitectura hechas en Visual Paradigm
  fuentes/plantuml/             código PlantUML de cada vista
  fuentes/xml/                  cada vista en XMI 2.1 (File > Import > XMI en Visual Paradigm)
```

El backend sirve el frontend: al abrir `http://localhost:8080` entrega `frontend/index.html`, y la página llama a la API en `/api`. Por eso los scripts se ejecutan desde `backend/`.

## Diagramas de arquitectura

| Vista | Archivo |
|---|---|
| Contexto | [01_vista_contexto.png](diagramas/visual-paradigm/01_vista_contexto.png) |
| Funcional (componentes) | [02_vista_funcional.png](diagramas/visual-paradigm/02_vista_funcional.png) |
| Lógica (aplicación y servicios) | [04_vista_logica_servicios.png](diagramas/visual-paradigm/04_vista_logica_servicios.png) |
| Desarrollo (paquetes) | [05_vista_desarrollo.png](diagramas/visual-paradigm/05_vista_desarrollo.png) |
| Despliegue | [06_vista_despliegue.png](diagramas/visual-paradigm/06_vista_despliegue.png) |

## Patrones GRASP

| Patrón | Dónde | Por qué |
|---|---|---|
| Experto en información | `GameObject.overlaps`, `GameObject.getComponent`, `Scene.findObject`, `Project.findScene`, `GameSession.step`, `RigidBody` (sabe si está en el suelo) | Cada responsabilidad la cumple la clase que ya tiene los datos para hacerlo. |
| Creador | `Project.createScene`, `Scene.createObject`, `EditorController.createProject`, `PlayController.start` (crea `GameSession`) | Crea un objeto quien lo contiene, lo registra o tiene los datos para inicializarlo. Por eso los constructores de `Scene` y `GameObject` son de paquete. |
| Controlador | `EditorController` (caso de uso «Editar juego») y `PlayController` («Jugar escena») | Son el primer objeto después de la interfaz que recibe los eventos del sistema. No hacen el trabajo: delegan en el dominio. |
| Bajo acoplamiento | `FrameContext`, `ViewMapper`, la inyección por constructor en `Main` | Los componentes no conocen `GameSession`, y el dominio no conoce HTTP, JSON ni archivos. |
| Alta cohesión | `PhysicsSystem` (solo física), `Json` (solo formato), `Request` (solo leer la petición), `ApiHandler` (solo rutas) | Cada clase tiene una sola responsabilidad. |
| Polimorfismo | `Component.update`, `copy` y `properties` en las 7 subclases; `ProjectRepository` con tres implementaciones (Mongo, archivo, memoria) | La partida llama `update()` sin preguntar el tipo del componente, y los controladores guardan sin saber si es MongoDB o un archivo. |
| Fabricación pura | `ComponentFactory`, `PhysicsSystem`, `SessionRegistry`, `ProjectDocumentMapper`, `MongoProjectRepository`, `Json`, `ViewMapper` | Son clases que no existen en el mundo del juego; se inventan para no ensuciar al dominio. |
| Indirección | `ProjectRepository` (entre los controladores y MongoDB), `ApiHandler` y los controladores (entre el navegador y el dominio) | Un intermediario evita que dos partes dependan directamente la una de la otra. |
| Variaciones protegidas | `ProjectRepository`, la clase abstracta `Component`, `ComponentFactory` | Pasar de archivo a MongoDB solo cambió `Main` (`createRepository`); agregar un componente solo toca la fábrica. |

## API

Todas las rutas responden JSON. Los datos se envían como formulario (`application/x-www-form-urlencoded`).

| Método | Ruta | Acción |
|---|---|---|
| GET | `/api/catalog` | tipos de componente y plantillas |
| GET / POST | `/api/projects` | listar / crear (`name`) |
| GET | `/api/projects/{p}` | ver un proyecto con sus escenas |
| POST | `/api/projects/{p}/rename` · `/delete` | renombrar / eliminar |
| POST | `/api/projects/{p}/scenes` | crear escena (`name`, `width`, `height`, `gravity`, `background`) |
| GET | `/api/projects/{p}/scenes/{s}` | ver la escena con sus objetos |
| POST | `/api/projects/{p}/scenes/{s}/update` · `/delete` | editar / eliminar escena |
| POST | `/api/projects/{p}/scenes/{s}/objects` | crear objeto (`name`, `preset`, `x`, `y`, `width`, `height`) |
| POST | `.../objects/{o}/update` · `/delete` | mover o redimensionar / eliminar |
| POST | `.../objects/{o}/components` | agregar componente (`type` y sus parámetros) |
| POST | `.../objects/{o}/components/{tipo}/delete` | quitar componente |
| POST | `/api/projects/{p}/scenes/{s}/play` | empezar una partida |
| POST | `/api/sessions/{id}/step` | avanzar un cuadro (`left`, `right`, `jump`) |
