# OOP Design Document — Engineering Design & Component Management System (EDCMS)

This document explains **how and why** Object-Oriented Programming principles are applied
throughout the EDCMS codebase. Each principle is illustrated with specific class references
and the reasoning behind each design decision.

---

## 1. Encapsulation

> **Definition:** Bundling data and the methods that operate on it into a single unit (class),
> and restricting direct access to internal state.

### Applied In: All Entity Classes

Every entity field is `private`. No code outside the class can directly read or write a field.

```java
// User.java — @Getter / @Setter generate access methods
// The field itself is private — external code goes through the method
@Column(name = "username", nullable = false, length = 50)
private String username;
```

**Lombok `@Getter` / `@Setter`** generate accessor methods at compile time, eliminating
boilerplate while enforcing the same encapsulation. External code calls `user.getUsername()`,
not `user.username`.

### Applied In: Business Logic Hidden Inside Services

Controllers don't know *how* a component is created — they only call `componentService.createComponent(request)`.
The logic for which entity subclass to instantiate, how to validate the code format, and how
to persist to the correct joined table is **encapsulated inside `ComponentService`**.

```java
// ComponentController.java — thin, doesn't know any DB details
public ResponseEntity<ComponentResponse> createComponent(@Valid @RequestBody CreateComponentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(componentService.createComponent(request));
}
```

### Applied In: User entity `isAdmin()` method

```java
// User.java
public boolean isAdmin() {
    return Role.ADMIN.equals(this.role);
}
```

The comparison logic is encapsulated inside the entity. No caller needs to compare role enums themselves.

---

## 2. Abstraction

> **Definition:** Exposing only what is necessary; hiding implementation details behind interfaces or abstract classes.

### Applied In: `CompatibilityChecker` Interface

```java
// CompatibilityChecker.java (interface)
public interface CompatibilityChecker {
    CompatibilityCheckResponse check(ComponentVersion newVersion, List<ComponentVersion> existingVersions);
    CompatibilityCheckResponse checkAll(List<ComponentVersion> versions);
}

// DefaultCompatibilityChecker.java (concrete implementation)
@Component
public class DefaultCompatibilityChecker implements CompatibilityChecker { ... }
```

`CompatibilityService` only depends on the `CompatibilityChecker` **interface**:
```java
private final CompatibilityChecker compatibilityChecker; // interface, NOT DefaultCompatibilityChecker
```

**Benefits:**
- In unit tests: a **mock** `CompatibilityChecker` is injected — no database needed.
- In future: a different checking strategy (e.g., ML-based) can be registered without changing `CompatibilityService`.
- This is the **Dependency Inversion Principle** (SOLID): depend on abstractions, not concretions.

### Applied In: `EdcmsException` Abstract Base Class

```java
// EdcmsException.java
public abstract class EdcmsException extends RuntimeException {
    protected EdcmsException(String message) { super(message); }
}
```

`EdcmsException` is abstract — you can never throw it directly. Only concrete subclasses
(`ComponentNotFoundException`, `CircularDependencyException`, etc.) are instantiated.
The `GlobalExceptionHandler` catches the base type to produce consistent HTTP error responses.

### Applied In: Repository Interfaces

```java
// ComponentRepository.java
@Repository
public interface ComponentRepository extends JpaRepository<Component, Long> { ... }
```

Callers use the `ComponentRepository` interface. Spring Data JPA generates the implementation
at runtime — callers are completely abstracted from SQL, JDBC, and connection pooling.

---

## 3. Inheritance

> **Definition:** A class can inherit fields and methods from a parent class,
> enabling code reuse and "is-a" relationships.

### Applied In: Component Hierarchy (JPA JOINED Strategy)

```
Component (abstract base, table: components)
├── MechanicalComponent (table: mechanical_components)  — adds: material, weightKg
├── ElectricalComponent (table: electrical_components)  — adds: voltageRating, currentRating, powerRatingW
└── SoftwareComponent   (table: software_components)    — adds: programmingLang, licenseType
```

**Entity-level:**
```java
// Component.java — base entity
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "component_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Component { ... }

// MechanicalComponent.java — subclass
@Entity
@Table(name = "mechanical_components")
@DiscriminatorValue("MECHANICAL")
public class MechanicalComponent extends Component {
    private String material;
    private BigDecimal weightKg;
}
```

**Why JOINED strategy?**
- Each component type has genuinely unique attributes.
- Separate tables prevent nullable "dummy" columns for attributes that don't apply.
- A JOIN query retrieves the full picture — base + subtype-specific fields — in one query.

**Interview point:** *Inheritance is used here because the relationship is genuinely "is-a" —
a `MechanicalComponent` IS a `Component`. It's not a tag or category. Using inheritance
here gives us type-safe storage, polymorphic service methods, and prevents code duplication.*

### Applied In: Exception Hierarchy

```
RuntimeException
└── EdcmsException (abstract)
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

All EDCMS domain exceptions inherit from `EdcmsException`. The `GlobalExceptionHandler`
catches the base type to produce consistent HTTP 400 error responses.

### Applied In: `@SuperBuilder` on Entity Hierarchy

Lombok's `@SuperBuilder` allows builder-pattern creation of subclasses:
```java
MechanicalComponent motor = MechanicalComponent.builder()
    .componentCode("MECH-MOTOR-001")  // from Component (parent)
    .name("High-Torque Motor")         // from Component (parent)
    .material("Aluminium Alloy 6061")  // MechanicalComponent-specific
    .weightKg(new BigDecimal("2.5"))   // MechanicalComponent-specific
    .build();
```

---

## 4. Polymorphism

> **Definition:** The same interface or method can work with different types at runtime,
> selecting the correct behaviour based on the actual object type.

### Applied In: `ComponentMapper.toResponse()` — Runtime Polymorphism

```java
// ComponentMapper.java
public ComponentResponse toResponse(Component component) {
    // The parameter is Component (base type).
    // Java 21 pattern matching instanceof extracts subtype fields.
    if (component instanceof MechanicalComponent mc) {
        builder.material(mc.getMaterial()).weightKg(mc.getWeightKg());
    } else if (component instanceof ElectricalComponent ec) {
        builder.voltageRating(ec.getVoltageRating());
    } else if (component instanceof SoftwareComponent sc) {
        builder.programmingLang(sc.getProgrammingLang());
    }
}
```

### Applied In: `componentRepository.save()` — Polymorphic Persistence

```java
// ComponentService.java
Component component = switch (request.getComponentType()) {
    case MECHANICAL -> MechanicalComponent.builder()...build();
    case ELECTRICAL -> ElectricalComponent.builder()...build();
    case SOFTWARE   -> SoftwareComponent.builder()...build();
};
Component saved = componentRepository.save(component); // routes to the correct joined table
```

### Applied In: `GlobalExceptionHandler` — Exception Dispatch

```java
// Catches EdcmsException AND all its subclasses
@ExceptionHandler(EdcmsException.class)
public ResponseEntity<ErrorResponse> handleEdcmsException(EdcmsException ex, ...) { ... }
```

When a `ComponentNotFoundException` is thrown, Spring finds the most specific handler.
This is runtime polymorphic dispatch on exception types.

---

## 5. Composition (vs Inheritance)

> **Definition:** Building complex objects from simpler ones by containing references,
> rather than extending them.

### Applied In: `Assembly` ↔ `AssemblyComponent` ↔ `ComponentVersion`

```
Assembly  ←→  AssemblyComponent (junction entity)  ←→  ComponentVersion
```

An `Assembly` is **composed of** component versions — it doesn't extend them.
`AssemblyComponent` is the junction entity that carries extra data: `quantity` and `notes`.

```java
// Assembly.java
@OneToMany(mappedBy = "assembly", cascade = CascadeType.ALL, orphanRemoval = true)
private List<AssemblyComponent> assemblyComponents = new ArrayList<>();
```

### Applied In: `User` and `Role` — Composition Over Inheritance

Instead of `AdminUser extends User` and `EngineerUser extends User`:
```java
// User.java
@Enumerated(EnumType.STRING)
private Role role; // User HAS-A Role, not IS-A Admin
```

**Why composition is better here:**
1. A user's role can change (ENGINEER promoted to ADMIN) — updating one field is trivial.
2. No meaningful behavioral difference at the entity level.
3. Avoids unnecessary JPA discriminator complexity.

---

## 6. Single Responsibility Principle (SRP)

Each class has exactly one reason to change.

| Class | Responsibility |
|---|---|
| `ComponentService` | Component CRUD, type-specific field mapping, deprecation |
| `ComponentVersionService` | Version lifecycle, state machine transitions |
| `DependencyService` | Dependency graph management, DFS cycle detection |
| `CompatibilityService` | Compatibility rule CRUD, validation before assembly add |
| `ImpactAnalysisService` | "What breaks if I deprecate X?" — read-only impact queries |
| `AssemblyService` | BoM composition — add/remove versions, business rules |
| `ComponentMapper` | Entity → DTO mapping only |
| `GlobalExceptionHandler` | HTTP error response translation only |

---

## 7. Open/Closed Principle (OCP)

> **Definition:** Open for extension, closed for modification.

### Applied In: `CompatibilityChecker` Interface

Adding a new compatibility checking strategy requires:
- ✅ Creating a new class implementing `CompatibilityChecker`
- ✅ Registering it as a Spring `@Component`
- ❌ NOT modifying `CompatibilityService` or any caller

```java
// Future extension — zero changes to existing code:
@Component
@Primary
public class MLCompatibilityChecker implements CompatibilityChecker {
    @Override
    public CompatibilityCheckResponse checkAll(List<ComponentVersion> versions) {
        // ML-based checking logic
    }
}
```

---

## 8. Dependency Injection (DI) and Inversion of Control (IoC)

Every service uses **constructor injection** via Lombok `@RequiredArgsConstructor`:

```java
@Service
@RequiredArgsConstructor
public class AssemblyService {
    private final AssemblyRepository assemblyRepository;    // injected
    private final ProjectService projectService;            // injected
    private final ComponentVersionService versionService;  // injected
    private final CompatibilityService compatibilityService; // injected
}
```

**Why constructor injection over field injection?**
- Makes dependencies visible — explicit class contract.
- Makes classes testable — pass mocks to the constructor directly.
- Makes dependencies immutable — they're `final`.
- Prevents circular dependency issues that `@Autowired` on fields can hide.

---

## 9. State Machine — Version Lifecycle

```
     ┌─────────────────────┐
     │                     ↓
  DRAFT ──────────────► APPROVED ──┐
     │                             │
     └─────────────────────────────┴──► DEPRECATED (terminal)
```

```java
// ComponentVersionService.java
public void validateTransition(VersionStatus current, VersionStatus next) {
    Set<VersionStatus> allowedNext = switch (current) {
        case DRAFT      -> Set.of(VersionStatus.APPROVED, VersionStatus.DEPRECATED);
        case APPROVED   -> Set.of(VersionStatus.DEPRECATED);
        case DEPRECATED -> Set.of(); // terminal — no transitions
    };

    if (!allowedNext.contains(next)) {
        throw new InvalidVersionTransitionException(current, next);
    }
}
```

**DEPRECATED is a terminal state** — once a version is retired, it cannot be re-approved.
This models real engineering change control: version history is immutable.

---

## 10. Strategy Pattern

The `CompatibilityChecker` interface + `DefaultCompatibilityChecker` implementation is a
classic **Strategy Pattern**:

```
CompatibilityService ──uses──► CompatibilityChecker (Strategy Interface)
                                         ▲
                             ────────────┴────────────
                             │                       │
                 DefaultCompatibilityChecker    (Future: MLCompatibilityChecker)
                 (checks DB rules)              (uses AI model)
```

The strategy (checker) can be swapped without changing the context (`CompatibilityService`).

---

## Summary

| Principle | Applied Where |
|-----------|--------------|
| **Encapsulation** | All entities (`private` fields + Lombok accessors), services hide DB details |
| **Abstraction** | `CompatibilityChecker` interface, `EdcmsException` abstract class, Repository interfaces |
| **Inheritance** | Component → Mechanical/Electrical/Software (JOINED), EdcmsException hierarchy |
| **Polymorphism** | `ComponentMapper.toResponse()`, `componentRepository.save()`, exception handler dispatch |
| **Composition** | `Assembly` has `AssemblyComponent`s, `User` has `Role` enum |
| **SRP** | Dedicated service classes for each bounded capability |
| **OCP** | `CompatibilityChecker` interface — extend without modifying |
| **DI/IoC** | Constructor injection via `@RequiredArgsConstructor` throughout |
| **State Machine** | `ComponentVersionService.validateTransition()` for DRAFT→APPROVED→DEPRECATED |
| **Strategy Pattern** | `CompatibilityChecker` pluggable implementation |
