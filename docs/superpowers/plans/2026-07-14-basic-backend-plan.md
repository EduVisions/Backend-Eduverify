# EduVerify Backend Básico (IAM + Academic Management) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Boot backend for EduVerify covering the IAM and Academic Management bounded contexts, in-memory persistence, and token-based auth, so the React front-end can stop using mock data for login/register and exam access/submission.

**Architecture:** Single Spring Boot Maven module (`com.eduverify.platform`), two bounded-context packages (`iam`, `academicmanagement`) plus a `shared` kernel, each following the professor's `domain/model/aggregates` + `domain/model/valueobjects` style (Lombok `@Getter`/`@Setter`/`@NonNull`, Java `record` value objects with validating compact constructors) extended with the minimum extra layers (`domain/services` interfaces, `application` impls, `infrastructure/persistence` in-memory repos, `interfaces/rest` controllers) needed to expose a REST API. Full design rationale: `docs/superpowers/specs/2026-07-14-basic-backend-design.md`.

**Tech Stack:** Java 21 (LTS), Spring Boot 3.3.4 (`spring-boot-starter-web`, `spring-boot-starter-test`), Lombok, JUnit 5, Mockito, MockMvc. No database, no Spring Security, no JWT — in-memory repositories and an opaque UUID token store, per the approved design.

## Global Constraints

- Base package: `com.eduverify.platform`. Module root: `Backend/oop-java-main` (existing `pom.xml`, existing `com.acme.oop.*` professor sample code stays untouched — it is a separate package, do not delete or modify it).
- Java 21 (Temurin) and Maven 3.9.9 are installed on this machine but **not** on the default PATH. Every command in this plan that invokes `mvn` MUST be run in PowerShell with this exact preamble (shell state does not persist between tool calls, so repeat it every time):
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" <args>
  ```
- This project has **no git repository** (confirmed with the user — left unversioned for now). Do **not** run `git init`, `git add`, or `git commit` as part of this plan. Each task's "commit" step below is replaced with "run the full test suite and confirm it's green" as the completion signal.
- All aggregates/value objects follow the professor's exact style from `com.acme.oop.crm`/`com.acme.oop.sales`: records for value objects with a validating compact constructor and a no-arg convenience constructor that generates a random UUID; Lombok `@Getter` on aggregate classes; `@Setter @NonNull` on mutable fields; validation via `if (Objects.isNull(...) ...) throw new IllegalArgumentException(...)` in constructors; child entities (like `ExamSession`) get a package-private constructor instantiated only by their aggregate root, mirroring `SalesOrderItem`.
- Money/BigDecimal/Currency are irrelevant here (that was the sales-context example) — not reused.
- All new tests use JUnit 5 (`org.junit.jupiter.api`) and, where Mockito is needed, `org.mockito` — both already on the classpath via `spring-boot-starter-test`.

---

### Task 1: Bootstrap Spring Boot project

**Files:**
- Modify: `pom.xml` (full rewrite)
- Create: `src/main/java/com/eduverify/platform/EduverifyApplication.java`
- Create: `src/main/resources/application.properties`
- Test: `src/test/java/com/eduverify/platform/EduverifyApplicationTests.java`

**Interfaces:**
- Consumes: nothing (first task)
- Produces: a bootable Spring Boot application with `spring-boot-starter-web` and `spring-boot-starter-test` on the classpath, package root `com.eduverify.platform` for component scanning. Every later task's `@Component`/`@Service`/`@Repository`/`@RestController` beans rely on this scan root.

- [ ] **Step 1: Replace `pom.xml` with the Spring Boot version**

Replace the entire contents of `pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>

    <groupId>com.eduverify</groupId>
    <artifactId>eduverify-api</artifactId>
    <version>1.0.0</version>
    <name>eduverify-api</name>

    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.19.0</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

Note: this changes the Java level for the whole module (including the untouched `com.acme.oop.*` sample) from 26 to 21. That sample has no Java 22+ syntax, so it still compiles fine.

- [ ] **Step 2: Create the application entry point**

`src/main/java/com/eduverify/platform/EduverifyApplication.java`:

```java
package com.eduverify.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EduverifyApplication {
    public static void main(String[] args) {
        SpringApplication.run(EduverifyApplication.class, args);
    }
}
```

- [ ] **Step 3: Add application properties**

`src/main/resources/application.properties`:

```properties
spring.application.name=eduverify-api
server.port=8080
```

- [ ] **Step 4: Write the failing context-load test**

`src/test/java/com/eduverify/platform/EduverifyApplicationTests.java`:

```java
package com.eduverify.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EduverifyApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 5: Run it and confirm it fails (dependencies not yet resolved)**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" test
```
Expected: FAIL or a long dependency-download log the first time (Maven resolving Spring Boot parent/starters from Maven Central), then compiles and the test should actually PASS once dependencies resolve — there's no code to be "missing" here since this is bootstrap. If it fails, read the error: it's almost always a dependency resolution or Java version mismatch, not a missing class.

- [ ] **Step 6: Fix any resolution/version errors, then run again to confirm PASS**

Run the same command as Step 5.
Expected: `Tests run: 1, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

---

### Task 2: Shared kernel — UserId, exceptions, CurrentUserContext, GlobalExceptionHandler

**Files:**
- Create: `src/main/java/com/eduverify/platform/shared/domain/model/valueobjects/UserId.java`
- Create: `src/main/java/com/eduverify/platform/shared/domain/exceptions/NotFoundException.java`
- Create: `src/main/java/com/eduverify/platform/shared/domain/exceptions/UnauthorizedException.java`
- Create: `src/main/java/com/eduverify/platform/shared/infrastructure/security/CurrentUserContext.java`
- Create: `src/main/java/com/eduverify/platform/shared/interfaces/rest/GlobalExceptionHandler.java`
- Test: `src/test/java/com/eduverify/platform/shared/domain/model/valueobjects/UserIdTest.java`
- Test: `src/test/java/com/eduverify/platform/shared/infrastructure/security/CurrentUserContextTest.java`
- Test: `src/test/java/com/eduverify/platform/shared/interfaces/rest/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: nothing beyond Task 1's Spring Boot setup.
- Produces: `UserId(UUID value)` record (used by `iam` for `User.id` and by `academicmanagement` for `Exam.teacherId`/`ExamSession.studentId`), `NotFoundException`/`UnauthorizedException` (thrown by later application services), `CurrentUserContext.set(UserId)`/`.get()`/`.clear()` (used by `TokenAuthenticationFilter` in Task 8 and by `ExamsController` in Task 13), `GlobalExceptionHandler` (a `@RestControllerAdvice`, auto-picked-up by Spring, no direct code dependency from other tasks).

- [ ] **Step 1: Write the failing test for `UserId`**

`src/test/java/com/eduverify/platform/shared/domain/model/valueobjects/UserIdTest.java`:

```java
package com.eduverify.platform.shared.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserIdTest {
    @Test
    void generatesRandomValueWhenNoArgConstructorUsed() {
        UserId id = new UserId();
        assertNotNull(id.value());
    }

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new UserId(null));
    }

    @Test
    void toStringReturnsRawUuidString() {
        UUID raw = UUID.randomUUID();
        UserId id = new UserId(raw);
        assertEquals(raw.toString(), id.toString());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=UserIdTest" test
```
Expected: FAIL — `UserId` does not exist (compile error).

- [ ] **Step 3: Implement `UserId`**

`src/main/java/com/eduverify/platform/shared/domain/model/valueobjects/UserId.java`:

```java
package com.eduverify.platform.shared.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {
    public UserId {
        if (Objects.isNull(value))
            throw new IllegalArgumentException("User identifier cannot be null");
    }

    public UserId() {
        this(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: PASS.

- [ ] **Step 5: Write the failing test for `CurrentUserContext`**

`src/test/java/com/eduverify/platform/shared/infrastructure/security/CurrentUserContextTest.java`:

```java
package com.eduverify.platform.shared.infrastructure.security;

import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CurrentUserContextTest {
    @AfterEach
    void cleanUp() {
        CurrentUserContext.clear();
    }

    @Test
    void returnsNullWhenNothingSet() {
        assertNull(CurrentUserContext.get());
    }

    @Test
    void returnsTheUserIdThatWasSet() {
        UserId userId = new UserId();
        CurrentUserContext.set(userId);
        assertEquals(userId, CurrentUserContext.get());
    }

    @Test
    void clearRemovesTheStoredValue() {
        CurrentUserContext.set(new UserId());
        CurrentUserContext.clear();
        assertNull(CurrentUserContext.get());
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=CurrentUserContextTest" test
```
Expected: FAIL — `CurrentUserContext` does not exist.

- [ ] **Step 7: Implement `CurrentUserContext`, `NotFoundException`, `UnauthorizedException`**

`src/main/java/com/eduverify/platform/shared/infrastructure/security/CurrentUserContext.java`:

```java
package com.eduverify.platform.shared.infrastructure.security;

import com.eduverify.platform.shared.domain.model.valueobjects.UserId;

public class CurrentUserContext {
    private static final ThreadLocal<UserId> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(UserId userId) {
        CURRENT_USER.set(userId);
    }

    public static UserId get() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
```

`src/main/java/com/eduverify/platform/shared/domain/exceptions/NotFoundException.java`:

```java
package com.eduverify.platform.shared.domain.exceptions;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
```

`src/main/java/com/eduverify/platform/shared/domain/exceptions/UnauthorizedException.java`:

```java
package com.eduverify.platform.shared.domain.exceptions;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
```

- [ ] **Step 8: Run test to verify it passes**

Run the same command as Step 6.
Expected: PASS.

- [ ] **Step 9: Write the failing test for `GlobalExceptionHandler`**

`src/test/java/com/eduverify/platform/shared/interfaces/rest/GlobalExceptionHandlerTest.java`:

```java
package com.eduverify.platform.shared.interfaces.rest;

import com.eduverify.platform.shared.domain.exceptions.NotFoundException;
import com.eduverify.platform.shared.domain.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsIllegalArgumentExceptionTo400() {
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(new IllegalArgumentException("bad input"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad input", response.getBody().get("message"));
    }

    @Test
    void mapsNotFoundExceptionTo404() {
        ResponseEntity<Map<String, String>> response = handler.handleNotFound(new NotFoundException("missing"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("missing", response.getBody().get("message"));
    }

    @Test
    void mapsUnauthorizedExceptionTo401() {
        ResponseEntity<Map<String, String>> response = handler.handleUnauthorized(new UnauthorizedException("no token"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("no token", response.getBody().get("message"));
    }
}
```

- [ ] **Step 10: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=GlobalExceptionHandlerTest" test
```
Expected: FAIL — `GlobalExceptionHandler` does not exist.

- [ ] **Step 11: Implement `GlobalExceptionHandler`**

`src/main/java/com/eduverify/platform/shared/interfaces/rest/GlobalExceptionHandler.java`:

```java
package com.eduverify.platform.shared.interfaces.rest;

import com.eduverify.platform.shared.domain.exceptions.NotFoundException;
import com.eduverify.platform.shared.domain.exceptions.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }
}
```

- [ ] **Step 12: Run the full test suite to confirm everything so far is green**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" test
```
Expected: `BUILD SUCCESS`, all tests pass.

---

### Task 3: IAM domain — `Role` and `User`

**Files:**
- Create: `src/main/java/com/eduverify/platform/iam/domain/model/valueobjects/Role.java`
- Create: `src/main/java/com/eduverify/platform/iam/domain/model/aggregates/User.java`
- Test: `src/test/java/com/eduverify/platform/iam/domain/model/aggregates/UserTest.java`

**Interfaces:**
- Consumes: `UserId` (Task 2).
- Produces: `Role` enum (`STUDENT`, `TEACHER`) and `User(String email, String passwordHash, Role role)` with getters `getId()`, `getEmail()`, `getPasswordHash()`, `getRole()`, setters `setEmail(String)`, `setPasswordHash(String)` — consumed by Task 4 (repository), Task 6 (service), Task 7 (controller).

- [ ] **Step 1: Write the failing test**

`src/test/java/com/eduverify/platform/iam/domain/model/aggregates/UserTest.java`:

```java
package com.eduverify.platform.iam.domain.model.aggregates;

import com.eduverify.platform.iam.domain.model.valueobjects.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {
    @Test
    void createsUserWithGeneratedId() {
        User user = new User("ana@universidad.edu", "hashed-value", Role.STUDENT);

        assertNotNull(user.getId());
        assertEquals("ana@universidad.edu", user.getEmail());
        assertEquals("hashed-value", user.getPasswordHash());
        assertEquals(Role.STUDENT, user.getRole());
    }

    @Test
    void rejectsBlankEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> new User("   ", "hashed-value", Role.STUDENT));
    }

    @Test
    void rejectsNullPasswordHash() {
        assertThrows(IllegalArgumentException.class,
                () -> new User("ana@universidad.edu", null, Role.STUDENT));
    }

    @Test
    void rejectsNullRole() {
        assertThrows(IllegalArgumentException.class,
                () -> new User("ana@universidad.edu", "hashed-value", null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=UserTest" test
```
Expected: FAIL — `User`/`Role` do not exist.

- [ ] **Step 3: Implement `Role` and `User`**

`src/main/java/com/eduverify/platform/iam/domain/model/valueobjects/Role.java`:

```java
package com.eduverify.platform.iam.domain.model.valueobjects;

public enum Role {
    STUDENT,
    TEACHER
}
```

`src/main/java/com/eduverify/platform/iam/domain/model/aggregates/User.java`:

```java
package com.eduverify.platform.iam.domain.model.aggregates;

import com.eduverify.platform.iam.domain.model.valueobjects.Role;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Objects;

@Getter
public class User {
    private final UserId id;
    @Setter @NonNull private String email;
    @Setter @NonNull private String passwordHash;
    private final Role role;

    public User(String email, String passwordHash, Role role) {
        if (Objects.isNull(email) || email.isBlank())
            throw new IllegalArgumentException("User email cannot be null or blank");
        if (Objects.isNull(passwordHash) || passwordHash.isBlank())
            throw new IllegalArgumentException("User password hash cannot be null or blank");
        if (Objects.isNull(role))
            throw new IllegalArgumentException("User role cannot be null");

        this.id = new UserId();
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: PASS.

---

### Task 4: IAM persistence — `UserRepository` + `InMemoryUserRepository`

**Files:**
- Create: `src/main/java/com/eduverify/platform/iam/domain/services/UserRepository.java`
- Create: `src/main/java/com/eduverify/platform/iam/infrastructure/persistence/InMemoryUserRepository.java`
- Test: `src/test/java/com/eduverify/platform/iam/infrastructure/persistence/InMemoryUserRepositoryTest.java`

**Interfaces:**
- Consumes: `User`, `Role` (Task 3), `UserId` (Task 2).
- Produces: `UserRepository` interface (`save`, `findById`, `findByEmail`, `existsByEmail`) implemented by the Spring `@Repository` bean `InMemoryUserRepository` — consumed by Task 6's `AuthenticationServiceImpl`.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/eduverify/platform/iam/infrastructure/persistence/InMemoryUserRepositoryTest.java`:

```java
package com.eduverify.platform.iam.infrastructure.persistence;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.iam.domain.model.valueobjects.Role;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryUserRepositoryTest {
    private final InMemoryUserRepository repository = new InMemoryUserRepository();

    @Test
    void savesAndFindsUserById() {
        User user = new User("ana@universidad.edu", "hash", Role.STUDENT);
        repository.save(user);

        assertTrue(repository.findById(user.getId()).isPresent());
        assertEquals(user.getEmail(), repository.findById(user.getId()).get().getEmail());
    }

    @Test
    void findsUserByEmailCaseInsensitive() {
        User user = new User("Ana@Universidad.edu", "hash", Role.STUDENT);
        repository.save(user);

        assertTrue(repository.findByEmail("ana@universidad.edu").isPresent());
    }

    @Test
    void existsByEmailReflectsSavedUsers() {
        assertFalse(repository.existsByEmail("ana@universidad.edu"));

        repository.save(new User("ana@universidad.edu", "hash", Role.STUDENT));

        assertTrue(repository.existsByEmail("ana@universidad.edu"));
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        assertTrue(repository.findById(new UserId()).isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=InMemoryUserRepositoryTest" test
```
Expected: FAIL — `InMemoryUserRepository` does not exist.

- [ ] **Step 3: Implement `UserRepository` and `InMemoryUserRepository`**

`src/main/java/com/eduverify/platform/iam/domain/services/UserRepository.java`:

```java
package com.eduverify.platform.iam.domain.services;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

`src/main/java/com/eduverify/platform/iam/infrastructure/persistence/InMemoryUserRepository.java`:

```java
package com.eduverify.platform.iam.infrastructure.persistence;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.iam.domain.services.UserRepository;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<UserId, User> usersById = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        usersById.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return usersById.values().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: PASS.

---

### Task 5: IAM security — `PasswordHasher` + `TokenStore`

**Files:**
- Create: `src/main/java/com/eduverify/platform/iam/infrastructure/security/PasswordHasher.java`
- Create: `src/main/java/com/eduverify/platform/iam/infrastructure/security/TokenStore.java`
- Test: `src/test/java/com/eduverify/platform/iam/infrastructure/security/PasswordHasherTest.java`
- Test: `src/test/java/com/eduverify/platform/iam/infrastructure/security/TokenStoreTest.java`

**Interfaces:**
- Consumes: `UserId` (Task 2).
- Produces: `PasswordHasher.hash(String)`/`.matches(String, String)` and `TokenStore.issue(UserId)`/`.resolve(String)`/`.revoke(String)`, both Spring `@Component` beans — consumed by Task 6's `AuthenticationServiceImpl` and Task 8's `TokenAuthenticationFilter`.

- [ ] **Step 1: Write the failing test for `PasswordHasher`**

`src/test/java/com/eduverify/platform/iam/infrastructure/security/PasswordHasherTest.java`:

```java
package com.eduverify.platform.iam.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void sameInputAlwaysProducesSameHash() {
        assertEquals(hasher.hash("Test1234!"), hasher.hash("Test1234!"));
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        assertNotEquals(hasher.hash("Test1234!"), hasher.hash("Different1!"));
    }

    @Test
    void matchesReturnsTrueForCorrectPassword() {
        String hashed = hasher.hash("Test1234!");
        assertTrue(hasher.matches("Test1234!", hashed));
    }

    @Test
    void matchesReturnsFalseForWrongPassword() {
        String hashed = hasher.hash("Test1234!");
        assertFalse(hasher.matches("wrong-password", hashed));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=PasswordHasherTest" test
```
Expected: FAIL — `PasswordHasher` does not exist.

- [ ] **Step 3: Implement `PasswordHasher`**

`src/main/java/com/eduverify/platform/iam/infrastructure/security/PasswordHasher.java`:

```java
package com.eduverify.platform.iam.infrastructure.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class PasswordHasher {

    public String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public boolean matches(String rawPassword, String hashedPassword) {
        return hash(rawPassword).equals(hashedPassword);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: PASS.

- [ ] **Step 5: Write the failing test for `TokenStore`**

`src/test/java/com/eduverify/platform/iam/infrastructure/security/TokenStoreTest.java`:

```java
package com.eduverify.platform.iam.infrastructure.security;

import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenStoreTest {
    private final TokenStore tokenStore = new TokenStore();

    @Test
    void issuedTokenResolvesToTheSameUser() {
        UserId userId = new UserId();
        String token = tokenStore.issue(userId);

        assertEquals(userId, tokenStore.resolve(token).orElseThrow());
    }

    @Test
    void unknownTokenResolvesToEmpty() {
        assertTrue(tokenStore.resolve("unknown-token").isEmpty());
    }

    @Test
    void revokedTokenNoLongerResolves() {
        UserId userId = new UserId();
        String token = tokenStore.issue(userId);

        tokenStore.revoke(token);

        assertTrue(tokenStore.resolve(token).isEmpty());
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=TokenStoreTest" test
```
Expected: FAIL — `TokenStore` does not exist.

- [ ] **Step 7: Implement `TokenStore`**

`src/main/java/com/eduverify/platform/iam/infrastructure/security/TokenStore.java`:

```java
package com.eduverify.platform.iam.infrastructure.security;

import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {
    private final Map<String, UserId> tokensToUsers = new ConcurrentHashMap<>();

    public String issue(UserId userId) {
        String token = UUID.randomUUID().toString();
        tokensToUsers.put(token, userId);
        return token;
    }

    public Optional<UserId> resolve(String token) {
        return Optional.ofNullable(tokensToUsers.get(token));
    }

    public void revoke(String token) {
        tokensToUsers.remove(token);
    }
}
```

- [ ] **Step 8: Run test to verify it passes**

Run the same command as Step 6.
Expected: PASS.

---

### Task 6: IAM application — `AuthenticationService`

**Files:**
- Create: `src/main/java/com/eduverify/platform/iam/domain/services/AuthenticatedUser.java`
- Create: `src/main/java/com/eduverify/platform/iam/domain/services/AuthenticationService.java`
- Create: `src/main/java/com/eduverify/platform/iam/application/AuthenticationServiceImpl.java`
- Test: `src/test/java/com/eduverify/platform/iam/application/AuthenticationServiceImplTest.java`

**Interfaces:**
- Consumes: `User`, `Role` (Task 3), `UserRepository`/`InMemoryUserRepository` (Task 4), `PasswordHasher`, `TokenStore` (Task 5), `UnauthorizedException` (Task 2).
- Produces: `AuthenticatedUser(User user, String token)` record and `AuthenticationService` interface with `register(String email, String rawPassword, Role role): User` and `login(String email, String rawPassword): AuthenticatedUser`, implemented by the `@Service` bean `AuthenticationServiceImpl` — consumed by Task 7's `AuthController`.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/eduverify/platform/iam/application/AuthenticationServiceImplTest.java`:

```java
package com.eduverify.platform.iam.application;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.iam.domain.model.valueobjects.Role;
import com.eduverify.platform.iam.domain.services.AuthenticatedUser;
import com.eduverify.platform.iam.infrastructure.persistence.InMemoryUserRepository;
import com.eduverify.platform.iam.infrastructure.security.PasswordHasher;
import com.eduverify.platform.iam.infrastructure.security.TokenStore;
import com.eduverify.platform.shared.domain.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationServiceImplTest {
    private AuthenticationServiceImpl service;
    private InMemoryUserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        service = new AuthenticationServiceImpl(userRepository, new PasswordHasher(), new TokenStore());
    }

    @Test
    void registersNewUser() {
        User user = service.register("ana@universidad.edu", "Test1234!", Role.STUDENT);

        assertNotNull(user.getId());
        assertTrue(userRepository.existsByEmail("ana@universidad.edu"));
    }

    @Test
    void rejectsRegisteringDuplicateEmail() {
        service.register("ana@universidad.edu", "Test1234!", Role.STUDENT);

        assertThrows(IllegalArgumentException.class,
                () -> service.register("ana@universidad.edu", "Other123!", Role.TEACHER));
    }

    @Test
    void logsInWithCorrectCredentials() {
        service.register("ana@universidad.edu", "Test1234!", Role.STUDENT);

        AuthenticatedUser authenticatedUser = service.login("ana@universidad.edu", "Test1234!");

        assertNotNull(authenticatedUser.token());
        assertEquals("ana@universidad.edu", authenticatedUser.user().getEmail());
    }

    @Test
    void rejectsLoginWithWrongPassword() {
        service.register("ana@universidad.edu", "Test1234!", Role.STUDENT);

        assertThrows(UnauthorizedException.class,
                () -> service.login("ana@universidad.edu", "wrong-password"));
    }

    @Test
    void rejectsLoginForUnknownEmail() {
        assertThrows(UnauthorizedException.class,
                () -> service.login("unknown@universidad.edu", "Test1234!"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=AuthenticationServiceImplTest" test
```
Expected: FAIL — `AuthenticationServiceImpl` does not exist.

- [ ] **Step 3: Implement `AuthenticatedUser`, `AuthenticationService`, `AuthenticationServiceImpl`**

`src/main/java/com/eduverify/platform/iam/domain/services/AuthenticatedUser.java`:

```java
package com.eduverify.platform.iam.domain.services;

import com.eduverify.platform.iam.domain.model.aggregates.User;

public record AuthenticatedUser(User user, String token) {
}
```

`src/main/java/com/eduverify/platform/iam/domain/services/AuthenticationService.java`:

```java
package com.eduverify.platform.iam.domain.services;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.iam.domain.model.valueobjects.Role;

public interface AuthenticationService {
    User register(String email, String rawPassword, Role role);
    AuthenticatedUser login(String email, String rawPassword);
}
```

`src/main/java/com/eduverify/platform/iam/application/AuthenticationServiceImpl.java`:

```java
package com.eduverify.platform.iam.application;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.iam.domain.model.valueobjects.Role;
import com.eduverify.platform.iam.domain.services.AuthenticatedUser;
import com.eduverify.platform.iam.domain.services.AuthenticationService;
import com.eduverify.platform.iam.domain.services.UserRepository;
import com.eduverify.platform.iam.infrastructure.security.PasswordHasher;
import com.eduverify.platform.iam.infrastructure.security.TokenStore;
import com.eduverify.platform.shared.domain.exceptions.UnauthorizedException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenStore tokenStore;

    public AuthenticationServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher, TokenStore tokenStore) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenStore = tokenStore;
    }

    @Override
    public User register(String email, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email))
            throw new IllegalArgumentException("Email is already registered: " + email);

        User user = new User(email, passwordHasher.hash(rawPassword), role);
        return userRepository.save(user);
    }

    @Override
    public AuthenticatedUser login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordHasher.matches(rawPassword, user.getPasswordHash()))
            throw new UnauthorizedException("Invalid email or password");

        String token = tokenStore.issue(user.getId());
        return new AuthenticatedUser(user, token);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: PASS.

---

### Task 7: IAM interfaces — DTOs + `AuthController`

**Files:**
- Create: `src/main/java/com/eduverify/platform/iam/interfaces/rest/resources/RegisterResource.java`
- Create: `src/main/java/com/eduverify/platform/iam/interfaces/rest/resources/LoginResource.java`
- Create: `src/main/java/com/eduverify/platform/iam/interfaces/rest/resources/UserResource.java`
- Create: `src/main/java/com/eduverify/platform/iam/interfaces/rest/resources/AuthenticatedUserResource.java`
- Create: `src/main/java/com/eduverify/platform/iam/interfaces/rest/AuthController.java`
- Test: `src/test/java/com/eduverify/platform/iam/interfaces/rest/AuthControllerTest.java`

**Interfaces:**
- Consumes: `AuthenticationService` (Task 6), `Role`, `User` (Task 3).
- Produces: `POST /api/iam/register` and `POST /api/iam/login` HTTP endpoints — consumed (via HTTP, not code) by Task 13's end-to-end test and eventually by the React front-end.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/eduverify/platform/iam/interfaces/rest/AuthControllerTest.java`:

```java
package com.eduverify.platform.iam.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@universidad.edu";
    }

    @Test
    void registerReturnsCreatedUser() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Test1234!",
                                "role", "student"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void registerRejectsUnknownRole() throws Exception {
        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", uniqueEmail(),
                                "password", "Test1234!",
                                "role", "admin"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        Map<String, String> body = Map.of("email", email, "password", "Test1234!", "role", "teacher");

        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsTokenForCorrectCredentials() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Test1234!", "role", "student"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/iam/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Test1234!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Test1234!", "role", "student"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/iam/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "wrong-password"
                        ))))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=AuthControllerTest" test
```
Expected: FAIL — `AuthController` does not exist / route 404.

- [ ] **Step 3: Implement the resources and `AuthController`**

`src/main/java/com/eduverify/platform/iam/interfaces/rest/resources/RegisterResource.java`:

```java
package com.eduverify.platform.iam.interfaces.rest.resources;

public record RegisterResource(String email, String password, String role) {
}
```

`src/main/java/com/eduverify/platform/iam/interfaces/rest/resources/LoginResource.java`:

```java
package com.eduverify.platform.iam.interfaces.rest.resources;

public record LoginResource(String email, String password) {
}
```

`src/main/java/com/eduverify/platform/iam/interfaces/rest/resources/UserResource.java`:

```java
package com.eduverify.platform.iam.interfaces.rest.resources;

public record UserResource(String id, String email, String role) {
}
```

`src/main/java/com/eduverify/platform/iam/interfaces/rest/resources/AuthenticatedUserResource.java`:

```java
package com.eduverify.platform.iam.interfaces.rest.resources;

public record AuthenticatedUserResource(String token, UserResource user) {
}
```

`src/main/java/com/eduverify/platform/iam/interfaces/rest/AuthController.java`:

```java
package com.eduverify.platform.iam.interfaces.rest;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.iam.domain.model.valueobjects.Role;
import com.eduverify.platform.iam.domain.services.AuthenticatedUser;
import com.eduverify.platform.iam.domain.services.AuthenticationService;
import com.eduverify.platform.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.eduverify.platform.iam.interfaces.rest.resources.LoginResource;
import com.eduverify.platform.iam.interfaces.rest.resources.RegisterResource;
import com.eduverify.platform.iam.interfaces.rest.resources.UserResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iam")
public class AuthController {
    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResource> register(@RequestBody RegisterResource resource) {
        Role role = parseRole(resource.role());
        User user = authenticationService.register(resource.email(), resource.password(), role);
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResource(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticatedUserResource> login(@RequestBody LoginResource resource) {
        AuthenticatedUser authenticatedUser = authenticationService.login(resource.email(), resource.password());
        return ResponseEntity.ok(new AuthenticatedUserResource(
                authenticatedUser.token(),
                toUserResource(authenticatedUser.user())
        ));
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank())
            throw new IllegalArgumentException("Role cannot be null or blank");
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown role: " + role);
        }
    }

    private UserResource toUserResource(User user) {
        return new UserResource(user.getId().toString(), user.getEmail(), user.getRole().name());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: PASS.

---

### Task 8: IAM security — `TokenAuthenticationFilter`

**Files:**
- Create: `src/main/java/com/eduverify/platform/iam/infrastructure/security/TokenAuthenticationFilter.java`
- Test: `src/test/java/com/eduverify/platform/iam/infrastructure/security/TokenAuthenticationFilterTest.java`

**Interfaces:**
- Consumes: `TokenStore` (Task 5), `CurrentUserContext`, `UserId` (Task 2).
- Produces: a Spring `@Component` `OncePerRequestFilter` auto-registered by Spring Boot against all routes, exempting `POST /api/iam/register` and `POST /api/iam/login`. Populates `CurrentUserContext` for the duration of each authenticated request. Consumed implicitly (via the servlet filter chain, not direct code coupling) by Task 13's `ExamsController` routes.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/eduverify/platform/iam/infrastructure/security/TokenAuthenticationFilterTest.java`:

```java
package com.eduverify.platform.iam.infrastructure.security;

import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import com.eduverify.platform.shared.infrastructure.security.CurrentUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TokenAuthenticationFilterTest {
    private final TokenStore tokenStore = new TokenStore();
    private final TokenAuthenticationFilter filter = new TokenAuthenticationFilter(tokenStore);

    @Test
    void allowsExemptPathsWithoutToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/iam/login");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(response);
    }

    @Test
    void rejectsProtectedPathWithoutToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/exams");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void rejectsProtectedPathWithUnknownToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/exams");
        when(request.getHeader("Authorization")).thenReturn("Bearer unknown-token");
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void allowsProtectedPathWithValidTokenAndSetsCurrentUser() throws Exception {
        UserId userId = new UserId();
        String token = tokenStore.issue(userId);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/exams");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        doAnswer(invocation -> {
            assertEquals(userId, CurrentUserContext.get());
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(CurrentUserContext.get());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=TokenAuthenticationFilterTest" test
```
Expected: FAIL — `TokenAuthenticationFilter` does not exist.

- [ ] **Step 3: Implement `TokenAuthenticationFilter`**

`src/main/java/com/eduverify/platform/iam/infrastructure/security/TokenAuthenticationFilter.java`:

```java
package com.eduverify.platform.iam.infrastructure.security;

import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import com.eduverify.platform.shared.infrastructure.security.CurrentUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private static final Set<String> EXEMPT_PATHS = Set.of("/api/iam/register", "/api/iam/login");

    private final TokenStore tokenStore;

    public TokenAuthenticationFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (EXEMPT_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<UserId> userId = extractToken(request).flatMap(tokenStore::resolve);

        if (userId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Missing or invalid authentication token\"}");
            return;
        }

        try {
            CurrentUserContext.set(userId.get());
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
        }
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            return Optional.empty();
        return Optional.of(header.substring("Bearer ".length()));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: PASS.

- [ ] **Step 5: Run the full test suite (this filter now guards everything except register/login — re-confirm Task 7's tests still pass since they only hit exempt routes)**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" test
```
Expected: `BUILD SUCCESS`, all tests pass.

---

### Task 9: Academic Management value objects

**Files:**
- Create: `src/main/java/com/eduverify/platform/academicmanagement/domain/model/valueobjects/ExamId.java`
- Create: `src/main/java/com/eduverify/platform/academicmanagement/domain/model/valueobjects/ExamSessionId.java`
- Create: `src/main/java/com/eduverify/platform/academicmanagement/domain/model/valueobjects/ExamStatus.java`
- Create: `src/main/java/com/eduverify/platform/academicmanagement/domain/model/valueobjects/ExamSessionStatus.java`
- Test: `src/test/java/com/eduverify/platform/academicmanagement/domain/model/valueobjects/ExamIdTest.java`
- Test: `src/test/java/com/eduverify/platform/academicmanagement/domain/model/valueobjects/ExamSessionIdTest.java`

**Interfaces:**
- Consumes: nothing beyond Task 1.
- Produces: `ExamId(UUID value)`, `ExamSessionId(UUID value)` records; `ExamStatus` enum (`SCHEDULED`, `IN_PROGRESS`, `FINISHED`); `ExamSessionStatus` enum (`IN_PROGRESS`, `FINISHED`) — consumed by Task 10's `Exam`/`ExamSession` aggregates.

- [ ] **Step 1: Write the failing tests**

`src/test/java/com/eduverify/platform/academicmanagement/domain/model/valueobjects/ExamIdTest.java`:

```java
package com.eduverify.platform.academicmanagement.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamIdTest {
    @Test
    void generatesRandomValueWhenNoArgConstructorUsed() {
        assertNotNull(new ExamId().value());
    }

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new ExamId(null));
    }

    @Test
    void toStringReturnsRawUuidString() {
        UUID raw = UUID.randomUUID();
        assertEquals(raw.toString(), new ExamId(raw).toString());
    }
}
```

`src/test/java/com/eduverify/platform/academicmanagement/domain/model/valueobjects/ExamSessionIdTest.java`:

```java
package com.eduverify.platform.academicmanagement.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamSessionIdTest {
    @Test
    void generatesRandomValueWhenNoArgConstructorUsed() {
        assertNotNull(new ExamSessionId().value());
    }

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new ExamSessionId(null));
    }

    @Test
    void toStringReturnsRawUuidString() {
        UUID raw = UUID.randomUUID();
        assertEquals(raw.toString(), new ExamSessionId(raw).toString());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=ExamIdTest,ExamSessionIdTest" test
```
Expected: FAIL — classes don't exist.

- [ ] **Step 3: Implement the value objects**

`src/main/java/com/eduverify/platform/academicmanagement/domain/model/valueobjects/ExamId.java`:

```java
package com.eduverify.platform.academicmanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record ExamId(UUID value) {
    public ExamId {
        if (Objects.isNull(value))
            throw new IllegalArgumentException("Exam identifier cannot be null");
    }

    public ExamId() {
        this(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
```

`src/main/java/com/eduverify/platform/academicmanagement/domain/model/valueobjects/ExamSessionId.java`:

```java
package com.eduverify.platform.academicmanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record ExamSessionId(UUID value) {
    public ExamSessionId {
        if (Objects.isNull(value))
            throw new IllegalArgumentException("Exam session identifier cannot be null");
    }

    public ExamSessionId() {
        this(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
```

`src/main/java/com/eduverify/platform/academicmanagement/domain/model/valueobjects/ExamStatus.java`:

```java
package com.eduverify.platform.academicmanagement.domain.model.valueobjects;

public enum ExamStatus {
    SCHEDULED,
    IN_PROGRESS,
    FINISHED
}
```

`src/main/java/com/eduverify/platform/academicmanagement/domain/model/valueobjects/ExamSessionStatus.java`:

```java
package com.eduverify.platform.academicmanagement.domain.model.valueobjects;

public enum ExamSessionStatus {
    IN_PROGRESS,
    FINISHED
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run the same command as Step 2.
Expected: PASS.

---

### Task 10: Academic Management aggregates — `ExamSession` + `Exam`

**Files:**
- Create: `src/main/java/com/eduverify/platform/academicmanagement/domain/model/aggregates/ExamSession.java`
- Create: `src/main/java/com/eduverify/platform/academicmanagement/domain/model/aggregates/Exam.java`
- Test: `src/test/java/com/eduverify/platform/academicmanagement/domain/model/aggregates/ExamTest.java`

**Interfaces:**
- Consumes: `ExamId`, `ExamSessionId`, `ExamStatus`, `ExamSessionStatus` (Task 9), `UserId` (Task 2).
- Produces: `Exam(UserId teacherId, String title, LocalDateTime scheduledDate, int durationMinutes)` with `getId()`, `getTeacherId()`, `getTitle()`/`setTitle(String)`, `getScheduledDate()`, `getDurationMinutes()`, `getStatus()`, `getSessions(): List<ExamSession>`, `start()`, `finish()`, `startSessionFor(UserId studentId): ExamSession`, `finishSession(ExamSessionId sessionId)`. `ExamSession` with `getId()`, `getExamId()`, `getStudentId()`, `getStartTime()`, `getEndTime()`, `getStatus()` (package-private constructor and `finish()`, only usable from `Exam`, same package). Consumed by Task 11 (repository) and Task 12 (service).

- [ ] **Step 1: Write the failing test**

`src/test/java/com/eduverify/platform/academicmanagement/domain/model/aggregates/ExamTest.java`:

```java
package com.eduverify.platform.academicmanagement.domain.model.aggregates;

import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionStatus;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamStatus;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamTest {
    private final UserId teacherId = new UserId();

    private Exam newExam() {
        return new Exam(teacherId, "Calculo Diferencial - Parcial 2", LocalDateTime.now().plusDays(1), 90);
    }

    @Test
    void createsExamInScheduledStatus() {
        Exam exam = newExam();

        assertNotNull(exam.getId());
        assertEquals(ExamStatus.SCHEDULED, exam.getStatus());
        assertTrue(exam.getSessions().isEmpty());
    }

    @Test
    void rejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> new Exam(teacherId, "  ", LocalDateTime.now(), 90));
    }

    @Test
    void rejectsNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> new Exam(teacherId, "Quiz", LocalDateTime.now(), 0));
    }

    @Test
    void startMovesFromScheduledToInProgress() {
        Exam exam = newExam();
        exam.start();
        assertEquals(ExamStatus.IN_PROGRESS, exam.getStatus());
    }

    @Test
    void startFailsWhenNotScheduled() {
        Exam exam = newExam();
        exam.start();
        assertThrows(IllegalStateException.class, exam::start);
    }

    @Test
    void finishMovesFromInProgressToFinished() {
        Exam exam = newExam();
        exam.start();
        exam.finish();
        assertEquals(ExamStatus.FINISHED, exam.getStatus());
    }

    @Test
    void finishFailsWhenNotInProgress() {
        Exam exam = newExam();
        assertThrows(IllegalStateException.class, exam::finish);
    }

    @Test
    void startSessionForAddsSessionForStudent() {
        Exam exam = newExam();
        UserId studentId = new UserId();

        ExamSession session = exam.startSessionFor(studentId);

        assertEquals(1, exam.getSessions().size());
        assertEquals(studentId, session.getStudentId());
        assertEquals(exam.getId(), session.getExamId());
        assertEquals(ExamSessionStatus.IN_PROGRESS, session.getStatus());
    }

    @Test
    void finishSessionMarksMatchingSessionAsFinished() {
        Exam exam = newExam();
        ExamSession session = exam.startSessionFor(new UserId());

        exam.finishSession(session.getId());

        assertEquals(ExamSessionStatus.FINISHED, session.getStatus());
        assertNotNull(session.getEndTime());
    }

    @Test
    void finishSessionThrowsWhenSessionIdUnknown() {
        Exam exam = newExam();
        assertThrows(IllegalArgumentException.class, () -> exam.finishSession(new ExamSessionId()));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=ExamTest" test
```
Expected: FAIL — `Exam`/`ExamSession` do not exist.

- [ ] **Step 3: Implement `ExamSession` and `Exam`**

`src/main/java/com/eduverify/platform/academicmanagement/domain/model/aggregates/ExamSession.java`:

```java
package com.eduverify.platform.academicmanagement.domain.model.aggregates;

import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionStatus;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import lombok.Getter;
import lombok.NonNull;

import java.time.LocalDateTime;

@Getter
public class ExamSession {
    private final ExamSessionId id;
    private final ExamId examId;
    private final UserId studentId;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private ExamSessionStatus status;

    ExamSession(@NonNull ExamId examId, @NonNull UserId studentId) {
        this.id = new ExamSessionId();
        this.examId = examId;
        this.studentId = studentId;
        this.startTime = LocalDateTime.now();
        this.status = ExamSessionStatus.IN_PROGRESS;
    }

    void finish() {
        if (status == ExamSessionStatus.FINISHED)
            throw new IllegalStateException("Exam session is already finished");
        this.status = ExamSessionStatus.FINISHED;
        this.endTime = LocalDateTime.now();
    }
}
```

`src/main/java/com/eduverify/platform/academicmanagement/domain/model/aggregates/Exam.java`:

```java
package com.eduverify.platform.academicmanagement.domain.model.aggregates;

import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamStatus;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class Exam {
    private final ExamId id;
    private final UserId teacherId;
    @Setter @NonNull private String title;
    private final LocalDateTime scheduledDate;
    private final int durationMinutes;
    private ExamStatus status;
    private final List<ExamSession> sessions;

    public Exam(@NonNull UserId teacherId, String title, @NonNull LocalDateTime scheduledDate, int durationMinutes) {
        if (Objects.isNull(title) || title.isBlank())
            throw new IllegalArgumentException("Exam title cannot be null or blank");
        if (durationMinutes <= 0)
            throw new IllegalArgumentException("Exam duration must be greater than zero");

        this.id = new ExamId();
        this.teacherId = teacherId;
        this.title = title;
        this.scheduledDate = scheduledDate;
        this.durationMinutes = durationMinutes;
        this.status = ExamStatus.SCHEDULED;
        this.sessions = new ArrayList<>();
    }

    public void start() {
        if (status != ExamStatus.SCHEDULED)
            throw new IllegalStateException("Only a scheduled exam can be started");
        this.status = ExamStatus.IN_PROGRESS;
    }

    public void finish() {
        if (status != ExamStatus.IN_PROGRESS)
            throw new IllegalStateException("Only an in-progress exam can be finished");
        this.status = ExamStatus.FINISHED;
    }

    public ExamSession startSessionFor(@NonNull UserId studentId) {
        ExamSession session = new ExamSession(this.id, studentId);
        this.sessions.add(session);
        return session;
    }

    public void finishSession(@NonNull ExamSessionId sessionId) {
        ExamSession session = sessions.stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No session found with id: " + sessionId));
        session.finish();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: PASS.

---

### Task 11: Academic Management persistence — `ExamRepository` + `InMemoryExamRepository`

**Files:**
- Create: `src/main/java/com/eduverify/platform/academicmanagement/domain/services/ExamRepository.java`
- Create: `src/main/java/com/eduverify/platform/academicmanagement/infrastructure/persistence/InMemoryExamRepository.java`
- Test: `src/test/java/com/eduverify/platform/academicmanagement/infrastructure/persistence/InMemoryExamRepositoryTest.java`

**Interfaces:**
- Consumes: `Exam` (Task 10), `ExamId` (Task 9), `UserId` (Task 2).
- Produces: `ExamRepository` interface (`save`, `findById`, `findAll`) implemented by the `@Repository` bean `InMemoryExamRepository` — consumed by Task 12's `ExamServiceImpl`.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/eduverify/platform/academicmanagement/infrastructure/persistence/InMemoryExamRepositoryTest.java`:

```java
package com.eduverify.platform.academicmanagement.infrastructure.persistence;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryExamRepositoryTest {
    private final InMemoryExamRepository repository = new InMemoryExamRepository();

    private Exam newExam() {
        return new Exam(new UserId(), "Quiz", LocalDateTime.now().plusDays(1), 45);
    }

    @Test
    void savesAndFindsExamById() {
        Exam exam = newExam();
        repository.save(exam);

        assertTrue(repository.findById(exam.getId()).isPresent());
        assertEquals(exam.getTitle(), repository.findById(exam.getId()).get().getTitle());
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        assertTrue(repository.findById(new ExamId()).isEmpty());
    }

    @Test
    void findAllReturnsAllSavedExams() {
        repository.save(newExam());
        repository.save(newExam());

        assertEquals(2, repository.findAll().size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=InMemoryExamRepositoryTest" test
```
Expected: FAIL — `InMemoryExamRepository` does not exist.

- [ ] **Step 3: Implement `ExamRepository` and `InMemoryExamRepository`**

`src/main/java/com/eduverify/platform/academicmanagement/domain/services/ExamRepository.java`:

```java
package com.eduverify.platform.academicmanagement.domain.services;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;

import java.util.List;
import java.util.Optional;

public interface ExamRepository {
    Exam save(Exam exam);
    Optional<Exam> findById(ExamId id);
    List<Exam> findAll();
}
```

`src/main/java/com/eduverify/platform/academicmanagement/infrastructure/persistence/InMemoryExamRepository.java`:

```java
package com.eduverify.platform.academicmanagement.infrastructure.persistence;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.services.ExamRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryExamRepository implements ExamRepository {
    private final Map<ExamId, Exam> examsById = new ConcurrentHashMap<>();

    @Override
    public Exam save(Exam exam) {
        examsById.put(exam.getId(), exam);
        return exam;
    }

    @Override
    public Optional<Exam> findById(ExamId id) {
        return Optional.ofNullable(examsById.get(id));
    }

    @Override
    public List<Exam> findAll() {
        return new ArrayList<>(examsById.values());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: PASS.

---

### Task 12: Academic Management application — `ExamService`

**Files:**
- Create: `src/main/java/com/eduverify/platform/academicmanagement/domain/services/ExamService.java`
- Create: `src/main/java/com/eduverify/platform/academicmanagement/application/ExamServiceImpl.java`
- Test: `src/test/java/com/eduverify/platform/academicmanagement/application/ExamServiceImplTest.java`

**Interfaces:**
- Consumes: `Exam`, `ExamSession` (Task 10), `ExamId`, `ExamSessionId` (Task 9), `ExamRepository`/`InMemoryExamRepository` (Task 11), `NotFoundException` (Task 2), `UserId` (Task 2).
- Produces: `ExamService` interface with `createExam`, `listExams`, `getExam`, `startExam`, `finishExam`, `accessExam`, `submitExam`, implemented by the `@Service` bean `ExamServiceImpl` — consumed by Task 13's `ExamsController`.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/eduverify/platform/academicmanagement/application/ExamServiceImplTest.java`:

```java
package com.eduverify.platform.academicmanagement.application;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.aggregates.ExamSession;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamStatus;
import com.eduverify.platform.academicmanagement.infrastructure.persistence.InMemoryExamRepository;
import com.eduverify.platform.shared.domain.exceptions.NotFoundException;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamServiceImplTest {
    private ExamServiceImpl service;
    private final UserId teacherId = new UserId();

    @BeforeEach
    void setUp() {
        service = new ExamServiceImpl(new InMemoryExamRepository());
    }

    @Test
    void createExamPersistsAndReturnsIt() {
        Exam exam = service.createExam(teacherId, "Quiz", LocalDateTime.now().plusDays(1), 45);

        assertEquals(exam.getId(), service.getExam(exam.getId()).getId());
    }

    @Test
    void listExamsReturnsAllCreatedExams() {
        service.createExam(teacherId, "Quiz 1", LocalDateTime.now().plusDays(1), 45);
        service.createExam(teacherId, "Quiz 2", LocalDateTime.now().plusDays(2), 60);

        assertEquals(2, service.listExams().size());
    }

    @Test
    void getExamThrowsNotFoundForUnknownId() {
        assertThrows(NotFoundException.class, () -> service.getExam(new ExamId()));
    }

    @Test
    void startExamMovesItToInProgress() {
        Exam exam = service.createExam(teacherId, "Quiz", LocalDateTime.now().plusDays(1), 45);

        Exam started = service.startExam(exam.getId());

        assertEquals(ExamStatus.IN_PROGRESS, started.getStatus());
    }

    @Test
    void finishExamMovesItToFinished() {
        Exam exam = service.createExam(teacherId, "Quiz", LocalDateTime.now().plusDays(1), 45);
        service.startExam(exam.getId());

        Exam finished = service.finishExam(exam.getId());

        assertEquals(ExamStatus.FINISHED, finished.getStatus());
    }

    @Test
    void accessExamCreatesASessionForTheStudent() {
        Exam exam = service.createExam(teacherId, "Quiz", LocalDateTime.now().plusDays(1), 45);
        UserId studentId = new UserId();

        ExamSession session = service.accessExam(exam.getId(), studentId);

        assertEquals(studentId, session.getStudentId());
        assertEquals(1, service.getExam(exam.getId()).getSessions().size());
    }

    @Test
    void submitExamFinishesTheMatchingSession() {
        Exam exam = service.createExam(teacherId, "Quiz", LocalDateTime.now().plusDays(1), 45);
        ExamSession session = service.accessExam(exam.getId(), new UserId());

        service.submitExam(exam.getId(), session.getId());

        assertEquals(
                com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionStatus.FINISHED,
                service.getExam(exam.getId()).getSessions().get(0).getStatus()
        );
    }

    @Test
    void submitExamThrowsForUnknownSession() {
        Exam exam = service.createExam(teacherId, "Quiz", LocalDateTime.now().plusDays(1), 45);

        assertThrows(IllegalArgumentException.class,
                () -> service.submitExam(exam.getId(), new ExamSessionId()));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=ExamServiceImplTest" test
```
Expected: FAIL — `ExamServiceImpl` does not exist.

- [ ] **Step 3: Implement `ExamService` and `ExamServiceImpl`**

`src/main/java/com/eduverify/platform/academicmanagement/domain/services/ExamService.java`:

```java
package com.eduverify.platform.academicmanagement.domain.services;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.aggregates.ExamSession;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;

import java.time.LocalDateTime;
import java.util.List;

public interface ExamService {
    Exam createExam(UserId teacherId, String title, LocalDateTime scheduledDate, int durationMinutes);
    List<Exam> listExams();
    Exam getExam(ExamId examId);
    Exam startExam(ExamId examId);
    Exam finishExam(ExamId examId);
    ExamSession accessExam(ExamId examId, UserId studentId);
    Exam submitExam(ExamId examId, ExamSessionId sessionId);
}
```

`src/main/java/com/eduverify/platform/academicmanagement/application/ExamServiceImpl.java`:

```java
package com.eduverify.platform.academicmanagement.application;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.aggregates.ExamSession;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.academicmanagement.domain.services.ExamRepository;
import com.eduverify.platform.academicmanagement.domain.services.ExamService;
import com.eduverify.platform.shared.domain.exceptions.NotFoundException;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExamServiceImpl implements ExamService {
    private final ExamRepository examRepository;

    public ExamServiceImpl(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Override
    public Exam createExam(UserId teacherId, String title, LocalDateTime scheduledDate, int durationMinutes) {
        Exam exam = new Exam(teacherId, title, scheduledDate, durationMinutes);
        return examRepository.save(exam);
    }

    @Override
    public List<Exam> listExams() {
        return examRepository.findAll();
    }

    @Override
    public Exam getExam(ExamId examId) {
        return findExamOrThrow(examId);
    }

    @Override
    public Exam startExam(ExamId examId) {
        Exam exam = findExamOrThrow(examId);
        exam.start();
        return examRepository.save(exam);
    }

    @Override
    public Exam finishExam(ExamId examId) {
        Exam exam = findExamOrThrow(examId);
        exam.finish();
        return examRepository.save(exam);
    }

    @Override
    public ExamSession accessExam(ExamId examId, UserId studentId) {
        Exam exam = findExamOrThrow(examId);
        ExamSession session = exam.startSessionFor(studentId);
        examRepository.save(exam);
        return session;
    }

    @Override
    public Exam submitExam(ExamId examId, ExamSessionId sessionId) {
        Exam exam = findExamOrThrow(examId);
        exam.finishSession(sessionId);
        return examRepository.save(exam);
    }

    private Exam findExamOrThrow(ExamId examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new NotFoundException("No exam found with id: " + examId));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: PASS.

---

### Task 13: Academic Management interfaces — DTOs + `ExamsController` + end-to-end flow

**Files:**
- Create: `src/main/java/com/eduverify/platform/academicmanagement/interfaces/rest/resources/CreateExamResource.java`
- Create: `src/main/java/com/eduverify/platform/academicmanagement/interfaces/rest/resources/ExamResource.java`
- Create: `src/main/java/com/eduverify/platform/academicmanagement/interfaces/rest/resources/ExamSessionResource.java`
- Create: `src/main/java/com/eduverify/platform/academicmanagement/interfaces/rest/ExamsController.java`
- Test: `src/test/java/com/eduverify/platform/academicmanagement/interfaces/rest/ExamsControllerTest.java`

**Interfaces:**
- Consumes: `ExamService` (Task 12), `CurrentUserContext` (Task 2), `AuthController`'s HTTP endpoints (Task 7, via HTTP in the test), `TokenAuthenticationFilter` (Task 8, via the filter chain).
- Produces: `POST /api/exams`, `GET /api/exams`, `GET /api/exams/{examId}`, `POST /api/exams/{examId}/start`, `POST /api/exams/{examId}/finish`, `POST /api/exams/{examId}/sessions`, `PUT /api/exams/{examId}/sessions/{sessionId}/finish` — the full API surface the React front-end will call.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/eduverify/platform/academicmanagement/interfaces/rest/ExamsControllerTest.java`:

```java
package com.eduverify.platform.academicmanagement.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExamsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String role) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@universidad.edu";

        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Test1234!", "role", role
                        ))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/iam/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Test1234!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
        return (String) body.get("token");
    }

    @Test
    void listExamsWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/exams"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void teacherCreatesExamAndStudentCompletesFullFlow() throws Exception {
        String teacherToken = registerAndLogin("teacher");
        String studentToken = registerAndLogin("student");

        MvcResult createResult = mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Calculo Diferencial - Parcial 2",
                                "scheduledDate", LocalDateTime.now().plusDays(1).toString(),
                                "durationMinutes", 90
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andReturn();

        Map<?, ?> exam = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        String examId = (String) exam.get("id");

        mockMvc.perform(post("/api/exams/" + examId + "/start")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        MvcResult sessionResult = mockMvc.perform(post("/api/exams/" + examId + "/sessions")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn();

        Map<?, ?> session = objectMapper.readValue(sessionResult.getResponse().getContentAsString(), Map.class);
        String sessionId = (String) session.get("id");

        mockMvc.perform(put("/api/exams/" + examId + "/sessions/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionCount").value(1));

        mockMvc.perform(get("/api/exams/" + examId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void gettingUnknownExamReturnsNotFound() throws Exception {
        String token = registerAndLogin("teacher");

        mockMvc.perform(get("/api/exams/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" "-Dtest=ExamsControllerTest" test
```
Expected: FAIL — `ExamsController` does not exist / routes 404 or 401 unexpectedly.

- [ ] **Step 3: Implement the resources and `ExamsController`**

`src/main/java/com/eduverify/platform/academicmanagement/interfaces/rest/resources/CreateExamResource.java`:

```java
package com.eduverify.platform.academicmanagement.interfaces.rest.resources;

public record CreateExamResource(String title, String scheduledDate, int durationMinutes) {
}
```

`src/main/java/com/eduverify/platform/academicmanagement/interfaces/rest/resources/ExamResource.java`:

```java
package com.eduverify.platform.academicmanagement.interfaces.rest.resources;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;

import java.time.LocalDateTime;

public record ExamResource(String id, String teacherId, String title, LocalDateTime scheduledDate,
                            int durationMinutes, String status, int sessionCount) {
    public static ExamResource from(Exam exam) {
        return new ExamResource(
                exam.getId().toString(),
                exam.getTeacherId().toString(),
                exam.getTitle(),
                exam.getScheduledDate(),
                exam.getDurationMinutes(),
                exam.getStatus().name(),
                exam.getSessions().size()
        );
    }
}
```

`src/main/java/com/eduverify/platform/academicmanagement/interfaces/rest/resources/ExamSessionResource.java`:

```java
package com.eduverify.platform.academicmanagement.interfaces.rest.resources;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.ExamSession;

public record ExamSessionResource(String id, String examId, String studentId, String status) {
    public static ExamSessionResource from(ExamSession session) {
        return new ExamSessionResource(
                session.getId().toString(),
                session.getExamId().toString(),
                session.getStudentId().toString(),
                session.getStatus().name()
        );
    }
}
```

`src/main/java/com/eduverify/platform/academicmanagement/interfaces/rest/ExamsController.java`:

```java
package com.eduverify.platform.academicmanagement.interfaces.rest;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.aggregates.ExamSession;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.academicmanagement.domain.services.ExamService;
import com.eduverify.platform.academicmanagement.interfaces.rest.resources.CreateExamResource;
import com.eduverify.platform.academicmanagement.interfaces.rest.resources.ExamResource;
import com.eduverify.platform.academicmanagement.interfaces.rest.resources.ExamSessionResource;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import com.eduverify.platform.shared.infrastructure.security.CurrentUserContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exams")
public class ExamsController {
    private final ExamService examService;

    public ExamsController(ExamService examService) {
        this.examService = examService;
    }

    @PostMapping
    public ResponseEntity<ExamResource> createExam(@RequestBody CreateExamResource resource) {
        UserId teacherId = CurrentUserContext.get();
        Exam exam = examService.createExam(
                teacherId,
                resource.title(),
                LocalDateTime.parse(resource.scheduledDate()),
                resource.durationMinutes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ExamResource.from(exam));
    }

    @GetMapping
    public List<ExamResource> listExams() {
        return examService.listExams().stream().map(ExamResource::from).toList();
    }

    @GetMapping("/{examId}")
    public ExamResource getExam(@PathVariable String examId) {
        return ExamResource.from(examService.getExam(new ExamId(UUID.fromString(examId))));
    }

    @PostMapping("/{examId}/start")
    public ExamResource startExam(@PathVariable String examId) {
        return ExamResource.from(examService.startExam(new ExamId(UUID.fromString(examId))));
    }

    @PostMapping("/{examId}/finish")
    public ExamResource finishExam(@PathVariable String examId) {
        return ExamResource.from(examService.finishExam(new ExamId(UUID.fromString(examId))));
    }

    @PostMapping("/{examId}/sessions")
    public ResponseEntity<ExamSessionResource> accessExam(@PathVariable String examId) {
        UserId studentId = CurrentUserContext.get();
        ExamSession session = examService.accessExam(new ExamId(UUID.fromString(examId)), studentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExamSessionResource.from(session));
    }

    @PutMapping("/{examId}/sessions/{sessionId}/finish")
    public ExamResource submitExam(@PathVariable String examId, @PathVariable String sessionId) {
        return ExamResource.from(examService.submitExam(
                new ExamId(UUID.fromString(examId)),
                new ExamSessionId(UUID.fromString(sessionId))
        ));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: PASS.

- [ ] **Step 5: Run the full test suite**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" test
```
Expected: `BUILD SUCCESS`, every test in the project passes.

---

### Task 14: Document how to run it + manual smoke test

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: nothing new — this documents Tasks 1–13's finished API.
- Produces: run instructions for the student and whoever picks up the front-end integration next.

- [ ] **Step 1: Replace `README.md` contents**

```markdown
# EduVerify API

Backend básico (bounded contexts IAM + Academic Management) para EduVerify, construido sobre el ejemplo DDD del profesor (`com.acme.oop.*`, que se deja intacto como referencia). Ver diseño completo en `docs/superpowers/specs/2026-07-14-basic-backend-design.md` y el plan de implementación en `docs/superpowers/plans/2026-07-14-basic-backend-plan.md`.

## Requisitos

- Java 21 (Temurin recomendado)
- Maven 3.9+

## Correr los tests

```powershell
mvn test
```

## Levantar el servidor

```powershell
mvn spring-boot:run
```

El servidor arranca en `http://localhost:8080`.

## Endpoints

```
POST /api/iam/register                 { "email", "password", "role": "student"|"teacher" }
POST /api/iam/login                    { "email", "password" } -> { "token", "user" }

POST /api/exams                          (teacher, Authorization: Bearer <token>)
GET  /api/exams
GET  /api/exams/{examId}
POST /api/exams/{examId}/start           (teacher)
POST /api/exams/{examId}/finish          (teacher)
POST /api/exams/{examId}/sessions        (student) -> acceder al examen
PUT  /api/exams/{examId}/sessions/{sessionId}/finish   (student) -> entregar examen
```

Persistencia en memoria: los datos se pierden al reiniciar el servidor.
```

- [ ] **Step 2: Run the full test suite one final time**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" test
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Manual smoke test — start the server and hit it once for real**

Run (background, it stays up):
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; & "C:\Users\LM\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -f "C:\Users\LM\Documents\U\Inge Soft 1\Github Repo\Backend\oop-java-main\pom.xml" spring-boot:run
```

In a second shell, once "Started EduverifyApplication" appears in the log:
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/iam/register" -ContentType "application/json" -Body '{"email":"smoke@universidad.edu","password":"Test1234!","role":"teacher"}'
```
Expected: JSON response with `id`, `email`, `role: "TEACHER"`. Stop the server afterward.

---

## Self-Review Notes

- **Spec coverage:** every section of `2026-07-14-basic-backend-design.md` maps to a task — architecture/packages (Tasks 1–2), IAM domain/persistence/security/application/interfaces (Tasks 3–8), Academic Management domain/persistence/application/interfaces (Tasks 9–13), error handling (Task 2's `GlobalExceptionHandler`, used throughout), auth (Tasks 5, 6, 8), testing (every task), README/run instructions (Task 14). No spec section is unaddressed.
- **Placeholder scan:** no TBD/TODO markers; every step has complete, runnable code.
- **Type consistency:** `UserId`, `ExamId`, `ExamSessionId`, `Role`, `ExamStatus`, `ExamSessionStatus`, `User`, `Exam`, `ExamSession`, `AuthenticatedUser` are defined once (Tasks 2, 3, 9, 10, 6) and referenced identically (same package, same method names/signatures) in every later task that consumes them.
