# BuildTracking

BuildTracking es un backend en Java (Spring Boot) para gestionar el seguimiento de obra en proyectos de construcción: empleados, asistencias, participaciones y proyectos.

Este repositorio contiene la implementación inicial de las entidades principales y una estructura modular (models, dto, repositories, services, controllers) preparada para extenderse.

## 🧭 Objetivo
Proveer una API REST ligera para permitir llevar control de recursos humanos y participación en proyectos, registrar asistencias y disponer de una estructura extensible para reglas de negocio y operaciones sobre proyectos de construcción.

## 📁 Estructura principal
- src/main/java/com/construmedicis/buildtracking
  - attendance
  - employee
  - participation
  - project
  - util (excepciones, response handler, etc.)

Cada módulo incluye:
- `models`: entidades JPA
- `dto`: DTOs para entrada y salida (evitan exponer entidades directamente)
- `repository`: interfaces JpaRepository
- `services`: interfaces y `impl` con la lógica de negocio
- `controller`: endpoints REST

## 🔧 Requisitos
- Java 25
- Maven (el proyecto incluye _mvnw_ como wrapper)

## ▶️ Cómo ejecutar localmente
1. Compilar/ejecutar con el wrapper (Windows PowerShell):

```powershell
cd c:\path\to\buildtracking
.\mvnw.cmd -DskipTests spring-boot:run
```

2. Compilar empaquetado (jar):

```powershell
.\mvnw.cmd -DskipTests package
```

## 🧩 Endpoints básicos (ejemplos)
Nota: cada controlador devuelve un objeto `Response<T>` (status, userMessage, moreInfo, data).

- Employee
  - GET  /api/employees
  - GET  /api/employees/{id}
  - POST /api/employees  (body: EmployeeDTO)
  - DELETE /api/employees/{id}

- Project
  - GET  /api/projects
  - GET  /api/projects/{id}
  - POST /api/projects  (body: ProjectDTO)
  - DELETE /api/projects/{id}

- Participation
  - GET  /api/participations
  - GET  /api/participations/{id}
  - POST /api/participations  (body: ParticipationDTO)
  - DELETE /api/participations/{id}

- Attendance
  - GET  /api/attendances
  - GET  /api/attendances/{id}
  - POST /api/attendances  (body: AttendanceDTO)
  - DELETE /api/attendances/{id}

## 🔐 Manejo de errores y respuestas
El proyecto ya incluye utilidades para respuestas estandarizadas y manejo de excepciones:
- `Response<T>` y `ResponseHandler` para devolver payloads uniformes
- `BusinessRuleException`, `ActionBusinessRuleException` y `CustomExceptionHandler` para transformar excepciones a respuestas HTTP apropiadas

## 📦 Versionado
Recomiendo usar versionado semántico (SemVer) y etiquetas Git (tags) para releases.

*Formato:* MAJOR.MINOR.PATCH

- MAJOR: cambios incompatibles
- MINOR: nuevas funcionalidades retrocompatibles
- PATCH: correcciones y mejoras pequeñas

Pasos recomendados para crear un release manualmente:

1. Bump de versión con Maven (ejemplo a 1.0.0):

```powershell
.\mvnw.cmd versions:set -DnewVersion=1.0.0
.\mvnw.cmd -DskipTests package
```

2. Commit y tag:

```powershell
git add pom.xml
git commit -m "chore(release): 1.0.0"
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin HEAD
git push origin v1.0.0
```

3. Crear un release en GitHub (desde la interfaz o con `gh` CLI).

Si quieres, puedo añadir integración para automatizar releases (GitHub Actions + publicación automática) — dímelo y lo habilito.

## 🧪 Tests
Hay una clase de prueba base; añade más tests para cubrir servicios y controladores. Puedes ejecutar:

```powershell
.\mvnw.cmd test
```

## 👥 Contribuciones
Si deseas colaborar añade un `CONTRIBUTING.md` con convenciones (commits, PRs, revisión). Para versionado automático, podemos añadir GitHub Actions que generen versiones basadas en etiquetas o convenios de commits.

---
Si quieres que implemente automatización de versionado (por ejemplo, crear tags y releases desde un workflow) puedo añadir una acción de GitHub para publicar automáticamente cuando se cree una etiqueta semántica.
