# BuildTracking

BuildTracking es un backend en Java (Spring Boot) para gestionar el seguimiento de obra en proyectos de construcción: empleados, asistencias, participaciones, proyectos e ítems.

Este repositorio contiene la implementación de las entidades principales con una arquitectura modular completa (models, dto, repositories, services, controllers) siguiendo las mejores prácticas de desarrollo empresarial.

## 🧭 Objetivo
Proveer una API REST robusta para llevar control de recursos humanos con jerarquías organizacionales, participación en proyectos, registro de asistencias, gestión de ítems y consultas personalizadas. La arquitectura está diseñada con manejo de excepciones centralizado, respuestas estandarizadas y separación completa entre DTOs y entidades JPA.

## 📁 Estructura principal
- src/main/java/com/construmedicis/buildtracking
  - attendance (asistencias)
  - employee (empleados con jerarquía)
  - participation (participaciones empleado-proyecto)
  - project (proyectos de construcción)
  - item (ítems de proyecto)
  - util (excepciones, response handler, etc.)

Cada módulo incluye:
- `models`: entidades JPA con relaciones (ManyToOne, OneToMany, ManyToMany)
- `dto`: DTOs para entrada y salida (evitan exponer entidades directamente y previenen referencias circulares)
- `repository`: interfaces JpaRepository con métodos de consulta personalizados
- `services`: interfaces y `impl` con lógica de negocio y validaciones
- `controller`: endpoints REST que usan DTOs y devuelven `Response<T>`

## 🔧 Requisitos
- Java 25
- Maven (el proyecto incluye _mvnw_ como wrapper)
- PostgreSQL (configurado en `application.properties` para `build_tracking_bd`)

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
Nota: todos los controladores devuelven un objeto `Response<T>` (status, userMessage, moreInfo, data).

### Employee
  - GET  /api/employees
  - GET  /api/employees/{id}
  - POST /api/employees  (body: EmployeeDTO)
  - DELETE /api/employees/{id}

*Características especiales*: 
- Los empleados soportan jerarquía organizacional (manager/subordinates)
- Relación ManyToMany con proyectos

### Project
  - GET  /api/projects
  - GET  /api/projects/{id}
  - POST /api/projects  (body: ProjectDTO)
  - DELETE /api/projects/{id}

### Participation
  - GET  /api/participations
  - GET  /api/participations/{id}
  - GET  /api/participations/project/{projectId}  *(consulta todas las participaciones de un proyecto)*
  - POST /api/participations  (body: ParticipationDTO)
  - DELETE /api/participations/{id}

### Attendance
  - GET  /api/attendances
  - GET  /api/attendances/{id}
  - GET  /api/attendances/participation/{participationId}  *(consulta todas las asistencias de una participación)*
  - POST /api/attendances  (body: AttendanceDTO)
  - DELETE /api/attendances/{id}

### Item
  - GET  /api/items
  - GET  /api/items/{id}
  - POST /api/items  (body: ItemDTO)
  - DELETE /api/items/{id}

## 🔐 Manejo de errores y respuestas
El proyecto implementa un sistema robusto de manejo de errores y respuestas estandarizadas:
- **`Response<T>`** y **`ResponseHandler`**: Wrapper uniforme para todas las respuestas con estructura `{status, userMessage, moreInfo, data}`
- **`BusinessRuleException`** y **`ActionBusinessRuleException`**: Excepciones personalizadas para reglas de negocio
- **`CustomExceptionHandler`**: Manejador global de excepciones con mensajes i18n desde `exceptions.properties`
- **Validaciones**: Los servicios validan la existencia de entidades relacionadas antes de operaciones (throw exception si no existen)

## 📦 Versionado
El proyecto usa **versionado semántico (SemVer)** y está configurado con automatización de releases mediante GitHub Actions.

**Versión actual**: `0.0.2-SNAPSHOT` (ver archivo `VERSION`)

*Formato:* MAJOR.MINOR.PATCH

- **MAJOR**: cambios incompatibles con versiones anteriores
- **MINOR**: nuevas funcionalidades retrocompatibles
- **PATCH**: correcciones y mejoras pequeñas

### Automatización de releases

El proyecto incluye un workflow de GitHub Actions (`.github/workflows/release.yml`) que se activa automáticamente al crear tags con formato `v*.*.*` (ej: `v1.0.0`).

**Pasos para crear un release**:

1. Actualizar versión en `VERSION` y `pom.xml`:

```powershell
# Editar VERSION manualmente (ej: 1.0.0)
.\mvnw.cmd versions:set -DnewVersion=1.0.0
.\mvnw.cmd -DskipTests package
```

2. Commit y crear tag:

```powershell
git add VERSION pom.xml
git commit -m "chore(release): 1.0.0"
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin main
git push origin v1.0.0
```

3. El workflow de GitHub Actions automáticamente:
   - Compila el proyecto
   - Crea el release en GitHub
   - Adjunta el JAR al release

## 🧪 Tests
Hay una clase de prueba base; añade más tests para cubrir servicios y controladores. Puedes ejecutar:

```powershell
.\mvnw.cmd test
```

## 👥 Contribuciones
Para colaborar en el proyecto:
- Usa commits descriptivos siguiendo convenciones (ej: `feat:`, `fix:`, `chore:`)
- Crea PRs con descripción clara de los cambios
- Mantén la consistencia con la arquitectura existente (DTOs, Response wrapper, manejo de excepciones)
- Agrega tests para nuevas funcionalidades

## 🏗️ Arquitectura y patrones
- **Separación de capas**: Controllers → Services → Repositories
- **DTOs**: Evitan exponer entidades JPA y previenen referencias circulares JSON
- **Response wrapper**: Todas las respuestas REST usan `Response<T>` para uniformidad
- **Validación**: Los servicios validan existencia de entidades relacionadas
- **i18n**: Mensajes de error centralizados en `exceptions.properties`
- **Jerarquías**: Employee soporta estructura árbol (manager/subordinates)
- **Consultas personalizadas**: Métodos repository adicionales para queries específicas (ej: `findByProjectId`)

---

**Versión**: 0.0.2-SNAPSHOT  
**Tecnologías**: Java 25 • Spring Boot 4.0.1-SNAPSHOT • PostgreSQL • JPA/Hibernate • Lombok • Maven
