# Ventas API - Complete Technical Implementation Guide

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture & Design Patterns](#2-architecture--design-patterns)
3. [Project Structure](#3-project-structure)
4. [Spring Boot Fundamentals](#4-spring-boot-fundamentals)
5. [Configuration Layer](#5-configuration-layer)
6. [Domain Layer](#6-domain-layer)
7. [Persistence Layer](#7-persistence-layer)
8. [Web Layer](#8-web-layer)
9. [Idempotency Filter](#9-idempotency-filter)
10. [Redis Integration](#10-redis-integration)
11. [Exception Handling](#11-exception-handling)
12. [Data Flow Examples](#12-data-flow-examples)
13. [Testing the API](#13-testing-the-api)
14. [Prometheus Metrics Implementation](#14-prometheus-metrics-implementation)

---

## 1. Project Overview

### What is Ventas API?

The Ventas API is a Spring Boot microservice that implements **idempotent order creation** (órdenes de compra) with Redis-backed duplicate detection. It is part of the MATI ARQ SOFTWARE platform (Reto 2), designed around the **Availability** quality attribute by ensuring that duplicate purchase order submissions always return the same result safely without creating duplicate records.

### Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 4.0.4 |
| Database | PostgreSQL | - |
| Cache | Redis | 7 |
| Build Tool | Maven | 3.x |
| ORM | Hibernate/JPA | 6.x |
| Metrics | Micrometer / Prometheus | - |

### Key Features

- **Idempotent Order Creation**: Duplicate POST requests with the same `Idempotencia-Key` always return the same response without re-inserting.
- **Multi-layer Duplicate Detection**: Check Redis first (fast cache), then PostgreSQL (authoritative) before processing a new order.
- **Prometheus Metrics**: Custom `Timer` (`orden.tiempo.total`) measuring end-to-end order processing time, including percentile histograms.
- **Request Validation**: Bean Validation (`@Valid`) on all inbound requests; UUID format validation in the filter layer.

---

## 2. Architecture & Design Patterns

### Layered Architecture

The project follows a **clean layered architecture** that separates concerns:

```
┌─────────────────────────────────────────────────────────────────┐
│                         WEB LAYER                                │
│  Controllers, Filters, Request/Response DTOs, Web Mappers       │
│  Handles HTTP requests, validation, response formatting         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       DOMAIN LAYER                               │
│  Services, Repository Interfaces, Domain DTOs, Enums            │
│  Contains business logic, defines contracts                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PERSISTENCE LAYER                             │
│  Entities, CRUD Repositories, Repository Implementations        │
│  Handles database operations, entity mapping                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   INFRASTRUCTURE / CONFIG                        │
│  Redis Store, Jackson Config, Metrics Config                     │
│  External integrations, cross-cutting concerns                   │
└─────────────────────────────────────────────────────────────────┘
```

### Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Repository Pattern** | Domain/Persistence | Abstracts data access, enables testing |
| **DTO Pattern** | All layers | Decouples layers, controls data exposure |
| **Mapper Pattern** | Persistence/Web | Converts between entities and DTOs |
| **Filter Pattern** | Web layer | Validates `Idempotencia-Key` before reaching the controller |
| **Builder Pattern** | DTOs, Entities | Clean object construction |
| **Dependency Injection** | Everywhere | Loose coupling, testability |

### Idempotency Pattern

The core business pattern is **idempotent API design**:

```
Client sends POST /api/orden_compra
          with header: Idempotencia-Key: <UUID>
                │
                ▼
   ┌────────────────────────┐
   │ IdempotenciaFilter      │  validates UUID format
   └────────────────────────┘
                │
                ▼
   ┌────────────────────────┐
   │ OrdenCompraService      │
   │  1. Check Redis cache   │──► Found? → return cached response
   │  2. Check PostgreSQL    │──► Found? → return DB response
   │  3. Save to DB + Redis  │──► New order → persist and cache
   └────────────────────────┘
```

---

## 3. Project Structure

```
src/main/java/com/uniandes/ventas/
├── VentasApiApplication.java          # Entry point
├── domain/
│   ├── model/                         # DTOs and Enums
│   │   ├── OrdenCompraDTO.java        # Internal domain object
│   │   └── OrdenCompraStatus.java     # Enum: PROCESO, PENDIENTE, CANCELADA, PAGADA
│   ├── repository/                    # Repository interfaces
│   │   └── OrdenCompraRepository.java
│   ├── service/                       # Business logic
│   │   └── OrdenCompraService.java
│   ├── exception/                     # Global exception handling
│   │   └── VentasExceptionHandler.java
│   └── util/                          # Constants
│       └── ConstantsVentas.java
├── persistence/
│   ├── entity/                        # JPA Entities
│   │   └── OrdenCompra.java
│   ├── crud/                          # JPA Repositories
│   │   └── CrudOrdenCompraEntity.java
│   ├── impl/                          # Repository implementations
│   │   └── OrdenCompraRepositoryImpl.java
│   └── mapper/                        # Entity <-> DTO mappers
│       └── OrdenCompraEntityMapper.java
└── web/
    ├── controller/                    # REST Controllers
    │   └── OrdenCompraController.java
    ├── dto/                           # Request/Response DTOs
    │   ├── OrdenCompraRequest.java
    │   ├── OrdenCompraResponse.java
    │   └── ErrorResponse.java
    ├── filter/                        # Servlet Filters
    │   └── IdempotenciaFilter.java
    ├── mapper/                        # Web layer mappers
    │   └── OrdenCompraMapper.java
    └── config/                        # Configuration classes
        ├── JacksonConfig.java
        ├── MetricsConfig.java
        └── RedisConfig.java

src/main/resources/
├── application.yaml                   # Main configuration
└── application-dev.yaml               # Development profile
```

---

## 4. Spring Boot Fundamentals

### Main Application Class

```java
@SpringBootApplication
public class VentasApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(VentasApiApplication.class, args);
    }
}
```

#### Annotations Explained

| Annotation | Purpose |
|------------|---------|
| `@SpringBootApplication` | Combines `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`. Marks this as the main configuration class and scans all sub-packages automatically. |

### Component Scanning

Spring Boot automatically scans for components in the package containing `@SpringBootApplication` and all sub-packages. Components are detected via stereotype annotations:

| Annotation | Layer | Purpose |
|------------|-------|---------|
| `@Component` | Any | Generic Spring-managed component |
| `@Service` | Domain | Business logic services |
| `@Repository` | Persistence | Data access components |
| `@RestController` | Web | HTTP request handlers |
| `@Configuration` | Config | Configuration classes |

---

## 5. Configuration Layer

### application.yaml - Main Configuration

```yaml
spring:
  application:
    name: ventas-api              # Application identifier
  profiles:
    active: dev                   # Active profile (loads application-dev.yaml)
  data:
    redis:
      host: ${REDIS_HOST:localhost}  # Env var with default fallback
      port: ${REDIS_PORT:6379}
      connect-timeout: 2000ms
      timeout: 1000ms

server:
  port: 8080
  servlet:
    context-path: /ventas-api     # Base path for all endpoints
  tomcat:
    threads:
      max: 50                     # Max worker threads
      min-spare: 10               # Keep 10 threads ready
    accept-count: 100             # Queue size when all threads busy
    max-connections: 10000        # Max concurrent connections
    connection-timeout: 20000

management:
  endpoints:
    web:
      exposure:
        include: "*"              # All actuator endpoints exposed
  metrics:
    distribution:
      percentiles-histogram:
        "orden.tiempo.total": true
      percentiles:
        "orden_tiempo_total": 0.95
```

#### Why These Settings Matter

| Setting | Purpose |
|---------|---------|
| `context-path: /ventas-api` | All endpoints prefixed: `POST /ventas-api/api/orden_compra` |
| `threads.max: 50` | Limits concurrent request processing, prevents resource exhaustion |
| `redis.timeout: 1000ms` | Fails fast if Redis is slow, protecting response times |
| `include: "*"` | Exposes health, metrics, prometheus endpoints for monitoring |

### application-dev.yaml - Development Profile

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/ventas-api-db?sslmode=disable&currentSchema=public
    username: appuser
    password: secret

  jpa:
    show-sql: false
    hibernate:
      ddl-auto: update            # Auto-create/update schema from entities
    properties:
      hibernate:
        format_sql: true
        jdbc:
          batch_size: 20          # Batch insert/update operations
        order_inserts: true
        order_updates: true
  sql:
    init:
      mode: always                # Run SQL init scripts on startup
```

#### HikariCP Connection Pool

HikariCP is the default JDBC connection pool in Spring Boot. Why pool connections?

```
Without Pool:                    With Pool:
─────────────────                ─────────────────
Request → Open Connection        Request → Get from Pool
        → Execute Query                  → Execute Query
        → Close Connection               → Return to Pool
        (Expensive: ~50-100ms)           (Fast: ~1ms)
```

### Custom Configuration: JacksonConfig

```java
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}
```

#### Why This Is Needed

By default, Jackson serializes `LocalDateTime` as a numeric timestamp array (e.g., `[2024, 1, 15, 10, 30]`). This configuration:

| Action | Effect |
|--------|--------|
| `registerModule(new JavaTimeModule())` | Adds support for Java 8 date/time types |
| `WRITE_DATES_AS_TIMESTAMPS = false` | Serializes dates as ISO strings: `"2024-01-15T10:30:00"` |
| `@Primary` | Makes this the default `ObjectMapper` bean used by Jackson and `StringRedisTemplate` serialization |

---

## 6. Domain Layer

The domain layer contains **business logic** and defines **contracts** (interfaces) that the persistence layer implements.

### Domain DTO

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraDTO implements Serializable {

    private Long idOrdenCompra;
    private String idFactura;
    private Integer idCliente;
    private LocalDateTime fechaCompra;
    private OrdenCompraStatus estado;
    private BigDecimal total;
}
```

`Serializable` is implemented to support serialization if needed (e.g., caching strategies).

#### Lombok Annotations Explained

| Annotation | Generated Code |
|------------|----------------|
| `@Data` | `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode`, `@RequiredArgsConstructor` |
| `@Builder` | Builder pattern: `OrdenCompraDTO.builder().idFactura("x").build()` |
| `@NoArgsConstructor` | No-argument constructor (required by JPA/Jackson) |
| `@AllArgsConstructor` | Constructor with all fields (used by Builder) |

### Enum: OrdenCompraStatus

```java
public enum OrdenCompraStatus {
    PROCESO,    // Order is being processed
    PENDIENTE,  // Awaiting payment or confirmation
    CANCELADA,  // Order was cancelled
    PAGADA      // Payment confirmed — set automatically on creation
}
```

> **Note**: When a new order is created via `POST /api/orden_compra`, the service always sets the status to `PAGADA` before persisting.

### Repository Interface

```java
public interface OrdenCompraRepository {
    OrdenCompraDTO save(OrdenCompraDTO order);
    Optional<OrdenCompraDTO> findById(Long id);
    List<OrdenCompraDTO> findAll();
    void deleteById(Long id);
    Optional<OrdenCompraDTO> findByIdFactura(String idFactura);  // Key for idempotency check
}
```

#### Why Interfaces in Domain Layer?

```
Domain Layer                      Persistence Layer
──────────────────────────        ──────────────────────────────────
┌──────────────────────────┐      ┌──────────────────────────────────┐
│ OrdenCompraRepository    │◄─────│ OrdenCompraRepositoryImpl        │
│ (interface)              │      │ (JPA implementation)             │
└──────────────────────────┘      └──────────────────────────────────┘
```

**Benefits:**
1. **Testability**: Mock the interface for unit tests
2. **Decoupling**: Domain doesn't know about JPA/Hibernate details
3. **Flexibility**: Can swap implementations (JPA → MongoDB)

### Domain Service: OrdenCompraService

```java
@Service
@RequiredArgsConstructor
public class OrdenCompraService {

    private final Timer ordenTimer;
    private final OrdenCompraRepositoryImpl ordenCompraRepositoryImpl;
    private final OrdenCompraMapper ordenCompraMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrdenCompraResponse crearOrdenCompra(OrdenCompraRequest request,
                                                String idempotencyKey,
                                                Timer.Sample sample) {
        String redisKey = ConstantsVentas.REDIS_PREFIX + idempotencyKey;
        request.setEstado(OrdenCompraStatus.PAGADA);
        request.setFechaCompra(LocalDateTime.now());

        // 1. Check Redis cache
        String cachedJson = redisTemplate.opsForValue().get(redisKey);
        if (cachedJson != null) {
            sample.stop(ordenTimer);
            log.warn("Duplicate operation detected. UUID: {}", idempotencyKey);
            return objectMapper.readValue(cachedJson, OrdenCompraResponse.class);
        }

        // 2. Check PostgreSQL
        var existente = ordenCompraRepositoryImpl.findByIdFactura(request.getIdFactura());
        if (existente.isPresent()) {
            sample.stop(ordenTimer);
            OrdenCompraResponse response = ordenCompraMapper.fromDTO(existente.get());
            RequestContextHolder.getRequestAttributes()
                .setAttribute("ordenCompraResponse", response, RequestAttributes.SCOPE_REQUEST);
            return response;
        }

        // 3. Save new order
        OrdenCompraDTO dto = ordenCompraMapper.fromOrderCompraRequest(request);
        dto = ordenCompraRepositoryImpl.save(dto);
        OrdenCompraResponse response = ordenCompraMapper.fromDTO(dto);

        // 4. Cache for 15 minutes
        saveInCache(redisKey, response);
        return response;
    }
}
```

| Annotation/Method | Purpose |
|-------------------|---------|
| `@Service` | Marks as a service component; business logic container |
| `@Transactional` | Wraps method in a DB transaction; rolls back on unchecked exceptions |
| `@RequiredArgsConstructor` | Injects `final` dependencies via constructor (Lombok) |
| `sample.stop(ordenTimer)` | Stops the Micrometer timer and records the measurement |

### Constants

```java
public class ConstantsVentas {
    public static final String REDIS_PREFIX = "idempotency:orden:";
    public static final String UUID_PATTERN =
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
}
```

| Constant | Usage |
|----------|-------|
| `REDIS_PREFIX` | Namespaces Redis keys: `idempotency:orden:<uuid>` |
| `UUID_PATTERN` | Regex validated in `IdempotenciaFilter` |

---

## 7. Persistence Layer

### JPA Entity: OrdenCompra

```java
@Entity
@Table(name = "orden_compra", schema = "public",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ordencompra_idFactura", columnNames = "id_factura")
    })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden_compra")
    @ToString.Include
    private Long idOrdenCompra;

    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;

    @Column(name = "id_factura", length = 120, nullable = false, unique = true)
    @ToString.Include
    private String idFactura;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 50, nullable = false)
    private OrdenCompraStatus estado;

    @Column(name = "total", precision = 18, scale = 2, nullable = false)
    private BigDecimal total;
}
```

#### JPA Annotations Explained

| Annotation | Purpose |
|------------|---------|
| `@Entity` | Marks class as JPA entity (maps to a DB table) |
| `@Table(name = "...")` | Specifies exact table name and schema |
| `@UniqueConstraint` | Creates a DB-level unique index on `id_factura` |
| `@Id` | Marks primary key field |
| `@GeneratedValue(strategy = IDENTITY)` | Auto-increment (PostgreSQL SERIAL) |
| `@Column(nullable = false)` | NOT NULL constraint at DB level |
| `@Enumerated(EnumType.STRING)` | Stores enum as string (not ordinal number) |
| `@NoArgsConstructor(access = PROTECTED)` | JPA requires a no-arg constructor; `PROTECTED` discourages direct use |
| `@ToString(onlyExplicitlyIncluded = true)` | Only includes `@ToString.Include` fields, avoids lazy-load issues |

#### Database Schema (auto-generated by `ddl-auto: update`)

```sql
CREATE TABLE public.orden_compra (
    id_orden_compra BIGSERIAL PRIMARY KEY,
    id_cliente      INTEGER         NOT NULL,
    id_factura      VARCHAR(120)    NOT NULL UNIQUE,
    fecha_compra    TIMESTAMP       NOT NULL,
    estado          VARCHAR(50)     NOT NULL,
    total           NUMERIC(18, 2)  NOT NULL,
    CONSTRAINT uk_ordencompra_idFactura UNIQUE (id_factura)
);
```

### CRUD Repository

```java
public interface CrudOrdenCompraEntity extends JpaRepository<OrdenCompra, Long> {

    Optional<OrdenCompra> findByIdFactura(String idFactura);
}
```

#### How JpaRepository Works

By extending `JpaRepository`, Spring Data automatically provides:

| Method | Generated SQL |
|--------|---------------|
| `save(entity)` | `INSERT` or `UPDATE` |
| `saveAndFlush(entity)` | `INSERT`/`UPDATE` + immediate flush to DB |
| `findById(id)` | `SELECT ... WHERE id = ?` |
| `findAll()` | `SELECT * FROM orden_compra` |
| `deleteById(id)` | `DELETE FROM ... WHERE id = ?` |
| `findByIdFactura(idFactura)` | `SELECT ... WHERE id_factura = ?` (derived query) |

> **Derived Queries**: Spring Data generates SQL from the method name. `findByIdFactura` → `WHERE id_factura = ?`.

### Repository Implementation

```java
@Repository
public class OrdenCompraRepositoryImpl implements OrdenCompraRepository {

    private final CrudOrdenCompraEntity crudRepository;
    private final OrdenCompraEntityMapper mapper;

    @Override
    public OrdenCompraDTO save(OrdenCompraDTO ordenCompraDTO) {
        OrdenCompra entity = mapper.fromDTO(ordenCompraDTO);
        OrdenCompra saved = crudRepository.saveAndFlush(entity);  // Flush ensures ID is populated
        return mapper.fromEntity(saved);
    }

    @Override
    public Optional<OrdenCompraDTO> findByIdFactura(String idFactura) {
        return crudRepository.findByIdFactura(idFactura)
                .map(mapper::fromEntity);
    }
}
```

> `saveAndFlush` is used instead of `save` to force the SQL to execute immediately and return the DB-generated `idOrdenCompra` within the same transaction.

### Entity Mapper

```java
@Component
public class OrdenCompraEntityMapper {

    public OrdenCompraDTO fromEntity(OrdenCompra entity) {
        return OrdenCompraDTO.builder()
                .idOrdenCompra(entity.getIdOrdenCompra())
                .idCliente(entity.getIdCliente())
                .idFactura(entity.getIdFactura())
                .fechaCompra(entity.getFechaCompra())
                .estado(entity.getEstado())
                .total(entity.getTotal())
                .build();
    }

    public OrdenCompra fromDTO(OrdenCompraDTO dto) {
        return OrdenCompra.builder()
                .idOrdenCompra(dto.getIdOrdenCompra())
                .idCliente(dto.getIdCliente())
                .idFactura(dto.getIdFactura())
                .fechaCompra(dto.getFechaCompra())
                .estado(dto.getEstado())
                .total(dto.getTotal())
                .build();
    }
}
```

**Why a separate mapper?** It prevents the domain layer from depending on JPA entity classes, keeping layers cleanly separated.

---

## 8. Web Layer

### REST Controller: OrdenCompraController

```java
@RestController
@RequestMapping("/api/orden_compra")
@RequiredArgsConstructor
public class OrdenCompraController {

    @Qualifier("ordenTimer")
    private final Timer ordenTimer;
    private final MeterRegistry registry;
    private final OrdenCompraService ordenCompraService;

    @PostMapping
    public ResponseEntity<OrdenCompraResponse> crearOrdenCompra(
            @Valid @RequestBody OrdenCompraRequest request,
            @RequestHeader("Idempotencia-Key") String idempotencyKey) {

        Timer.Sample sample = Timer.start(registry);
        OrdenCompraResponse response = ordenCompraService.crearOrdenCompra(request, idempotencyKey, sample);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Idempotencia-Key", idempotencyKey)
                .body(response);
    }
}
```

#### Annotations Explained

| Annotation | Purpose |
|------------|---------|
| `@RestController` | Combines `@Controller` + `@ResponseBody`; all methods return JSON |
| `@RequestMapping("/api/orden_compra")` | Base path; full URL is `/ventas-api/api/orden_compra` |
| `@PostMapping` | Maps HTTP POST requests to this method |
| `@Valid` | Triggers Bean Validation on the request body |
| `@RequestBody` | Deserializes JSON body to `OrdenCompraRequest` |
| `@RequestHeader("Idempotencia-Key")` | Extracts required header value |
| `@Qualifier("ordenTimer")` | Selects the specific `Timer` bean named `"ordenTimer"` |
| `Timer.Sample sample = Timer.start(registry)` | Starts timing before service call |

#### Full Endpoint

```
POST /ventas-api/api/orden_compra
Headers:
  Content-Type: application/json
  Idempotencia-Key: <UUID>

Response: 201 Created
Headers:
  Idempotencia-Key: <same UUID echoed back>
Body: OrdenCompraResponse JSON
```

### Request DTO: OrdenCompraRequest

```java
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OrdenCompraRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    @Min(0)
    private Integer idCliente;

    @NotBlank(message = "El idFactura es obligatorio y no puede estar vacío")
    @Size(min = 5, max = 50, message = "El idFactura debe tener entre 5 y 50 caracteres")
    private String idFactura;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaCompra = LocalDateTime.now();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private OrdenCompraStatus estado;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "El total debe ser > 0")
    @Digits(integer = 16, fraction = 2)
    private BigDecimal total;
}
```

#### Validation Annotations

| Annotation | Rule |
|------------|------|
| `@NotNull` | Field must not be null |
| `@Min(0)` | Number must be ≥ 0 |
| `@NotBlank` | String must not be null, empty, or whitespace |
| `@Size(min, max)` | String length range |
| `@DecimalMin(value, inclusive)` | Decimal must be > 0 |
| `@Digits(integer, fraction)` | Max digits before/after decimal point |
| `@JsonProperty(READ_ONLY)` | Field is excluded from deserialization (client cannot set it) |
| `@JsonFormat(pattern = ...)` | Controls date serialization format |

### Response DTO: OrdenCompraResponse

```java
@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class OrdenCompraResponse {
    private Long idOrdenCompra;
    private String idFactura;
    private Integer idCliente;
    private LocalDateTime fechaCompra;
    private OrdenCompraStatus estado;
    private BigDecimal total;
}
```

### Web Mapper: OrdenCompraMapper

```java
@Component
public class OrdenCompraMapper {

    public static OrdenCompraDTO fromOrderCompraRequest(OrdenCompraRequest entity) {
        return OrdenCompraDTO.builder()
                .idCliente(entity.getIdCliente())
                .idFactura(entity.getIdFactura())
                .fechaCompra(entity.getFechaCompra())
                .estado(entity.getEstado())
                .total(entity.getTotal())
                .build();
    }

    public static OrdenCompraResponse fromDTO(OrdenCompraDTO oc) {
        return OrdenCompraResponse.builder()
                .idOrdenCompra(oc.getIdOrdenCompra())
                .idFactura(oc.getIdFactura())
                .idCliente(oc.getIdCliente())
                .fechaCompra(oc.getFechaCompra())
                .estado(oc.getEstado())
                .total(oc.getTotal())
                .build();
    }
}
```

**Note**: `idOrdenCompra` is deliberately excluded from `fromOrderCompraRequest` because the ID is DB-generated and unknown at request time.

---

## 9. Idempotency Filter

### IdempotenciaFilter

```java
@Component
public class IdempotenciaFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator");  // Skip filter for health checks
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String key = request.getHeader("Idempotencia-Key");

        if (key == null || key.isBlank()) {
            sendErrorResponse(response, SC_BAD_REQUEST,
                "Formato de 'Idempotency-Key' inválido. Se espera un UUID.");
            return;
        }

        if (!key.matches(ConstantsVentas.UUID_PATTERN)) {
            sendErrorResponse(response, SC_BAD_REQUEST,
                "Formato de 'Idempotency-Key' inválido. Se espera un UUID.");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

#### How OncePerRequestFilter Works

```
HTTP Request arrives
        │
        ▼
┌──────────────────────────┐
│  IdempotenciaFilter       │
│  (OncePerRequestFilter)   │
│  1. Check if /actuator    │──► yes → skip filter, pass through
│  2. Check header exists   │──► null/blank → 400 Bad Request
│  3. Validate UUID format  │──► invalid → 400 Bad Request
│  4. filterChain.doFilter  │──► OK → pass to controller
└──────────────────────────┘
```

#### Why OncePerRequestFilter?

In Spring MVC, filters can be invoked multiple times for a single logical request (e.g., during forwards/includes). `OncePerRequestFilter` guarantees the filter runs exactly once per HTTP request.

| Method | Purpose |
|--------|---------|
| `shouldNotFilter(request)` | Return `true` to bypass the filter for matching paths |
| `doFilterInternal(...)` | The actual filter logic |
| `filterChain.doFilter(...)` | Passes the request to the next filter or controller |

#### Filter vs Controller Validation

| | Filter | Controller |
|--|--------|------------|
| When | Before the controller | Inside the controller |
| What | Structural validation (header format) | Business validation (`@Valid`) |
| On failure | Returns response directly | Throws exception → ExceptionHandler |

---

## 10. Redis Integration

### RedisConfig

```java
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
```

`StringRedisTemplate` is a specialized `RedisTemplate<String, String>` — all keys and values are plain strings. This works well with JSON serialization from `ObjectMapper`.

### How Idempotency Caching Works

```
Key format: "idempotency:orden:<UUID>"
Value:       JSON string of OrdenCompraResponse
TTL:         15 minutes
```

```java
// Write to cache
String json = objectMapper.writeValueAsString(response);
redisTemplate.opsForValue().set(redisKey, json, Duration.ofMinutes(15));

// Read from cache
String cachedJson = redisTemplate.opsForValue().get(redisKey);
if (cachedJson != null) {
    return objectMapper.readValue(cachedJson, OrdenCompraResponse.class);
}
```

### Why 15-Minute TTL?

The TTL is a trade-off:
- **Too short**: Cache misses force DB lookups, defeating the purpose of caching
- **Too long**: Stale data stays in Redis longer than needed; memory usage grows

15 minutes is a reasonable window within which a client would retry a failed request, after which retries are unlikely.

### Redis Connection Configuration

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}   # Can be overridden by environment variable
      port: ${REDIS_PORT:6379}
      connect-timeout: 2000ms         # Fail fast if Redis is unreachable
      timeout: 1000ms                 # Fail fast for individual operations
```

### Multi-layer Duplicate Detection Strategy

```
Layer 1: Redis Cache (fast, ~1ms)
  ↓ miss
Layer 2: PostgreSQL (authoritative, ~5-10ms)
  ↓ miss
Layer 3: Insert new record (DB + cache population)
```

This two-layer approach handles the case where the Redis TTL expires but the order still exists in PostgreSQL.

---

## 11. Exception Handling

### VentasExceptionHandler

```java
@RestControllerAdvice
@RequiredArgsConstructor
public class VentasExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_PARAMETER", ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleIntegrity(DataIntegrityViolationException ex) {
        // Read order from request-scoped session (set in the service layer)
        OrdenCompraResponse ordenCompraResponse =
            (OrdenCompraResponse) RequestContextHolder.getRequestAttributes()
                .getAttribute("ordenCompraResponse", RequestAttributes.SCOPE_REQUEST);

        if (ordenCompraResponse != null) {
            // Race condition: two concurrent requests with same idFactura
            // One lost the DB race but can still return a valid response
            return ResponseEntity.ok(ordenCompraResponse);
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DATA_INTEGRITY_ERROR",
                        "Error de integridad, la transacción ya existe pero no se pudo recuperar el detalle.",
                        LocalDateTime.now()));
    }
}
```

#### How @RestControllerAdvice Works

```
Exception thrown in service/controller
              │
              ▼
    ┌───────────────────────┐
    │  VentasExceptionHandler│
    │  @RestControllerAdvice │
    │                        │
    │  Matches exception type│
    │  → builds HTTP response│
    └───────────────────────┘
```

| Annotation | Purpose |
|------------|---------|
| `@RestControllerAdvice` | Intercepts exceptions from all controllers; responses are JSON |
| `@ExceptionHandler(X.class)` | Handles exceptions of type X |

#### The Race Condition Scenario

`DataIntegrityViolationException` can still occur despite the pre-checks:

```
Request A                      Request B
    │                              │
    ├── Check Redis → miss         │
    ├── Check DB    → miss         ├── Check Redis → miss
    │                              ├── Check DB    → miss
    ├── INSERT (wins) ✓            │
    │                              ├── INSERT → UNIQUE violation ✗
    │                              │     └── DataIntegrityViolationException
    │                              │         → Handler reads session → returns 200
```

The service stores the found `OrdenCompraResponse` in request scope (`RequestAttributes.SCOPE_REQUEST`) so the exception handler can recover gracefully.

### ErrorResponse

```java
public class ErrorResponse {
    private String code;
    private String message;
    private LocalDateTime timestamp;
}
```

Example error JSON:
```json
{
  "code": "INVALID_PARAMETER",
  "message": "El idFactura es obligatorio y no puede estar vacío",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## 12. Data Flow Examples

### Happy Path: New Order

```
POST /ventas-api/api/orden_compra
Headers: Idempotencia-Key: 550e8400-e29b-41d4-a716-446655440000
Body:
{
  "idCliente": 42,
  "idFactura": "FACT-2024-001",
  "total": 149.99
}

─────────────────────────────────────────────────
1. IdempotenciaFilter
   └── Header present? ✓
   └── Valid UUID? ✓
   └── filterChain.doFilter()

2. OrdenCompraController.crearOrdenCompra()
   └── @Valid validates: idCliente ≥ 0 ✓, idFactura length ✓, total > 0 ✓
   └── Timer.Sample started
   └── service.crearOrdenCompra(request, key, sample)

3. OrdenCompraService
   └── redisKey = "idempotency:orden:550e8400-..."
   └── Redis GET → null (not cached)
   └── DB findByIdFactura("FACT-2024-001") → empty
   └── Set estado = PAGADA, fechaCompra = now()
   └── Map Request → DTO → Entity
   └── saveAndFlush(entity) → id = 1001
   └── Map Entity → DTO → Response
   └── Redis SET redisKey = JSON, TTL = 15 min
   └── return response

4. Controller
   └── sample.stop() ← NOT called here (service forgot on happy path)
   └── return 201 Created
       Header: Idempotencia-Key: 550e8400-...
       Body: { "idOrdenCompra": 1001, "idFactura": "FACT-2024-001", ... }
```

### Duplicate Request (Redis Cache Hit)

```
POST /ventas-api/api/orden_compra
Headers: Idempotencia-Key: 550e8400-e29b-41d4-a716-446655440000  ← same UUID

─────────────────────────────────────────────────
1. Filter passes (same UUID still valid)

2. OrdenCompraService
   └── Redis GET → "{\"idOrdenCompra\":1001,...}"  ← cache hit!
   └── sample.stop(ordenTimer)  ← timer stops here (fast path)
   └── log.warn("Duplicate operation detected...")
   └── return deserializedResponse (same as first time)

3. Controller returns 201 Created (idempotent!)
```

### Duplicate Request (DB Fallback)

```
POST /ventas-api/api/orden_compra
Headers: Idempotencia-Key: 550e8400-e29b-41d4-a716-446655440000  ← same UUID
         (but Redis TTL expired)

─────────────────────────────────────────────────
1. Filter passes

2. OrdenCompraService
   └── Redis GET → null (TTL expired)
   └── DB findByIdFactura("FACT-2024-001") → present
   └── sample.stop(ordenTimer)
   └── Store response in RequestAttributes (for race condition safety)
   └── return existingResponse

3. Controller returns 201 Created
```

---

## 13. Testing the API

### Prerequisites

1. PostgreSQL running with database `ventas-api-db`
2. Redis running on `localhost:6379`
3. Application started: `./mvnw spring-boot:run`

### Test 1: Create a New Order (Happy Path)

```bash
curl -X POST http://localhost:8080/ventas-api/api/orden_compra \
  -H "Content-Type: application/json" \
  -H "Idempotencia-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "idCliente": 42,
    "idFactura": "FACT-2024-001",
    "total": 149.99
  }'
```

**Expected response (201 Created):**
```json
{
  "idOrdenCompra": 1001,
  "idFactura": "FACT-2024-001",
  "idCliente": 42,
  "fechaCompra": "2024-01-15T10:30:00",
  "estado": "PAGADA",
  "total": 149.99
}
```

### Test 2: Duplicate Request (Same Idempotencia-Key)

Run the **exact same** `curl` command again. The response must be **identical** to Test 1 with status 201.

### Test 3: Missing Idempotencia-Key

```bash
curl -X POST http://localhost:8080/ventas-api/api/orden_compra \
  -H "Content-Type: application/json" \
  -d '{"idCliente": 1, "idFactura": "FACT-001", "total": 10.00}'
```

**Expected response (400 Bad Request):**
```json
{
  "error": "Formato de 'Idempotency-Key' inválido. Se espera un UUID."
}
```

### Test 4: Invalid UUID Format

```bash
curl -X POST http://localhost:8080/ventas-api/api/orden_compra \
  -H "Content-Type: application/json" \
  -H "Idempotencia-Key: not-a-valid-uuid" \
  -d '{"idCliente": 1, "idFactura": "FACT-001", "total": 10.00}'
```

**Expected response (400 Bad Request):**
```json
{
  "error": "Formato de 'Idempotency-Key' inválido. Se espera un UUID."
}
```

### Test 5: Invalid Request Body

```bash
curl -X POST http://localhost:8080/ventas-api/api/orden_compra \
  -H "Content-Type: application/json" \
  -H "Idempotencia-Key: 550e8400-e29b-41d4-a716-446655440001" \
  -d '{"idCliente": 1, "total": -5.00}'
```

**Expected response (400 Bad Request)** from Bean Validation.

### Test 6: Health Check

```bash
curl http://localhost:8080/ventas-api/actuator/health
```

**Expected response (200 OK):**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

### Test 7: Prometheus Metrics

```bash
curl http://localhost:8080/ventas-api/actuator/prometheus | grep orden_tiempo
```

**Expected output (sample):**
```
orden_tiempo_total_seconds_bucket{le="0.001"} 0.0
orden_tiempo_total_seconds_bucket{le="0.005"} 3.0
...
orden_tiempo_total_seconds_count 5.0
orden_tiempo_total_seconds_sum 0.145
```

---

## 14. Prometheus Metrics Implementation

### MetricsConfig

```java
@Configuration
public class MetricsConfig {

    @Bean(name = "ordenTimer")
    public Timer ordenTimer(MeterRegistry registry) {
        return Timer.builder("orden.tiempo.total")
                .publishPercentiles(0.5, 0.95)          // p50, p95 pre-computed
                .publishPercentileHistogram(true)        // full histogram for Prometheus
                .register(registry);
    }
}
```

### How the Timer Works

```
Controller                     Service
    │                             │
    ├── Timer.Sample sample       │
    │   = Timer.start(registry)   │
    │                             │
    │──────────────────────────►  │
    │                             ├── Redis check (if hit → sample.stop())
    │                             ├── DB check    (if hit → sample.stop())
    │                             ├── New order   → sample NOT stopped here
    │◄─────────────────────────── │
    │                             │
    └── (controller does not stop the timer on happy path)
```

> **Note**: On the new order (happy path), `sample.stop(ordenTimer)` is never called. This is a current implementation detail — the timer effectively only records measurements for duplicate detection paths. If full end-to-end measurement is desired, `sample.stop()` should also be called at the end of the happy path.

### Metric Name Mapping

| YAML key | Prometheus metric |
|----------|-------------------|
| `orden.tiempo.total` | `orden_tiempo_total_seconds` |

Micrometer automatically appends `_seconds`, `_count`, `_sum`, and `_bucket` suffixes.

### Prometheus Configuration (application.yaml)

```yaml
management:
  metrics:
    distribution:
      percentiles-histogram:
        "orden.tiempo.total": true    # Export full histogram buckets
      percentiles:
        "orden_tiempo_total": 0.95    # Export p95
```

### Example Prometheus Queries

```promql
# Average order processing time (last 5 minutes)
rate(orden_tiempo_total_seconds_sum[5m])
  / rate(orden_tiempo_total_seconds_count[5m])

# 95th percentile latency
histogram_quantile(0.95, rate(orden_tiempo_total_seconds_bucket[5m]))

# Total number of duplicate detections
increase(orden_tiempo_total_seconds_count[1h])
```

### Actuator Endpoints Summary

| Endpoint | URL | Purpose |
|----------|-----|---------|
| Health | `/ventas-api/actuator/health` | Service + Redis + DB health |
| Metrics | `/ventas-api/actuator/metrics` | All metric names |
| Prometheus | `/ventas-api/actuator/prometheus` | Prometheus scrape endpoint |
| Info | `/ventas-api/actuator/info` | Application info |
| Thread Dump | `/ventas-api/actuator/threaddump` | Active threads |
