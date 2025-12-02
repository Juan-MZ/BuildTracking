# BuildTracking

BuildTracking es un backend en Java (Spring Boot) para gestionar el seguimiento de obra en proyectos de construcción: empleados, asistencias, participaciones, proyectos, ítems y **contabilidad por proyecto**.

Este repositorio contiene la implementación de las entidades principales con una arquitectura modular completa (models, dto, repositories, services, controllers) siguiendo las mejores prácticas de desarrollo empresarial.

## 🧭 Objetivo
Proveer una API REST robusta para llevar control de recursos humanos con jerarquías organizacionales, participación en proyectos, registro de asistencias, gestión de ítems, **contabilidad de facturas por proyecto** y consultas personalizadas. La arquitectura está diseñada con manejo de excepciones centralizado, respuestas estandarizadas y separación completa entre DTOs y entidades JPA.

## 📁 Estructura principal
- src/main/java/com/construmedicis/buildtracking
  - attendance (asistencias)
  - employee (empleados con jerarquía)
  - participation (participaciones empleado-proyecto)
  - project (proyectos de construcción)
  - item (ítems de proyecto)
  - **invoice** (facturas con contabilidad)
  - **assignment** (reglas de asignación automática)
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
  - GET  /api/items/project/{projectId}  *(ítems de un proyecto - catálogo específico)* 🆕
  - POST /api/items  (body: ItemDTO)
  - PUT  /api/items/{id}  (body: ItemDTO) *(actualizar nombre, descripción, precio, cantidad)* 🆕
  - DELETE /api/items/{id}

*Características especiales*:
- **Catálogo dinámico**: Los ítems se crean automáticamente al importar facturas desde Gmail si no existen
- **Vinculación inteligente**: Sistema de matching que busca ítems existentes por descripción exacta o código
- **Multi-proyecto**: Un ítem puede asociarse a múltiples proyectos mediante relación ManyToMany
- **Sincronización con facturas**: Cada `InvoiceItem` se vincula a un `Item` del catálogo mediante `itemId`

### Invoice (Facturas) 💰
  - GET  /api/invoices
  - GET  /api/invoices/{id}
  - GET  /api/invoices/project/{projectId}  *(facturas de un proyecto)*
  - GET  /api/invoices/supplier/{supplierId}  *(facturas de un proveedor)*
  - GET  /api/invoices/date-range?startDate=yyyy-MM-dd&endDate=yyyy-MM-dd  *(por rango de fechas)*
  - GET  /api/invoices/pending-review?maxConfidence=70  *(facturas con baja confianza en asignación)*
  - POST /api/invoices  (body: InvoiceDTO)
  - **POST /api/invoices/sync-gmail?gmailLabel=Facturas/Proyecto1** 🎯 *(sincronización automática desde Gmail)* 🆕
  - PUT  /api/invoices/{id}/assign-project?projectId=X  *(asignar proyecto manualmente)*
  - DELETE /api/invoices/{id}

*Flujo de sincronización desde Gmail*:
1. Llama `POST /api/invoices/sync-gmail?gmailLabel=Facturas` (especifica etiqueta de Gmail)
2. Sistema autentica con Gmail usando OAuth 2.0 (credentials.json en src/main/resources/)
3. Busca correos con esa etiqueta que tengan adjuntos
4. Descarga adjuntos:
   - **XMLs directos**: Procesa inmediatamente
   - **ZIPs**: Descomprime y extrae XMLs contenidos (las facturas suelen enviarse comprimidas con PDF+XML)
5. Parsea cada XML (formato DIAN - facturas electrónicas Colombia) y **verifica si ya existe** en BD por número de factura (evita duplicados)
6. Para **facturas nuevas**:
   - Crea `Invoice` con:
     * `source=EMAIL_AUTO` (todas las facturas de correo ya están pagadas)
     * `withholdingTax` y `withholdingICA`: BigDecimal.ZERO si no existen en XML (nunca null)
   - Para cada ítem del XML:
     * Busca/crea `Item` en catálogo (matching por código o descripción, **sin precio** - el precio está en la factura)
     * Crea `InvoiceItem` vinculado al `Item` del catálogo (aquí se guarda el precio de compra)
   - Evalúa **reglas de asignación automática**:
     * Si confianza ≥ 70%: asigna factura al proyecto y asocia items al proyecto
     * Si confianza < 70%: marca para revisión manual
7. Elimina archivos temporales (ZIPs, XMLs extraídos después de procesarlos)
8. Retorna estadísticas: emails procesados, facturas creadas, auto-asignadas, pendientes revisión

### InvoiceItem (Líneas de factura)
  - GET  /api/invoice-items
  - GET  /api/invoice-items/{id}
  - GET  /api/invoice-items/invoice/{invoiceId}  *(líneas de una factura)*
  - GET  /api/invoice-items/item/{itemId}  *(facturas donde se compró un ítem)*
  - POST /api/invoice-items  (body: InvoiceItemDTO)
  - DELETE /api/invoice-items/{id}

*Características especiales*:
- **Precios en facturas, no en catálogo**: El catálogo de `Item` NO tiene precio. Los precios están en `InvoiceItem` (cada compra puede tener precio diferente)
- **Cálculo automático de totales**: El servicio calcula automáticamente el total considerando subtotal, IVA y retenciones
- **Asignación de confianza**: Sistema de confianza (0-100%) para asignaciones automáticas de proyecto
- **Todas pagadas**: Las facturas en el sistema ya están pagadas (no hay estados pendientes)

### ProjectAssignmentRule (Reglas de asignación automática) 🤖
  - GET  /api/assignment-rules
  - GET  /api/assignment-rules/{id}
  - GET  /api/assignment-rules/project/{projectId}  *(reglas de un proyecto)*
  - GET  /api/assignment-rules/active  *(solo reglas activas ordenadas por prioridad)*
  - GET  /api/assignment-rules/type/{ruleType}  *(filtrar por tipo de regla)*
  - POST /api/assignment-rules  (body: ProjectAssignmentRuleDTO)
  - POST /api/assignment-rules/evaluate  *(evaluar reglas para una factura)*
  - PUT  /api/assignment-rules/{id}/toggle?isActive=true  *(activar/desactivar regla)*
  - DELETE /api/assignment-rules/{id}

*Tipos de reglas disponibles*:
- **SUPPLIER_NIT**: Asigna por NIT del proveedor (confianza: 95%)
- **DATE_RANGE**: Asigna por rango de fechas (confianza: 70%)
- **KEYWORDS**: Asigna por palabras clave en descripciones (confianza: 60-85%)
- **EMPLOYEE_PARTICIPATION**: Asigna si hay empleados participando en el proyecto (confianza: 75%)
- **MANUAL**: Siempre requiere confirmación manual

*Flujo de evaluación*:
1. Se ordenan las reglas activas por prioridad (menor número = mayor prioridad)
2. Se evalúa cada regla en orden hasta encontrar una coincidencia
3. Retorna projectId, nombre, confianza y razón de la coincidencia
4. Si confianza < 70%, la factura queda pendiente de revisión manual

## 🔐 Configuración Gmail OAuth 2.0

Para que la sincronización automática funcione, necesitas configurar credenciales OAuth 2.0:

1. **credentials.json**: Archivo de credenciales de Google Cloud Console
   - Ubicación: `src/main/resources/credentials.json`
   - Obtener en: https://console.cloud.google.com/apis/credentials
   - Habilitar Gmail API en Google Cloud Console
2. **tokens/**: Directorio para tokens de acceso (se crea automáticamente en `src/main/resources/tokens/`)
3. **Primera autenticación**: Al llamar por primera vez `POST /api/invoices/sync-gmail` se abrirá el navegador para autorizar

### EmailConfig (Configuración legacy - DEPRECADO) 📧

**NOTA**: El módulo `EmailConfig` está deprecado. Usa directamente `POST /api/invoices/sync-gmail?gmailLabel=TuEtiqueta`

Los endpoints de EmailConfig aún funcionan pero ya no son necesarios:
  - GET  /api/email-config
  - GET  /api/email-config/{id}
  - GET  /api/email-config/project/{projectId}
  - GET  /api/email-config/auto-sync
  - POST /api/email-config  (body: EmailConfigDTO)
  - PUT  /api/email-config/{id}  (body: EmailConfigDTO)
  - DELETE /api/email-config/{id}
  - ~~POST /api/email-config/{id}/sync~~ (usar `POST /api/invoices/sync-gmail` en su lugar)

## 🔐 Manejo de errores y respuestas
El proyecto implementa un sistema robusto de manejo de errores y respuestas estandarizadas:
- **`Response<T>`** y **`ResponseHandler`**: Wrapper uniforme para todas las respuestas con estructura `{status, userMessage, moreInfo, data}`
- **`BusinessRuleException`** y **`ActionBusinessRuleException`**: Excepciones personalizadas para reglas de negocio
- **`CustomExceptionHandler`**: Manejador global de excepciones con mensajes i18n desde `exceptions.properties`
- **Validaciones**: Los servicios validan la existencia de entidades relacionadas antes de operaciones (throw exception si no existen)

## 📦 Versionado
El proyecto usa **versionado semántico (SemVer)** y está configurado con automatización de releases mediante GitHub Actions.

**Versión actual**: `0.0.3-SNAPSHOT` (ver archivo `VERSION`)

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
