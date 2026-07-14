# EduVerify — Backend básico (IAM + Academic Management)

## Contexto

El repositorio `oop-java-main` es el ejemplo que dio el profesor: un proyecto Maven en Java plano (sin framework), con Lombok, organizado por *bounded context* como paquetes top-level (`crm`, `sales`, `shared`). Cada contexto tiene `domain/model/aggregates` y `domain/model/valueobjects`. No hay API, ni persistencia, ni capa de aplicación — es solo un `Main.java` que ejercita los objetos de dominio por consola.

El objetivo es construir, sobre esa misma base y estilo, un backend real (Spring Boot) que sirva al front-end React de EduVerify (`Front-end/Eduverify`), que hoy usa datos mock (`mockUsers.js`, `mockExams.js`) vía Context API.

Se modelan solo los dos *bounded contexts* que el front-end ya necesita, según los diagramas UML del proyecto (`UML-codes/Diagramas de clases/DC3.puml` — IAM, `DC4.puml` — Academic Management):

- **IAM** — usuarios, registro, login.
- **Academic Management** — exámenes y sesiones de examen por estudiante.

Los demás contextos (Proctoring, Incident Management, Reporting, Logging, Institution) quedan fuera de este alcance hasta que el front-end tenga pantallas que los necesiten.

## Decisiones ya tomadas (ver preguntas de brainstorming)

- El backend debe servir de verdad al front-end vía HTTP (no es solo ejercicio de dominio).
- Alcance inicial: solo IAM + Academic Management.
- Persistencia: en memoria (sin base de datos), consistente con lo que ya asumen los mocks del front-end.
- Autenticación: token opaco (UUID) en memoria, sin JWT ni Spring Security.

## Decisión de diseño: `User` único con `role`, sin `Student`/`Teacher` separados (por ahora)

El diagrama `DC3.puml` modela `User`, `Student` y `Teacher` como tres entidades separadas, donde `Student`/`Teacher` referencian a `User` vía `UserId`. Para la versión más básica, se colapsa esto en un solo aggregate `User` con un campo `role` (el diagrama ya contempla `role: String` en `User`), porque:

- El front-end no usa hoy campos exclusivos de `Student` (`studentCode`, `enrollmentDate`) ni de `Teacher` (`hireDate`).
- Evita relaciones/joins adicionales sin necesidad real todavía.

**Extensión futura:** cuando se necesite `studentCode` o `hireDate`, se agregan `Student`/`Teacher` como entidades propias que referencian `UserId`, tal como está en el diagrama, sin tocar lo ya construido.

## Desviación del diagrama: `login()`/`logout()` no son métodos de instancia

El diagrama pone `login()`/`logout()` como métodos de `User`. En la implementación, autenticar requiere comparar un hash de contraseña y emitir un token — es un proceso, no una mutación de estado del propio objeto. Se modela como un **domain service** (`AuthenticationService`) que orquesta `UserRepository` + verificación de password + `TokenStore`, siguiendo el mismo espíritu del diagrama (el comportamiento de login vive en el contexto IAM) sin forzarlo dentro del aggregate.

## Arquitectura

Un solo proyecto Spring Boot (Maven). Estructura por contexto, extendiendo el patrón del profesor con las capas mínimas necesarias para exponer una API:

```
<context>/
  domain/
    model/
      aggregates/       <- igual que el profe (Customer, SalesOrder)
      valueobjects/      <- igual que el profe (Money, Address) — records con validación
    services/            <- interfaces (Repository, servicios de dominio) — nuevo
  application/            <- implementación de los servicios de dominio — nuevo
  infrastructure/
    persistence/          <- repositorio en memoria — nuevo
    security/              <- (solo iam) TokenStore, filtro de auth — nuevo
  interfaces/
    rest/                  <- Controller + resources (DTOs) — nuevo
```

No se separan comandos/queries (CQRS) ni se agrega ACL entre contextos — eso pertenece a una plantilla DDD más completa, más de lo que pide "lo más básico posible". El camino para evolucionar a eso más adelante queda abierto (ver sección final).

### Paquetes

```
com.eduverify.platform
├── EduverifyApplication.java              (@SpringBootApplication)
├── shared
│   ├── domain/model/valueobjects/UserId.java
│   ├── domain/exceptions/NotFoundException.java
│   ├── domain/exceptions/UnauthorizedException.java
│   └── infrastructure/security/CurrentUserContext.java   (ThreadLocal<UserId>)
│   └── interfaces/rest/GlobalExceptionHandler.java
├── iam
│   ├── domain/model/aggregates/User.java
│   ├── domain/model/valueobjects/Role.java                (enum: STUDENT, TEACHER)
│   ├── domain/services/UserRepository.java                 (interface)
│   ├── domain/services/AuthenticationService.java           (interface)
│   ├── application/AuthenticationServiceImpl.java
│   ├── infrastructure/persistence/InMemoryUserRepository.java
│   ├── infrastructure/security/TokenStore.java
│   ├── infrastructure/security/TokenAuthenticationFilter.java
│   └── interfaces/rest/
│       ├── AuthController.java
│       └── resources/ (RegisterResource, LoginResource, AuthenticatedUserResource, UserResource)
└── academicmanagement
    ├── domain/model/aggregates/Exam.java
    ├── domain/model/aggregates/ExamSession.java             (entidad hija, como SalesOrderItem)
    ├── domain/model/valueobjects/ExamId.java
    ├── domain/model/valueobjects/ExamSessionId.java
    ├── domain/model/valueobjects/ExamStatus.java              (enum: SCHEDULED, IN_PROGRESS, FINISHED)
    ├── domain/model/valueobjects/ExamSessionStatus.java         (enum: IN_PROGRESS, FINISHED)
    ├── domain/services/ExamRepository.java                       (interface)
    ├── domain/services/ExamService.java                           (interface)
    ├── application/ExamServiceImpl.java
    ├── infrastructure/persistence/InMemoryExamRepository.java
    └── interfaces/rest/
        ├── ExamsController.java
        └── resources/ (CreateExamResource, ExamResource, ExamSessionResource)
```

`ExamId` vive en `academicmanagement`, no en `shared`, porque solo ese contexto lo posee (igual que `ProductId` vive en `sales`, no en `shared`, en el ejemplo del profe). `UserId` sí va en `shared` porque tanto `iam` como `academicmanagement` lo referencian (`Exam.teacherId`, `ExamSession.studentId`) — igual que `CustomerId` vive en `shared` porque `sales` lo referencia.

## Modelo de dominio

### IAM

**`User`** (aggregate root) — mismo estilo que `Customer.java`: `@Getter`, campos `@Setter @NonNull`, validación en el constructor con `IllegalArgumentException`.

```java
public class User {
    private final UserId id;
    @Setter @NonNull private String email;
    @Setter @NonNull private String passwordHash;
    private final Role role;

    public User(String email, String passwordHash, Role role) { ... }
}
```

**`Role`** — enum simple: `STUDENT`, `TEACHER`.

### Academic Management

**`Exam`** (aggregate root) — mismo estilo que `SalesOrder.java`. Contiene la lista de `ExamSession` y controla su propio ciclo de vida (`startExam()`/`endExam()` del diagrama → `start()`/`finish()`).

```java
public class Exam {
    private final ExamId id;
    private final UserId teacherId;
    @Setter @NonNull private String title;
    private final LocalDateTime scheduledDate;
    private final int durationMinutes;
    private ExamStatus status;
    private final List<ExamSession> sessions;

    public Exam(UserId teacherId, String title, LocalDateTime scheduledDate, int durationMinutes) { ... }
    public void start() { ... }   // SCHEDULED -> IN_PROGRESS
    public void finish() { ... }  // IN_PROGRESS -> FINISHED
    public ExamSession startSessionFor(UserId studentId) { ... }   // como addItem en SalesOrder
    public void finishSession(ExamSessionId sessionId) { ... }
}
```

**`ExamSession`** (entidad hija, no aggregate propio) — mismo estilo que `SalesOrderItem.java`: constructor *package-private*, solo se crea desde `Exam.startSessionFor(...)`.

```java
public class ExamSession {
    private final ExamSessionId id;
    private final ExamId examId;
    private final UserId studentId;
    private final LocalDateTime startTime;
    @Setter private LocalDateTime endTime;
    @Setter private ExamSessionStatus status;

    ExamSession(ExamId examId, UserId studentId) { ... }   // package-private, como SalesOrderItem
    void finish() { ... }
}
```

## API

```
POST /api/iam/register                              -> crea User                         201
POST /api/iam/login                                  -> valida credenciales, emite token   200

POST /api/exams                                       (teacher) crea Exam                  201
GET  /api/exams                                        lista exámenes                       200
GET  /api/exams/{examId}                                detalle                              200
POST /api/exams/{examId}/start                           (teacher) SCHEDULED -> IN_PROGRESS    200
POST /api/exams/{examId}/finish                            (teacher) IN_PROGRESS -> FINISHED     200
POST /api/exams/{examId}/sessions                            (student) "acceder al examen"          201
PUT  /api/exams/{examId}/sessions/{sessionId}/finish            (student) "entregar examen"            200
```

Todas las rutas (excepto `register`/`login`) requieren header `Authorization: Bearer <token>`.

## Autenticación

- `TokenStore` (in-memory `Map<String, UserId>`): `issue(UserId)`, `resolve(String token)`, `revoke(String token)`.
- `TokenAuthenticationFilter` (`OncePerRequestFilter`, `@Component`): lee el header, resuelve el `UserId` vía `TokenStore`, lo deja en `CurrentUserContext` (ThreadLocal) para el resto del request, y lo limpia al terminar. Rutas exentas (lista fija en el filtro): `POST /api/iam/register` y `POST /api/iam/login`. Para cualquier otra ruta, si el token falta o no resuelve, corta con 401 antes de llegar al controller.
- Los controllers que necesitan el usuario actual (p.ej. `POST /api/exams/{id}/sessions` para saber qué estudiante accede) lo leen de `CurrentUserContext.get()`.
- Password hashing: `MessageDigest` SHA-256 simple (sin sal ni BCrypt) para no traer una dependencia nueva solo para esto. **No es apto para producción real** — se deja anotado como mejora futura (migrar a `BCryptPasswordEncoder` de `spring-security-crypto` cuando se quiera reforzar seguridad).

## Persistencia

Repositorios en memoria respaldados por `ConcurrentHashMap`, implementando las interfaces de `domain/services`:

- `InMemoryUserRepository implements UserRepository`
- `InMemoryExamRepository implements ExamRepository`

Los datos se pierden al reiniciar el servidor — mismo comportamiento que ya asumen los mocks actuales del front-end.

## Manejo de errores

- `IllegalArgumentException` (validaciones de dominio, igual que el profe) → 400, cuerpo `{ "message": "..." }`.
- `NotFoundException` (nuevo, en `shared/domain/exceptions`) → 404.
- `UnauthorizedException` (nuevo) → 401.
- Un solo `@RestControllerAdvice` (`shared/interfaces/rest/GlobalExceptionHandler`) centraliza el mapeo.

## Flujos de datos (ejemplo)

**Registro + login:**
1. Front envía `POST /api/iam/register {email, password, role}`.
2. `AuthController` → `AuthenticationService.register(...)` → valida que el email no exista, crea `User`, lo guarda vía `UserRepository`.
3. Front envía `POST /api/iam/login {email, password}`.
4. `AuthenticationService.login(...)` busca el `User`, compara hash, emite token vía `TokenStore`, devuelve `{ token, id, email, role }`.

**Crear examen (docente) → acceder (estudiante) → entregar:**
1. `POST /api/exams` con token del docente → `ExamService.create(...)` crea `Exam` en `SCHEDULED`.
2. `POST /api/exams/{id}/start` (docente) → `IN_PROGRESS`.
3. `POST /api/exams/{id}/sessions` con token del estudiante → `Exam.startSessionFor(studentId)` crea `ExamSession`, devuelve `sessionId`.
4. `PUT /api/exams/{id}/sessions/{sessionId}/finish` → `Exam.finishSession(sessionId)`.

## Nota de integración con el front-end

Los mocks actuales (`mockExams.js`) mezclan campos crudos del examen con datos derivados de las sesiones (`inscritos`, `conectados`, `alertas`, `nota`) y usan nombres en español. La API nueva expone el modelo de dominio tal cual el diagrama (inglés, sin campos derivados). El front-end deberá:
- Mapear nombres de campo al conectar los servicios reales.
- Calcular `inscritos`/`conectados` a partir de `GET /api/exams/{id}` + el conteo de `sessions` (se puede agregar un endpoint agregador más adelante si hace falta antes de tiempo).

Esto no se resuelve en este backend básico; se deja explícito para no generar una expectativa incorrecta.

## Testing

- Unitarios de dominio (JUnit 5, vía `spring-boot-starter-test`): reglas de validación y comportamiento de `User`, `Exam`, `ExamSession` — mismo espíritu que `Main.java` del profe, pero como tests reales en vez de prints.
- Integración por endpoint clave (`MockMvc` o `TestRestTemplate`): registro+login, crear+acceder+entregar examen.

## pom.xml

Se parte del `pom.xml` del profe (Lombok, Commons Lang3, Java 26) y se agrega:
- `spring-boot-starter-parent` como parent (versión a confirmar contra la que soporte Java 26 al momento de implementar).
- `spring-boot-starter-web`
- `spring-boot-starter-test`

## Camino de extensión futuro (fuera de alcance de este backend básico)

- Agregar `Student`/`Teacher` como entidades propias cuando el front-end los necesite.
- Swap de repos en memoria → JPA + MySQL (implementando las mismas interfaces `UserRepository`/`ExamRepository`), sin tocar dominio ni controllers.
- Token opaco → JWT + Spring Security, si se necesita expiración/roles más finos.
- Nuevos bounded contexts (Proctoring, Incident Management, Reporting, Logging, Institution) como paquetes top-level nuevos, siguiendo la misma estructura de capas.
