# Motor de juegos 2D "PIXELFORGE"

**Proyecto Final — Diseño de Software**

Motor de juegos 2D de plataformas que funciona en el navegador. El diseñador arma proyectos, escenas y objetos, y les agrega componentes como física, colisiones, controles o puntos. Después prueba la escena en modo juego, y el backend es quien calcula la gravedad, los choques y el resultado de la partida.

> **Estado:** análisis, diseño (5 vistas de arquitectura) e implementación **completos y desplegados**.
> - **Aplicación en línea:** https://pixelforge-juur.onrender.com (no hay que instalar nada; la primera carga puede tardar cerca de un minuto si el servicio estaba dormido).
> - **API:** https://pixelforge-juur.onrender.com/api/catalog
> - **Backend:** Java 21 con Spring Boot 4.2, en [`backend/`](backend/).
> - **Frontend:** HTML + JavaScript, en [`frontend/`](frontend/).
> - **Base de datos:** MongoDB (local o Atlas).
> - **Diagramas:** Visual Paradigm, en [`diagramas/`](diagramas/).

---

## Actores

| Actor | Tipo | Objetivo principal |
|---|---|---|
| Diseñador de juegos | Primario | Crear proyectos, escenas y objetos, y configurar sus componentes |
| Jugador | Primario | Jugar una escena con el teclado (izquierda, derecha, saltar) |
| Operador del servidor | Secundario | Arrancar el servidor (`run.ps1`) y elegir puerto y almacenamiento |
| Servidor MongoDB | Sistema externo | Guardar y devolver los proyectos como documentos |

---

## Alcance

| Módulo | Qué incluye |
|---|---|
| Proyectos | Crear, listar, renombrar y eliminar proyectos |
| Escenas | Crear, editar (tamaño, gravedad, fondo) y eliminar escenas de un proyecto |
| Objetos | Crear objetos desde plantillas (jugador, plataforma, moneda, enemigo, pinchos), moverlos con el ratón, redimensionarlos y eliminarlos |
| Componentes | `Sprite`, `RigidBody`, `BoxCollider`, `PlayerController`, `Patrol`, `Collectible` y `Hazard`, que se agregan o quitan de cada objeto |
| Modo juego | El navegador envía las teclas y el backend calcula física, colisiones, puntos y resultado. Se gana recogiendo todas las monedas y se pierde al tocar un peligro o al caer de la escena |
| Persistencia | Un documento de MongoDB por proyecto. Como alternativa sin MongoDB, se puede guardar en un archivo |

Queda **fuera del alcance**: usuarios e inicio de sesión, editor de imágenes o sonido, animaciones por cuadros, exportar el juego como ejecutable y juego en línea entre varias personas.

---

## Backend (código fuente)

La carpeta [`backend/`](backend/) contiene la API REST en **Java 21 con Spring Boot 4.2** (Spring Web MVC + Spring Data MongoDB), construida con Maven. Spring solo se usa en los bordes: los controladores REST, la conexión a MongoDB y el arranque. El dominio, el juego y los controladores GRASP son clases Java normales, sin anotaciones de Spring, que se registran como `@Bean` en `PixelForgeApplication`. El código está organizado en las capas de la vista de desarrollo:

| Paquete | Capa |
|---|---|
| `com.pixelforge.web` | Presentación: `@RestController` de la API, manejo de errores (`@RestControllerAdvice`) y `ViewMapper` |
| `com.pixelforge.application` | Aplicación: controladores GRASP de los casos de uso (`EditorController`, `PlayController`) |
| `com.pixelforge.runtime` | Ejecución del juego: partida, física y registro de sesiones |
| `com.pixelforge.domain` / `.component` | Modelo de dominio: proyecto, escena, objeto y componentes |
| `com.pixelforge.persistence` | Datos: repositorio de proyectos (MongoDB, archivo o memoria) |

### Cómo se guarda en MongoDB

Un proyecto es un agregado: sus escenas, objetos y componentes no existen por separado. Por eso se guarda como **un documento por proyecto**, con todo anidado:

```json
{ "_id": "a1b2c3d4", "name": "Demo de plataformas", "createdAt": ISODate("..."),
  "scenes": [ { "id": "...", "name": "Nivel 1", "width": 960, "height": 540, "gravity": 1400, "background": "#87ceeb",
                "objects": [ { "id": "...", "name": "Jugador", "x": 60, "y": 440, "width": 32, "height": 48,
                               "components": [ { "type": "RigidBody", "properties": { "gravityScale": 1 } } ] } ] } ] }
```

Guardar es un `replaceOne` con `upsert` (inserta o reemplaza el documento completo). Leer reconstruye los objetos del dominio usando la misma `ComponentFactory` del editor.

### API REST

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

---

## Frontend (código fuente)

La carpeta [`frontend/`](frontend/) contiene un solo archivo, [`index.html`](frontend/index.html), con HTML, CSS y JavaScript sin dependencias. Tiene dos modos:

- **Editor:** lista de proyectos y escenas, lienzo (`<canvas>`) donde se arrastran los objetos, y panel de propiedades y componentes del objeto seleccionado.
- **Modo juego:** captura las teclas, las envía a `/api/sessions/{id}/step` unas 30 veces por segundo y dibuja el estado que devuelve el backend.

El backend sirve esta página: al abrir `http://localhost:8080` entrega `frontend/index.html`, y la página llama a la API en `/api`. No hace falta un servidor aparte ni configurar CORS.

---

## Cómo ejecutarlo

**Requisitos:**
1. **JDK 21.**
2. **MongoDB.** Tienes dos opciones:
   - Instalar [MongoDB Community Server](https://www.mongodb.com/try/download/community). En Windows queda como servicio en `localhost:27017`.
   - Usar MongoDB Atlas y poner su cadena de conexión antes de arrancar: `$env:MONGODB_URI = "mongodb+srv://usuario:clave@cluster.../"`
3. **Internet la primera vez.** El wrapper de Maven (`mvnw`) descarga Maven, Spring Boot y el driver de MongoDB. No hace falta instalar Maven.

```powershell
cd backend
.\run.ps1                 # MongoDB en localhost:27017 · abre http://localhost:8080
.\run.ps1 -Storage file   # sin MongoDB: guarda en backend\data\pixelforge.dat
.\test.ps1                # 14 pruebas JUnit 5 (no necesitan MongoDB encendido)
```

Sin los scripts: `.\mvnw.cmd spring-boot:run` y `.\mvnw.cmd test` (en Linux o macOS, `./mvnw`). Para usar archivo en vez de MongoDB, define la variable `PIXELFORGE_STORAGE=file`.

Si PowerShell bloquea los scripts, ejecuta primero `Set-ExecutionPolicy -Scope Process Bypass`.

Al arrancar por primera vez se crea el proyecto de ejemplo **"Demo de plataformas"**. Para ver los datos en **MongoDB Compass**, conéctate a `mongodb://localhost:27017` y abre la base `pixelforge`, colección `projects`.

---

## Despliegue en la nube (Render + MongoDB Atlas)

Para usar PixelForge sin instalar nada, solo con el navegador, se despliega como **un solo servicio web en [Render](https://render.com)** y con la base de datos en **[MongoDB Atlas](https://www.mongodb.com/atlas)**. Ambos tienen plan gratis. No hace falta un hosting aparte para el frontend: el mismo backend entrega `index.html`.

```
Navegador ──HTTPS──▶ Render (Docker: pixelforge.jar + frontend/) ──TLS──▶ MongoDB Atlas
```

| Archivo | Para qué |
|---|---|
| [`render.yaml`](render.yaml) | Blueprint de Render: servicio Docker, plan free, variable `MONGODB_URI`, health check en `/api/catalog` |
| [`backend/Dockerfile`](backend/Dockerfile) | Compila con Maven (corre las pruebas) y arma una imagen con el JRE 21, el `.jar` y `frontend/` |
| [`.dockerignore`](.dockerignore) | Deja fuera de la imagen `.git`, los diagramas y lo compilado |
| [`.gitattributes`](.gitattributes) | Mantiene `mvnw` con saltos de línea de Linux aunque se haga commit desde Windows |

**1. Base de datos en MongoDB Atlas**

1. Crear un clúster gratis (M0).
2. En *Database Access*, crear un usuario con contraseña.
3. En *Network Access*, permitir `0.0.0.0/0` (Render no tiene IP fija en el plan gratis).
4. En *Connect → Drivers*, copiar la cadena `mongodb+srv://usuario:clave@cluster.../`. La base `pixelforge` se crea sola al guardar el primer proyecto.

**2. Servicio en Render**

1. En Render: *New → Blueprint* y conectar este repositorio de GitHub. Render lee `render.yaml`.
2. Cuando pida `MONGODB_URI`, pegar la cadena de Atlas y aplicar.
3. Esperar el primer build (unos minutos: descarga Maven, compila y corre las pruebas). La URL de este despliegue es **https://pixelforge-juur.onrender.com**.

Cada `git push` a `main` vuelve a desplegar solo (`autoDeploy: true`).

Limitaciones del plan gratis de Render: el servicio se duerme tras un rato sin visitas y la primera petición después tarda en despertar (del orden de un minuto). Las partidas en curso viven en memoria y se pierden si el servicio se reinicia; los proyectos no, porque están en Atlas.

---

## Diagramas

Los diagramas se hicieron en Visual Paradigm. En [`diagramas/visual-paradigm/`](diagramas/visual-paradigm/) están las exportaciones en PNG, y en [`diagramas/fuentes/`](diagramas/fuentes/) los proyectos editables (`.vpp`).

### Vista de contexto (vista de sistema)

Muestra al sistema como un solo proceso y lo que intercambia con cada entidad externa: el diseñador, el jugador, el operador del servidor y MongoDB.

![Vista de contexto](diagramas/visual-paradigm/01_vista_contexto.png)

### Vista funcional (componentes)

Divide el sistema en seis componentes: Interfaz web, API HTTP, Editor, Juego, Modelo del juego y Persistencia. También muestra las interfaces que cada uno **provee** (lollipop) y **requiere** (socket). El Juego usa al Editor para leer la escena, y la Persistencia es la única que habla con MongoDB.

![Vista funcional](diagramas/visual-paradigm/02_vista_funcional.png)

### Vista lógica (aplicación y servicios)

Muestra las clases que atienden las peticiones. Los `@RestController` `ProjectApiController` y `PlayApiController` reciben la petición y la pasan a los controladores GRASP `EditorController` y `PlayController`, que guardan a través de la interfaz `ProjectRepository`. Esa interfaz tiene tres implementaciones: MongoDB, memoria y archivo. Aquí se ven las realizaciones, la herencia, la agregación y las dependencias «use» y «create».

![Vista lógica](diagramas/visual-paradigm/04_vista_logica_servicios.png)

### Vista de desarrollo (implementación)

Organiza el código en paquetes de Java (web, application, runtime, domain, domain.component y persistence) y muestra las dependencias de `import` entre ellos y con las librerías externas: Spring Boot (Web MVC y Data MongoDB) y el driver de MongoDB.

![Vista de desarrollo](diagramas/visual-paradigm/05_vista_desarrollo.png)

### Vista de despliegue

Todo corre en el PC del usuario. El navegador se comunica por HTTP en `localhost:8080` con la JVM, que ejecuta la aplicación Spring Boot con su Tomcat embebido. La JVM habla con MongoDB por TCP en el puerto 27017 y lee del sistema de archivos el `index.html` del frontend.

![Vista de despliegue](diagramas/visual-paradigm/06_vista_despliegue.png)

| Archivo | Contenido |
|---|---|
| [01_vista_contexto.png](diagramas/visual-paradigm/01_vista_contexto.png) | Vista de contexto exportada |
| [02_vista_funcional.png](diagramas/visual-paradigm/02_vista_funcional.png) | Vista funcional por componentes exportada |
| [04_vista_logica_servicios.png](diagramas/visual-paradigm/04_vista_logica_servicios.png) | Vista lógica (clases de aplicación y servicios) exportada |
| [05_vista_desarrollo.png](diagramas/visual-paradigm/05_vista_desarrollo.png) | Vista de desarrollo (paquetes) exportada |
| [06_vista_despliegue.png](diagramas/visual-paradigm/06_vista_despliegue.png) | Vista de despliegue exportada |
| [PixelForge Arquitectura.vpp](diagramas/fuentes/PixelForge%20Arquitectura.vpp) | Fuente editable de las vistas de contexto, lógica, desarrollo y despliegue (Visual Paradigm) |
| [PixelForge Vista funcional.vpp](diagramas/fuentes/PixelForge%20Vista%20funcional.vpp) | Fuente editable de la vista funcional (Visual Paradigm) |

---

## Patrones GRASP aplicados

| Patrón | Dónde | Por qué |
|---|---|---|
| Experto en información | `GameObject.overlaps`, `GameObject.getComponent`, `Scene.findObject`, `Project.findScene`, `GameSession.step`, `RigidBody` (sabe si está en el suelo) | Cada responsabilidad la cumple la clase que ya tiene los datos para hacerlo. |
| Creador | `Project.createScene`, `Scene.createObject`, `EditorController.createProject`, `PlayController.start` (crea `GameSession`) | Crea un objeto quien lo contiene, lo registra o tiene los datos para inicializarlo. Por eso los constructores de `Scene` y `GameObject` son de paquete. |
| Controlador | `EditorController` (caso de uso «Editar juego») y `PlayController` («Jugar escena») | Son el primer objeto después de la interfaz que recibe los eventos del sistema. No hacen el trabajo: delegan en el dominio. |
| Bajo acoplamiento | `FrameContext`, `ViewMapper`, la inyección por constructor con `@Bean` en `PixelForgeApplication` | Los componentes no conocen `GameSession`, y el dominio no conoce HTTP, JSON ni archivos. |
| Alta cohesión | `PhysicsSystem` (solo física), `ApiExceptionHandler` (solo errores HTTP), `ProjectApiController` (solo rutas del editor) | Cada clase tiene una sola responsabilidad. |
| Polimorfismo | `Component.update`, `copy` y `properties` en las 7 subclases; `ProjectRepository` con tres implementaciones (Mongo, archivo, memoria) | La partida llama `update()` sin preguntar el tipo del componente, y los controladores guardan sin saber si es MongoDB o un archivo. |
| Fabricación pura | `ComponentFactory`, `PhysicsSystem`, `SessionRegistry`, `ProjectDocumentMapper`, `MongoProjectRepository`, `ViewMapper` | Son clases que no existen en el mundo del juego; se inventan para no ensuciar al dominio. |
| Indirección | `ProjectRepository` (entre los controladores y MongoDB), los `@RestController` y los controladores GRASP (entre el navegador y el dominio) | Un intermediario evita que dos partes dependan directamente la una de la otra. |
| Variaciones protegidas | `ProjectRepository`, la clase abstracta `Component`, `ComponentFactory` | Pasar de archivo a MongoDB solo cambió `PixelForgeApplication` (`projectRepository`); agregar un componente solo toca la fábrica. |

---

## Estructura

```
├── README.md
├── render.yaml                # Despliegue en Render (Blueprint)
├── backend/                   # API REST Spring Boot (Java 21 + Maven)
│   ├── pom.xml · mvnw · mvnw.cmd · run.ps1 · test.ps1 · Dockerfile
│   ├── src/main/resources/application.properties
│   └── src/
│       ├── main/java/com/pixelforge/
│       │   ├── PixelForgeApplication.java  # Arranque Spring Boot y @Bean del sistema
│       │   ├── web/           # ProjectApiController, PlayApiController, ApiExceptionHandler, ViewMapper
│       │   ├── application/   # EditorController, PlayController, NotFoundException
│       │   ├── runtime/       # GameSession, PhysicsSystem, SessionRegistry, GameStatus
│       │   ├── domain/        # Project, Scene, GameObject, Vector2, Ids
│       │   │   └── component/ # Component, sus 7 subclases, ComponentFactory, FrameContext, InputState
│       │   └── persistence/   # ProjectRepository y sus implementaciones, ProjectDocumentMapper
│       └── test/java/com/pixelforge/EngineTests.java
├── frontend/
│   └── index.html             # Editor y modo juego (HTML + JavaScript)
└── diagramas/
    ├── visual-paradigm/       # Vistas de arquitectura exportadas en PNG
    └── fuentes/               # Proyectos editables de Visual Paradigm (.vpp)
```

---

## Integrantes

- Jesús David Pantoja González

Proyecto académico — Universidad Cooperativa de Colombia, uso educativo.
