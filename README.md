# 📐 Engineering Design & Component Management System

**EDCMS** is a production-grade **Spring Boot REST API** backend that manages the complete lifecycle of engineering components, versioned assemblies (Bills of Materials), dependency graphs, and compatibility rules — built as a comprehensive showcase of **Object-Oriented Programming** in Java 21.

---

## 📑 Table of Contents

- [Project Purpose](#-project-purpose)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Database Schema](#-database-schema)
- [Package Structure](#-package-structure)
- [Domain Model](#-domain-model)
- [REST API Reference](#-rest-api-reference)
- [Key Business Rules](#-key-business-rules)
- [OOP Principles Applied](#-oop-principles-applied)
- [Setup & Running](#-setup--running)
- [Environment Variables](#-environment-variables)
- [Testing](#-testing)
- [Known Limitations](#-known-limitations)

---

## 🎯 Project Purpose

EDCMS acts as the software backbone for an engineering organization, solving problems like:

- **Catalog management** — store and classify Mechanical, Electrical, and Software components
- **Version control** — each component has versioned releases with a strict lifecycle (`DRAFT → APPROVED → DEPRECATED`)
- **Bill of Materials (BOM)** — compose assemblies from approved component versions
- **Dependency graphs** — declare what depends on what, with automatic circular dependency detection
- **Compatibility enforcement** — define and enforce incompatibility rules between version pairs
- **Impact analysis** — instantly answer "what assemblies break if I deprecate this version?"

---

## 🛠️ Tech Stack

| Category | Technology | Version |
|---|---|---|
| **Language** | Java | 21 (LTS) |
| **Framework** | Spring Boot | 3.3.4 |
| **Web Layer** | Spring MVC (REST) | — |
| **Persistence** | Spring Data JPA + Hibernate | — |
| **Database** | MySQL | 8.x |
| **Test Database** | H2 (in-memory) | — |
| **Code Generation** | Lombok | — |
| **API Documentation** | SpringDoc OpenAPI (Swagger UI) | 2.6.0 |
| **Validation** | Jakarta Bean Validation (JSR-380) | — |
| **Testing** | JUnit 5 + Mockito + AssertJ | — |
| **Build Tool** | Apache Maven | — |
| **Connection Pool** | HikariCP | — |

---

## 🏗️ Architecture

### Layered Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                        REST Clients                                │
│               (Swagger UI / Postman / Frontend App)                │
└────────────────────────────┬───────────────────────────────────────┘
                             │  HTTP JSON Requests / Responses
                             ▼
┌────────────────────────────────────────────────────────────────────┐
│                     Controller Layer  (8 classes)                  │
│  UserController │ ProjectController │ ComponentController          │
│  ComponentVersionController │ AssemblyController                   │
│  DependencyController │ CompatibilityController                    │
│  ImpactAnalysisController                                          │
│                                                                    │
│  Role: Route HTTP requests → delegate to service, return DTO       │
└────────────────────────────┬───────────────────────────────────────┘
                             │  Service method calls
                             ▼
┌────────────────────────────────────────────────────────────────────┐
│                      Service Layer  (8 classes)                    │
│  UserService │ ProjectService │ ComponentService                   │
│  ComponentVersionService │ AssemblyService                         │
│  DependencyService │ CompatibilityService │ ImpactAnalysisService  │
│                                                                    │
│  Role: All business rules, state machine, cycle detection,         │
│        compatibility validation, @Transactional boundaries         │
└──────────┬─────────────────────┬──────────────────────────────────┘
           │                     │
           ▼                     ▼
┌──────────────────┐   ┌──────────────────────────────────────────┐
│  Mapper Layer    │   │          Repository Layer  (8 interfaces) │
│  (5 classes)     │   │  ComponentRepository                      │
│  Entity ↔ DTO    │   │  ComponentVersionRepository               │
│  conversion      │   │  AssemblyRepository                       │
└──────────────────┘   │  AssemblyComponentRepository              │
                       │  DependencyRepository                     │
┌──────────────────┐   │  CompatibilityRuleRepository              │
│  Checker Layer   │   │  ProjectRepository │ UserRepository        │
│  (Strategy)      │   │                                          │
│  CompatibilityChecker  Spring Data JPA generates implementations │
│  interface +     │   └────────────────────┬─────────────────────┘
│  DefaultImpl     │                        │ JDBC / Hibernate ORM
└──────────────────┘                        ▼
                       ┌──────────────────────────────────────────┐
                       │             MySQL Database                │
                       │                                          │
                       │  users                                   │
                       │  projects                                │
                       │  components  ◄── base (JOINED)           │
                       │  mechanical_components                   │
                       │  electrical_components                   │
                       │  software_components                     │
                       │  component_versions                      │
                       │  assemblies                              │
                       │  assembly_components  (junction)         │
                       │  component_dependencies                  │
                       │  compatibility_rules                     │
                       └──────────────────────────────────────────┘
```

### Design Patterns Used

| Pattern | Where Applied |
|---|---|
| **Strategy** | `CompatibilityChecker` interface → `DefaultCompatibilityChecker` |
| **Repository** | All Spring Data JPA interfaces |
| **Builder** | Lombok `@Builder` / `@SuperBuilder` on all entities and DTOs |
| **State Machine** | `ComponentVersionService.validateTransition()` |
| **Template Method** | JPA JOINED inheritance on `Component` hierarchy |
| **Dependency Injection** | Constructor injection via `@RequiredArgsConstructor` throughout |

---

## 🗄️ Database Schema

### Entity Relationship Overview

```
users
 └──< projects (createdBy)
        └──< assemblies
               └──< assembly_components >──┐
                                           │
components (abstract base — JOINED)        │
  ├── mechanical_components                │
  ├── electrical_components      component_versions >─┘
  └── software_components               │      │
              └──────────────────────────┘      │
                                                │
component_dependencies (self-referencing graph) │
  dependent_version_id ──────────────────────────┘
  dependency_version_id ─────────────────────────┐
                                                  │
compatibility_rules                               │
  version_a_id ─────────────────────────────── ──┘
  version_b_id ─────────────────────────────── ──┘
```

### Tables

| Table | Description |
|---|---|
| `users` | Engineers and admins |
| `projects` | Top-level containers for assemblies |
| `components` | Base table (has `component_type` discriminator column) |
| `mechanical_components` | Extends components: `material`, `weight_kg` |
| `electrical_components` | Extends components: `voltage_rating`, `current_rating`, `power_rating_w` |
| `software_components` | Extends components: `programming_lang`, `license_type` |
| `component_versions` | Versioned releases (DRAFT / APPROVED / DEPRECATED) |
| `assemblies` | Named BOMs belonging to a project |
| `assembly_components` | Junction — maps versions into assemblies with quantity |
| `component_dependencies` | Directed dependency edges between versions |
| `compatibility_rules` | Declared incompatibility pairs between versions |

> **Hibernate auto-generates all tables** on first startup via `ddl-auto: update`.  
> Only the database itself needs to be created manually (see [Setup](#-setup--running)).

---

## 📁 Package Structure

```
src/
└── main/
    ├── java/com/edcms/
    │   ├── EngineeringComponentManagerApplication.java  ← Spring Boot entry point
    │   │
    │   ├── entity/                      ← JPA entities (11 classes)
    │   │   ├── Component.java           ← abstract base (JOINED inheritance root)
    │   │   ├── MechanicalComponent.java
    │   │   ├── ElectricalComponent.java
    │   │   ├── SoftwareComponent.java
    │   │   ├── ComponentVersion.java
    │   │   ├── Assembly.java
    │   │   ├── AssemblyComponent.java   ← junction entity
    │   │   ├── Dependency.java
    │   │   ├── CompatibilityRule.java
    │   │   ├── Project.java
    │   │   └── User.java
    │   │
    │   ├── enums/                       ← Domain enumerations (6)
    │   │   ├── Role.java                  ADMIN | ENGINEER
    │   │   ├── ComponentType.java         MECHANICAL | ELECTRICAL | SOFTWARE
    │   │   ├── ComponentStatus.java       ACTIVE | DEPRECATED
    │   │   ├── VersionStatus.java         DRAFT | APPROVED | DEPRECATED
    │   │   ├── ProjectStatus.java         ACTIVE | COMPLETED | ARCHIVED
    │   │   └── DependencyType.java        REQUIRED | OPTIONAL | CONFLICTS_WITH
    │   │
    │   ├── dto/
    │   │   ├── request/                 ← Input POJOs (10 classes)
    │   │   │   ├── CreateUserRequest.java
    │   │   │   ├── CreateProjectRequest.java
    │   │   │   ├── ChangeProjectStatusRequest.java
    │   │   │   ├── CreateComponentRequest.java
    │   │   │   ├── CreateComponentVersionRequest.java
    │   │   │   ├── CreateAssemblyRequest.java
    │   │   │   ├── AddComponentToAssemblyRequest.java
    │   │   │   ├── CreateDependencyRequest.java
    │   │   │   ├── CreateCompatibilityRuleRequest.java
    │   │   │   └── CompatibilityCheckRequest.java
    │   │   │
    │   │   └── response/                ← Output POJOs (11 classes)
    │   │       ├── UserResponse.java
    │   │       ├── ProjectResponse.java
    │   │       ├── ComponentResponse.java
    │   │       ├── ComponentVersionResponse.java
    │   │       ├── AssemblyResponse.java
    │   │       ├── AssemblyComponentResponse.java
    │   │       ├── AssemblySummaryResponse.java
    │   │       ├── DependencyResponse.java
    │   │       ├── CompatibilityRuleResponse.java
    │   │       ├── CompatibilityCheckResponse.java
    │   │       └── ImpactAnalysisResponse.java
    │   │
    │   ├── repository/                  ← Spring Data JPA interfaces (8)
    │   │   ├── UserRepository.java
    │   │   ├── ProjectRepository.java
    │   │   ├── ComponentRepository.java
    │   │   ├── ComponentVersionRepository.java
    │   │   ├── AssemblyRepository.java
    │   │   ├── AssemblyComponentRepository.java
    │   │   ├── DependencyRepository.java
    │   │   └── CompatibilityRuleRepository.java
    │   │
    │   ├── service/                     ← Business logic (8 classes)
    │   │   ├── UserService.java
    │   │   ├── ProjectService.java
    │   │   ├── ComponentService.java
    │   │   ├── ComponentVersionService.java
    │   │   ├── AssemblyService.java
    │   │   ├── DependencyService.java
    │   │   ├── CompatibilityService.java
    │   │   └── ImpactAnalysisService.java
    │   │
    │   ├── controller/                  ← REST controllers (8 classes)
    │   │   ├── UserController.java
    │   │   ├── ProjectController.java
    │   │   ├── ComponentController.java
    │   │   ├── ComponentVersionController.java
    │   │   ├── AssemblyController.java
    │   │   ├── DependencyController.java
    │   │   ├── CompatibilityController.java
    │   │   └── ImpactAnalysisController.java
    │   │
    │   ├── mapper/                      ← Entity ↔ DTO converters (5 classes)
    │   │   ├── UserMapper.java
    │   │   ├── ProjectMapper.java
    │   │   ├── ComponentMapper.java
    │   │   ├── ComponentVersionMapper.java
    │   │   └── AssemblyMapper.java
    │   │
    │   ├── checker/                     ← Compatibility Strategy Pattern
    │   │   ├── CompatibilityChecker.java          (interface)
    │   │   └── DefaultCompatibilityChecker.java   (implementation)
    │   │
    │   ├── config/
    │   │   └── SwaggerConfig.java       ← OpenAPI / Swagger UI configuration
    │   │
    │   └── exception/                   ← Exception hierarchy (13 classes)
    │       ├── EdcmsException.java                 (abstract base)
    │       ├── ComponentNotFoundException.java
    │       ├── ComponentVersionNotFoundException.java
    │       ├── DuplicateComponentException.java
    │       ├── DuplicateUserException.java
    │       ├── ProjectNotFoundException.java
    │       ├── AssemblyNotFoundException.java
    │       ├── CircularDependencyException.java
    │       ├── CompatibilityException.java
    │       ├── InvalidVersionStatusException.java
    │       ├── InvalidVersionTransitionException.java
    │       ├── UserNotFoundException.java
    │       └── GlobalExceptionHandler.java
    │
    └── resources/
        ├── application.yml              ← Main configuration
        ├── application-example.yml      ← Template for local setup
        └── db/
            └── create_database.sql     ← One-time DB creation script

src/
└── test/
    └── java/com/edcms/
        ├── ComponentServiceTest.java         (8 unit tests)
        ├── ComponentVersionServiceTest.java  (13 unit tests)
        ├── DependencyServiceTest.java        (6 unit tests)
        ├── CompatibilityServiceTest.java     (8 unit tests)
        ├── ImpactAnalysisServiceTest.java    (5 unit tests)
        └── WorkflowIntegrationTest.java      (9 integration tests)
```

---

## 🧩 Domain Model

### Component Inheritance Hierarchy (JPA JOINED Strategy)

```
Component  (abstract — table: components)
│  id, componentCode, name, description, manufacturer
│  status: ACTIVE | DEPRECATED
│  component_type (discriminator column)
│
├── MechanicalComponent  (table: mechanical_components)
│      material, weightKg
│
├── ElectricalComponent  (table: electrical_components)
│      voltageRating, currentRating, powerRatingW
│
└── SoftwareComponent    (table: software_components)
       programmingLang, licenseType
```

### Exception Hierarchy

```
RuntimeException
└── EdcmsException  (abstract)
    ├── ComponentNotFoundException
    ├── ComponentVersionNotFoundException
    ├── DuplicateComponentException
    ├── DuplicateUserException
    ├── ProjectNotFoundException
    ├── AssemblyNotFoundException
    ├── CircularDependencyException
    ├── CompatibilityException
    ├── InvalidVersionStatusException
    └── InvalidVersionTransitionException
```

All exceptions are caught by `GlobalExceptionHandler` → consistent JSON error responses with appropriate HTTP status codes.

### Version State Machine

```
         ┌────────────────────────┐
         │                        ▼
      DRAFT ──────────────► APPROVED ──┐
         │                             │
         └─────────────────────────────┴──► DEPRECATED  (terminal — irreversible)
```

| From | To | Allowed? |
|---|---|---|
| DRAFT | APPROVED | ✅ |
| DRAFT | DEPRECATED | ✅ |
| APPROVED | DEPRECATED | ✅ |
| APPROVED | DRAFT | ❌ (no rollback) |
| DEPRECATED | anything | ❌ (terminal) |

---

## 🌐 REST API Reference

> **Base URL:** `http://localhost:8080`  
> **Interactive Docs:** `http://localhost:8080/swagger-ui.html`  
> **OpenAPI JSON:** `http://localhost:8080/api-docs`

---

### 👤 Users — `/api/users`

| Method | Endpoint | Description | Status |
|---|---|---|---|
| `POST` | `/api/users` | Create a new user | `201 Created` |
| `GET` | `/api/users/{id}` | Get user by ID | `200 OK` |
| `GET` | `/api/users` | List all users | `200 OK` |

**Create User — Request Body:**
```json
{
  "username": "john_engineer",
  "email": "john@company.com",
  "password": "securepass123",
  "role": "ENGINEER"
}
```

---

### 📁 Projects — `/api/projects`

| Method | Endpoint | Description | Status |
|---|---|---|---|
| `POST` | `/api/projects` | Create a new project | `201 Created` |
| `GET` | `/api/projects/{id}` | Get project by ID | `200 OK` |
| `GET` | `/api/projects` | List all projects | `200 OK` |
| `PATCH` | `/api/projects/{id}/status` | Change project status | `200 OK` |

**Create Project — Request Body:**
```json
{
  "name": "Drone v3 Project",
  "description": "Next-gen quadrotor assembly",
  "createdById": 1
}
```

---

### ⚙️ Components — `/api/components`

| Method | Endpoint | Description | Status |
|---|---|---|---|
| `POST` | `/api/components` | Create Mechanical / Electrical / Software component | `201 Created` |
| `GET` | `/api/components/{id}` | Get component (with subtype-specific fields) | `200 OK` |
| `GET` | `/api/components` | List all components | `200 OK` |
| `PUT` | `/api/components/{id}` | Update mutable fields (name, description, manufacturer) | `200 OK` |
| `PATCH` | `/api/components/{id}/deprecate` | Mark component DEPRECATED | `200 OK` |

**Create Mechanical Component — Request Body:**
```json
{
  "componentCode": "MECH-MOTOR-001",
  "name": "High-Torque Motor",
  "description": "Brushless DC motor for propulsion",
  "manufacturer": "TurboMotion Inc.",
  "componentType": "MECHANICAL",
  "material": "Aluminium Alloy 6061",
  "weightKg": 0.250,
  "createdById": 1
}
```

**Create Electrical Component — Request Body:**
```json
{
  "componentCode": "ELEC-CTRL-001",
  "name": "Motor Controller v2",
  "componentType": "ELECTRICAL",
  "voltageRating": 24.0,
  "currentRating": 30.0,
  "powerRatingW": 720.0,
  "createdById": 1
}
```

**Create Software Component — Request Body:**
```json
{
  "componentCode": "SW-FW-001",
  "name": "Flight Controller Firmware",
  "componentType": "SOFTWARE",
  "programmingLang": "C++",
  "licenseType": "MIT",
  "createdById": 1
}
```

---

### 🔖 Component Versions — `/api/components/{id}/versions`

| Method | Endpoint | Description | Status |
|---|---|---|---|
| `POST` | `/api/components/{id}/versions` | Create a new version (always starts as DRAFT) | `201 Created` |
| `GET` | `/api/components/{id}/versions` | List all versions for a component | `200 OK` |
| `GET` | `/api/versions/{versionId}` | Get version by ID | `200 OK` |
| `PATCH` | `/api/versions/{versionId}/approve` | DRAFT → APPROVED | `200 OK` |
| `PATCH` | `/api/versions/{versionId}/deprecate` | Any → DEPRECATED | `200 OK` |

**Create Version — Request Body:**
```json
{
  "versionNumber": "2.1.0",
  "releaseNotes": "Improved torque output by 15%",
  "createdById": 1
}
```

---

### 🏗️ Assemblies — `/api/projects/{id}/assemblies`

| Method | Endpoint | Description | Status |
|---|---|---|---|
| `POST` | `/api/projects/{id}/assemblies` | Create a new BOM under a project | `201 Created` |
| `GET` | `/api/projects/{id}/assemblies` | List all assemblies in a project | `200 OK` |
| `GET` | `/api/assemblies/{id}` | Get assembly with full component list | `200 OK` |
| `POST` | `/api/assemblies/{id}/components` | Add an APPROVED component version to BOM | `200 OK` |
| `DELETE` | `/api/assemblies/{id}/components/{acId}` | Remove a component from BOM | `204 No Content` |

**Add Component to Assembly — Request Body:**
```json
{
  "componentVersionId": 5,
  "quantity": 4,
  "notes": "One motor per arm"
}
```

> ⚠️ Only **APPROVED** versions can be added. DRAFT/DEPRECATED versions are rejected.  
> ⚠️ Compatibility rules are checked automatically before adding.

---

### 🔗 Dependencies — `/api/dependencies`

| Method | Endpoint | Description | Status |
|---|---|---|---|
| `POST` | `/api/dependencies` | Declare a dependency between two versions | `201 Created` |
| `GET` | `/api/versions/{versionId}/dependencies` | Get all dependencies for a version | `200 OK` |
| `DELETE` | `/api/dependencies/{id}` | Remove a dependency | `204 No Content` |

**Create Dependency — Request Body:**
```json
{
  "dependentVersionId": 3,
  "dependencyVersionId": 7,
  "dependencyType": "REQUIRED",
  "notes": "Firmware requires Controller v2 or higher"
}
```

> ⚠️ **Circular dependencies are automatically detected and rejected.**

---

### ✅ Compatibility — `/api/compatibility`

| Method | Endpoint | Description | Status |
|---|---|---|---|
| `POST` | `/api/compatibility/rules` | Declare two versions as incompatible | `201 Created` |
| `GET` | `/api/compatibility/rules` | List all incompatibility rules | `200 OK` |
| `POST` | `/api/compatibility/check` | Check if a set of version IDs are mutually compatible | `200 OK` |
| `DELETE` | `/api/compatibility/rules/{id}` | Delete a rule | `204 No Content` |

**Create Rule — Request Body:**
```json
{
  "versionAId": 2,
  "versionBId": 5,
  "notes": "Motor v2 and Controller v2 have incompatible CAN bus protocols",
  "createdById": 1
}
```

**Check Compatibility — Request Body:**
```json
{
  "versionIds": [2, 5, 8]
}
```

**Check Response:**
```json
{
  "compatible": false,
  "violations": [
    {
      "versionAId": 2,
      "versionAName": "Motor v2.0",
      "versionBId": 5,
      "versionBName": "Controller v2.0",
      "notes": "Incompatible CAN bus protocols"
    }
  ]
}
```

---

### 📊 Impact Analysis — `/api/impact`

| Method | Endpoint | Description | Status |
|---|---|---|---|
| `GET` | `/api/impact/versions/{versionId}` | Which assemblies use this specific version? | `200 OK` |
| `GET` | `/api/impact/components/{componentId}` | Which assemblies use any version of this component? | `200 OK` |

**Response Example:**
```json
{
  "versionId": 3,
  "versionNumber": "2.0",
  "componentName": "High-Torque Motor",
  "affectedAssemblyCount": 2,
  "affectedAssemblies": [
    { "assemblyId": 1, "assemblyName": "Drone BOM v1", "projectName": "Drone v3 Project" },
    { "assemblyId": 4, "assemblyName": "Rover Assembly", "projectName": "Ground Rover Project" }
  ]
}
```

---

## 🔍 Key Business Rules

### 1. Version State Machine Enforcement
- New versions always start as **DRAFT**
- Transitions are validated by `ComponentVersionService.validateTransition()`
- **DEPRECATED is terminal** — models real engineering change control

### 2. Circular Dependency Detection (DFS)
- `DependencyService` runs **Depth-First Search** on the version dependency graph before every new edge insertion
- If adding edge `A → B` would create a path `B →...→ A`, the request is rejected with `CircularDependencyException`

### 3. Compatibility Check Before BOM Add
- When adding a version to an assembly, `CompatibilityChecker` compares the new version against **every existing version** in the assembly
- If any pair matches a `CompatibilityRule`, the request is rejected with `CompatibilityException`
- All validation happens inside a single `@Transactional` boundary

### 4. BOM Assembly Rules
- Only **APPROVED** versions may be added to an assembly
- The same version cannot be added to the same assembly twice
- Removing a component from a BOM does not affect the version itself

### 5. Immutable Fields
- `componentCode` is unique and cannot be changed after creation (it's the natural key)
- `versionNumber` is immutable per component after creation

---

## 🏛️ OOP Principles Applied

> See [`OOP_DESIGN.md`](OOP_DESIGN.md) for full code examples and rationale.

| Principle | Applied Where |
|---|---|
| **Encapsulation** | All entity fields are `private`; exposed only via Lombok-generated getters/setters. Controllers call services without knowing DB details. |
| **Abstraction** | `CompatibilityChecker` interface hides checking algorithm. `EdcmsException` abstract class unifies error handling. Repository interfaces hide all SQL. |
| **Inheritance** | `Component → MechanicalComponent / ElectricalComponent / SoftwareComponent` (JPA JOINED strategy). `EdcmsException` exception hierarchy. |
| **Polymorphism** | `ComponentMapper.toResponse()` uses Java 21 pattern-matching instanceof. `componentRepository.save(component)` routes to the correct joined table at runtime. `GlobalExceptionHandler` dispatches by most-specific exception type. |
| **Composition** | `Assembly` is composed of `AssemblyComponent` junction records. `User` has-a `Role` enum (not extends). |
| **SRP** | 8 dedicated service classes, each with exactly one focused responsibility. |
| **OCP** | New compatibility checking strategy: create a new class implementing `CompatibilityChecker` — zero changes to existing code. |
| **DI / IoC** | Constructor injection via `@RequiredArgsConstructor`; all dependencies are `final` and testable via mock injection. |
| **State Machine** | `ComponentVersionService.validateTransition()` enforces all legal/illegal version transitions. |
| **Strategy Pattern** | `CompatibilityChecker` interface + `DefaultCompatibilityChecker` implementation — swappable without touching `CompatibilityService`. |

---

## ⚡ Setup & Running

### Prerequisites

| Tool | Version |
|---|---|
| Java JDK | 21+ |
| Maven | 3.9+ |
| MySQL | 8.x |

---

### Step 1 — Create the Database

```sql
-- Run this once in your MySQL client
mysql -u root -p < src/main/resources/db/create_database.sql
```

Or manually:
```sql
CREATE DATABASE IF NOT EXISTS edcms_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

---

### Step 2 — Configure Environment Variables

Set the following environment variables (never hard-code credentials):

```bash
# Windows (PowerShell)
$env:EDCMS_DB_HOST = "localhost"
$env:EDCMS_DB_PORT = "3306"
$env:EDCMS_DB_NAME = "edcms_db"
$env:EDCMS_DB_USER = "your_mysql_username"
$env:EDCMS_DB_PASS = "your_mysql_password"
```

```bash
# Linux / macOS
export EDCMS_DB_HOST=localhost
export EDCMS_DB_PORT=3306
export EDCMS_DB_NAME=edcms_db
export EDCMS_DB_USER=your_mysql_username
export EDCMS_DB_PASS=your_mysql_password
```

Alternatively, copy `application-example.yml` to `application-local.yml` (gitignored) and fill in credentials directly.

---

### Step 3 — Build & Run

```bash
# Clone and enter project
cd "Engineering Design & Component Management System"

# Build (skipping tests for first run)
mvn clean package -DskipTests

# Run
mvn spring-boot:run
```

> On first startup, Hibernate auto-creates all 11 database tables via `ddl-auto: update`.

---

### Step 4 — Access the Application

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interactive Swagger UI — try all endpoints |
| `http://localhost:8080/api-docs` | Raw OpenAPI JSON spec |
| `http://localhost:8080/api/components` | Example REST endpoint |

---

## 🔧 Environment Variables

| Variable | Default | Description |
|---|---|---|
| `EDCMS_DB_HOST` | `localhost` | MySQL host |
| `EDCMS_DB_PORT` | `3306` | MySQL port |
| `EDCMS_DB_NAME` | `edcms_db` | Database name |
| `EDCMS_DB_USER` | *(required)* | MySQL username |
| `EDCMS_DB_PASS` | *(required)* | MySQL password |

---

## 🧪 Testing

### Run All Tests

```bash
mvn test
```

### Test Statistics

| Category | Count |
|---|---|
| **Total tests** | **50** |
| Unit tests | 41 |
| Integration tests | 9 |
| Test classes | 6 |
| Bugs found & fixed | 5 |

### Unit Tests (41 tests across 5 classes)

| Test Class | Count | Coverage Focus |
|---|---|---|
| `ComponentServiceTest` | 8 | Create (Mechanical/Software), duplicate rejection, get, update, deprecate |
| `ComponentVersionServiceTest` | 13 | All 6 state machine transitions (legal & illegal), duplicate version number |
| `DependencyServiceTest` | 6 | Dependency creation, self-dependency rejection, DFS cycle detection |
| `CompatibilityServiceTest` | 8 | Rule creation, self-rule rejection, duplicate rule, check pass/fail, validate |
| `ImpactAnalysisServiceTest` | 5 | Single/multi-assembly impact, zero-impact cases, component-level impact |

### Integration Tests (9 tests — `WorkflowIntegrationTest`)

| Workflow | Scenario |
|---|---|
| **BOM Lifecycle** | Create project → component → version → approve → assembly → add version → verify |
| **BOM Lifecycle (negative)** | DRAFT version rejected when adding to assembly |
| **Compatibility Check (fail)** | Motor v2 + Controller v2 → `CompatibilityException` |
| **Compatibility Check (pass)** | Motor v2 + Controller v3 → assembly has 2 components |
| **Circular Dependency** | Motor→Controller→Firmware + Firmware→Motor → `CircularDependencyException` |
| **State Machine — valid** | DRAFT → APPROVED → DEPRECATED complete lifecycle |
| **State Machine — DEPRECATED→APPROVED** | `InvalidVersionTransitionException` |
| **State Machine — APPROVED→DRAFT** | `InvalidVersionTransitionException` |
| **Impact Analysis** | Battery v1.0 in 2 assemblies → impact count = 2 |

### Test Infrastructure

- Unit tests use **Mockito** to mock repositories — no database needed
- Integration tests use **H2 in-memory database** with `application-test.yml`
- H2 schema is recreated fresh per test run (`create-drop`)

---

## ⚠️ Known Limitations

1. **No HTTP-level authentication** — `createdById` is passed in request body. In production, this would come from a JWT token via Spring Security.

2. **Plain-text password storage** — `passwordHash` stores the raw password. BCrypt hashing (via Spring Security) is the production fix; intentionally excluded from this scope.

3. **No pagination** — List endpoints (`getAllComponents()`, etc.) return all records. For large catalogs, `Page<T>` from Spring Data should be used.

4. **O(n²) compatibility check** — `DefaultCompatibilityChecker.checkAll()` checks every unique pair. For assemblies with 100+ components, a cached or indexed approach is needed.

5. **No soft-delete** — Components/assemblies are not soft-deleted; there is no audit trail of deletions.

---

## 📄 Additional Documentation

| Document | Description |
|---|---|
| [`OOP_DESIGN.md`](OOP_DESIGN.md) | Detailed explanation of every OOP principle applied, with code examples |
| [`TESTING.md`](TESTING.md) | Full test report — all 50 test cases, 5 bugs found and fixed, N+1 analysis |

---

## 📜 License

This project is developed for academic purposes as part of an Object-Oriented Programming course project.

---

*Built with ☕ Java 21 · 🍃 Spring Boot 3.3.4 · 🐬 MySQL · 📄 Swagger UI*
