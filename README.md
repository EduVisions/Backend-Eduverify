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
