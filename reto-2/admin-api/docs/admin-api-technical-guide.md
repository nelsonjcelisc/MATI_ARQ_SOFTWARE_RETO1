# Admin API - Complete Technical Implementation Guide

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture & Design Patterns](#2-architecture--design-patterns)
3. [Project Structure](#3-project-structure)
4. [Spring Boot Fundamentals](#4-spring-boot-fundamentals)
5. [Configuration Layer](#5-configuration-layer)
6. [Domain Layer](#6-domain-layer)
7. [Persistence Layer](#7-persistence-layer)
8. [Web Layer](#8-web-layer)
9. [Infrastructure Layer](#9-infrastructure-layer)
10. [Security: Context Scorer Filter](#10-security-context-scorer-filter)
11. [Redis Integration](#11-redis-integration)
12. [Data Flow Examples](#12-data-flow-examples)
13. [Testing the API](#13-testing-the-api)
14. [Prometheus Metrics Implementation](#14-prometheus-metrics-implementation)

---

## 1. Project Overview

### What is Admin API?

The Admin API is a Spring Boot microservice that implements **Defense in Depth** security for administrative operations. It's part of a larger CCP Security Platform designed to detect and block anomalous access attempts using stolen admin tokens.

### Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 4.0.5 |
| Database | PostgreSQL | 15 |
| Cache | Redis | 7 |
| Build Tool | Maven | 3.x |
| ORM | Hibernate/JPA | 6.x |

### Key Features

- **Vendor Commission Management**: CRUD operations for vendor commissions
- **Context-Based Security**: Anomaly detection based on device, IP, hour, and user-agent
- **Redis Profile Caching**: Fast lookups for admin behavioral profiles
- **Async Event Logging**: Non-blocking persistence of security events

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
│                   INFRASTRUCTURE LAYER                           │
│  Redis Store, Cache Loaders, Configuration Properties           │
│  External integrations, cross-cutting concerns                   │
└─────────────────────────────────────────────────────────────────┘
```

### Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Repository Pattern** | Domain/Persistence | Abstracts data access, enables testing |
| **DTO Pattern** | All layers | Decouples layers, controls data exposure |
| **Mapper Pattern** | Persistence/Web | Converts between entities and DTOs |
| **Filter Pattern** | Web layer | Intercepts requests for security |
| **Builder Pattern** | DTOs, Entities | Clean object construction |
| **Dependency Injection** | Everywhere | Loose coupling, testability |

---

## 3. Project Structure

```
src/main/java/com/uniandes/admin_api/
├── AdminApiApplication.java          # Entry point
├── domain/
│   ├── model/                         # DTOs and Enums
│   │   ├── AdminProfileDTO.java
│   │   ├── AdminContextEventDTO.java
│   │   ├── VendorCommissionDTO.java
│   │   ├── AdminProfileStatus.java    # Enum: ACTIVE, LEARNING, BLOCKED
│   │   ├── ContextType.java           # Enum: DEVICE, IP, HOUR, etc.
│   │   └── Decision.java              # Enum: ALLOWED, BLOCKED
│   ├── repository/                    # Repository interfaces
│   │   ├── AdminProfileRepository.java
│   │   ├── AdminContextEventRepository.java
│   │   └── VendorCommissionRepository.java
│   └── service/                       # Business logic
│       ├── CommissionService.java
│       └── ContextScoringService.java
├── persistence/
│   ├── entity/                        # JPA Entities
│   │   ├── AdminProfile.java
│   │   ├── AdminTrustedContext.java
│   │   ├── AdminContextEvent.java
│   │   └── VendorCommission.java
│   ├── crud/                          # JPA Repositories
│   │   ├── AdminProfileCRUD.java
│   │   ├── AdminContextEventCRUD.java
│   │   └── VendorCommissionCRUD.java
│   ├── impl/                          # Repository implementations
│   │   ├── AdminProfileRepositoryImpl.java
│   │   ├── AdminContextEventRepositoryImpl.java
│   │   └── VendorCommissionRepositoryImpl.java
│   └── mapper/                        # Entity <-> DTO mappers
│       ├── AdminProfileMapper.java
│       ├── AdminContextEventMapper.java
│       └── VendorCommissionMapper.java
├── web/
│   ├── controller/                    # REST Controllers
│   │   └── VendorController.java
│   ├── dto/                           # Request/Response DTOs
│   │   ├── VendorCommissionRequest.java
│   │   ├── VendorCommissionResponse.java
│   │   └── RequestContextDTO.java
│   ├── filter/                        # Servlet Filters
│   │   └── ContextScorerFilter.java
│   └── mapper/                        # Web layer mappers
│       └── VendorCommissionWebMapper.java
└── infrastructure/
    ├── config/                        # Configuration classes
    │   └── ContextScorerProperties.java
    └── redis/                         # Redis integration
        ├── RedisProfileStore.java
        └── ProfileCacheLoader.java

src/main/resources/
├── application.yaml                   # Main configuration
├── application-dev.yaml               # Development profile
└── data.sql                           # Seed data
```

---

## 4. Spring Boot Fundamentals

### Main Application Class

```java
@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(ContextScorerProperties.class)
public class AdminApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApiApplication.class, args);
    }
}
```

#### Annotations Explained

| Annotation | Purpose |
|------------|---------|
| `@SpringBootApplication` | Combines `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`. Marks this as the main configuration class. |
| `@EnableAsync` | Enables Spring's asynchronous method execution. Required for `@Async` methods to work. |
| `@EnableConfigurationProperties` | Registers configuration properties classes as Spring beans. |

### Component Scanning

Spring Boot automatically scans for components in the package containing `@SpringBootApplication` and all sub-packages. Components are detected via stereotype annotations:

| Annotation | Layer | Purpose |
|------------|-------|---------|
| `@Component` | Any | Generic Spring-managed component |
| `@Service` | Domain | Business logic services |
| `@Repository` | Persistence | Data access components |
| `@Controller` / `@RestController` | Web | HTTP request handlers |
| `@Configuration` | Config | Configuration classes |

---

## 5. Configuration Layer

### application.yaml - Main Configuration

```yaml
spring:
  application:
    name: admin-api              # Application identifier
  profiles:
    active: dev                  # Active profile (loads application-dev.yaml)
  data:
    redis:
      host: ${REDIS_HOST:localhost}  # Environment variable with default
      port: ${REDIS_PORT:6379}
      connect-timeout: 2000ms
      timeout: 1000ms

server:
  port: 8080
  servlet:
    context-path: /admin-api     # Base path for all endpoints
  tomcat:
    threads:
      max: 50                    # Max worker threads
      min-spare: 10              # Keep 10 threads ready
    accept-count: 100            # Queue size when all threads busy
    max-connections: 10000       # Max concurrent connections
```

#### Why These Settings Matter

| Setting | Purpose |
|---------|---------|
| `context-path: /admin-api` | All endpoints prefixed with `/admin-api`, e.g., `GET /admin-api/vendor/commission` |
| `threads.max: 50` | Limits concurrent request processing to prevent resource exhaustion |
| `redis.timeout: 1000ms` | Fails fast if Redis is slow, protecting response times |

### application-dev.yaml - Development Profile

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/reto2_db
    username: appuser
    password: secret
    hikari:
      maximum-pool-size: 10      # Connection pool size
      minimum-idle: 2            # Minimum ready connections
      connection-timeout: 20000  # 20s to get connection
      idle-timeout: 600000       # 10min before closing idle
      max-lifetime: 1800000      # 30min max connection age

  jpa:
    show-sql: true               # Log SQL statements
    hibernate:
      ddl-auto: update           # Auto-create/update schema
    properties:
      hibernate:
        format_sql: true         # Pretty-print SQL
        jdbc:
          batch_size: 20         # Batch insert/update operations
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

### Custom Configuration Properties

```java
@Data
@ConfigurationProperties(prefix = "security.context-scorer")
public class ContextScorerProperties {

    private boolean enabled = true;
    private double threshold = 0.7;
    private Weights weights = new Weights();
    private Headers headers = new Headers();

    @Data
    public static class Weights {
        private double device = 0.30;
        private double ip = 0.30;
        private double hour = 0.20;
        private double userAgent = 0.20;
    }

    @Data
    public static class Headers {
        private String userId = "X-User-Id";
        private String deviceId = "X-Device-Id";
        private String forwardedFor = "X-Forwarded-For";
    }
}
```

#### How It Maps to YAML

```yaml
security:
  context-scorer:
    enabled: true
    threshold: 0.7
    weights:
      device: 0.30
      ip: 0.30
      hour: 0.20
      user-agent: 0.20          # kebab-case in YAML = camelCase in Java
    headers:
      user-id: X-User-Id
      device-id: X-Device-Id
      forwarded-for: X-Forwarded-For
```

| Annotation | Purpose |
|------------|---------|
| `@ConfigurationProperties(prefix = "...")` | Binds YAML properties starting with prefix to this class |
| `@Data` (Lombok) | Generates getters, setters, toString, equals, hashCode |
| Nested classes | Map nested YAML structures |

---

## 6. Domain Layer

The domain layer contains **business logic** and defines **contracts** (interfaces) that the persistence layer implements.

### Domain DTOs (Data Transfer Objects)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileDTO {
    private Long id;
    private String adminId;
    private AdminProfileStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AdminTrustedContextDTO> trustedContexts;
}
```

#### Lombok Annotations Explained

| Annotation | Generated Code |
|------------|----------------|
| `@Data` | `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode`, `@RequiredArgsConstructor` |
| `@Builder` | Builder pattern: `AdminProfileDTO.builder().adminId("x").build()` |
| `@NoArgsConstructor` | No-argument constructor (required by JPA/Jackson) |
| `@AllArgsConstructor` | Constructor with all fields (used by Builder) |

### Enums

```java
public enum AdminProfileStatus {
    ACTIVE,    // Profile is active, scoring applies
    LEARNING,  // New admin, collecting behavioral data
    BLOCKED    // Admin is blocked, all requests denied
}

public enum ContextType {
    DEVICE,      // Device fingerprint
    IP,          // IP address
    HOUR,        // Hour of day (0-23)
    USER_AGENT,  // Browser/client identifier
    REGION       // Geographic region
}

public enum Decision {
    ALLOWED,   // Request passed security check
    BLOCKED    // Request denied
}
```

### Repository Interfaces

```java
public interface AdminContextEventRepository {
    AdminContextEventDTO save(AdminContextEventDTO event);
    List<AdminContextEventDTO> findByAdminId(String adminId);
    List<AdminContextEventDTO> findByDecision(Decision decision);
    long countByAdminIdAndDecision(String adminId, Decision decision);
}
```

#### Why Interfaces in Domain Layer?

```
Domain Layer                  Persistence Layer
─────────────────────         ─────────────────────
┌─────────────────────┐       ┌─────────────────────────────┐
│ AdminProfileRepository │ ◄── │ AdminProfileRepositoryImpl │
│ (interface)            │       │ (implementation)            │
└─────────────────────┘       └─────────────────────────────┘
```

**Benefits:**
1. **Testability**: Mock the interface for unit tests
2. **Decoupling**: Domain doesn't know about JPA/Hibernate
3. **Flexibility**: Can swap implementations (JPA → MongoDB)

### Domain Services

```java
@Service
public class CommissionService {
    private final VendorCommissionRepository repository;

    public CommissionService(VendorCommissionRepository repository) {
        this.repository = repository;
    }

    public List<VendorCommissionDTO> findAll() {
        return repository.findAll();
    }

    public VendorCommissionDTO create(VendorCommissionDTO dto) {
        return repository.save(dto);
    }
}
```

| Annotation | Purpose |
|------------|---------|
| `@Service` | Marks as a service component; business logic container |
| Constructor Injection | Dependencies injected via constructor (preferred over `@Autowired`) |

---

## 7. Persistence Layer

### JPA Entities

Entities are Java classes mapped to database tables.

```java
@Entity
@Table(name = "admin_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false, unique = true)
    private String adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminProfileStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "adminProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AdminTrustedContext> trustedContexts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

#### JPA Annotations Explained

| Annotation | Purpose |
|------------|---------|
| `@Entity` | Marks class as JPA entity (maps to table) |
| `@Table(name = "...")` | Specifies table name (default: class name) |
| `@Id` | Marks primary key field |
| `@GeneratedValue(strategy = IDENTITY)` | Auto-increment ID (PostgreSQL SERIAL) |
| `@Column(name = "...", nullable = false)` | Column mapping and constraints |
| `@Enumerated(EnumType.STRING)` | Store enum as string (not ordinal) |
| `@OneToMany(mappedBy = "...")` | One-to-many relationship |
| `cascade = CascadeType.ALL` | Operations cascade to children |
| `orphanRemoval = true` | Delete orphaned children |
| `@PrePersist` | Callback before INSERT |
| `@PreUpdate` | Callback before UPDATE |

#### Entity Relationships

```java
// Parent Entity
@Entity
public class AdminProfile {
    @OneToMany(mappedBy = "adminProfile", cascade = CascadeType.ALL)
    private List<AdminTrustedContext> trustedContexts;
}

// Child Entity
@Entity
public class AdminTrustedContext {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_profile_id", nullable = false)
    private AdminProfile adminProfile;
}
```

```
admin_profile                    admin_trusted_contexts
┌────┬───────────┬────────┐     ┌────┬──────────────────┬──────┬───────┐
│ id │ admin_id  │ status │     │ id │ admin_profile_id │ type │ value │
├────┼───────────┼────────┤     ├────┼──────────────────┼──────┼───────┤
│ 1  │ admin-001 │ ACTIVE │◄────│ 1  │ 1                │DEVICE│ dev-1 │
│    │           │        │◄────│ 2  │ 1                │ IP   │192... │
└────┴───────────┴────────┘     └────┴──────────────────┴──────┴───────┘
```

### CRUD Repositories (JpaRepository)

```java
public interface AdminProfileCRUD extends JpaRepository<AdminProfile, Long> {

    // Derived query - Spring generates SQL from method name
    List<AdminProfile> findByStatus(AdminProfileStatus status);
    Optional<AdminProfile> findByAdminId(String adminId);

    // Custom JPQL with JOIN FETCH to avoid N+1 problem
    @Query("SELECT DISTINCT p FROM AdminProfile p " +
           "LEFT JOIN FETCH p.trustedContexts " +
           "WHERE p.status = :status")
    List<AdminProfile> findByStatusWithTrustedContexts(@Param("status") AdminProfileStatus status);
}
```

#### Why JOIN FETCH?

Without JOIN FETCH (N+1 Problem):
```sql
-- 1 query for profiles
SELECT * FROM admin_profile WHERE status = 'ACTIVE';
-- N queries for trusted contexts (one per profile!)
SELECT * FROM admin_trusted_contexts WHERE admin_profile_id = 1;
SELECT * FROM admin_trusted_contexts WHERE admin_profile_id = 2;
-- ... N more queries
```

With JOIN FETCH:
```sql
-- 1 query gets everything
SELECT DISTINCT p.*, t.*
FROM admin_profile p
LEFT JOIN admin_trusted_contexts t ON p.id = t.admin_profile_id
WHERE p.status = 'ACTIVE';
```

### Repository Implementations

```java
@Repository
public class AdminContextEventRepositoryImpl implements AdminContextEventRepository {

    private final AdminContextEventCRUD crudRepository;
    private final AdminContextEventMapper mapper;

    public AdminContextEventRepositoryImpl(
            AdminContextEventCRUD crudRepository,
            AdminContextEventMapper mapper) {
        this.crudRepository = crudRepository;
        this.mapper = mapper;
    }

    @Override
    public AdminContextEventDTO save(AdminContextEventDTO dto) {
        AdminContextEvent entity = mapper.toEntity(dto);
        AdminContextEvent saved = crudRepository.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    public List<AdminContextEventDTO> findByAdminId(String adminId) {
        return crudRepository.findByAdminIdOrderByTimestampDesc(adminId).stream()
                .map(mapper::toDTO)
                .toList();
    }
}
```

| Annotation | Purpose |
|------------|---------|
| `@Repository` | Marks as data access component; enables exception translation |

### Mappers

```java
@Component
public class AdminContextEventMapper {

    public AdminContextEventDTO toDTO(AdminContextEvent entity) {
        if (entity == null) return null;
        return AdminContextEventDTO.builder()
                .id(entity.getId())
                .adminId(entity.getAdminId())
                .device(entity.getDevice())
                .ip(entity.getIp())
                .score(entity.getScore())
                .decision(entity.getDecision())
                .timestamp(entity.getTimestamp())
                .build();
    }

    public AdminContextEvent toEntity(AdminContextEventDTO dto) {
        if (dto == null) return null;
        return AdminContextEvent.builder()
                .id(dto.getId())
                .adminId(dto.getAdminId())
                .device(dto.getDevice())
                .ip(dto.getIp())
                .score(dto.getScore())
                .decision(dto.getDecision())
                .timestamp(dto.getTimestamp())
                .build();
    }
}
```

#### Why Mappers?

```
Entity (Persistence)          DTO (Domain/Web)
─────────────────────         ─────────────────────
- JPA annotations             - No JPA dependencies
- Database column names       - Clean field names
- All fields                  - Only needed fields
- Relationships               - Flattened structure
```

---

## 8. Web Layer

### REST Controllers

```java
@Slf4j
@RestController
@RequestMapping("/vendor")
public class VendorController {

    private final CommissionService commissionService;
    private final VendorCommissionWebMapper mapper;

    public VendorController(CommissionService commissionService,
                           VendorCommissionWebMapper mapper) {
        this.commissionService = commissionService;
        this.mapper = mapper;
    }

    @GetMapping("ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.status(HttpStatus.OK).body("pong");
    }

    @GetMapping("commission")
    public ResponseEntity<List<VendorCommissionResponse>> getAllCommissions() {
        List<VendorCommissionResponse> response = commissionService.findAll().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("commission/{id}")
    public ResponseEntity<VendorCommissionResponse> getCommission(@PathVariable Long id) {
        VendorCommissionDTO commission = commissionService.findById(id);
        return ResponseEntity.ok(mapper.toResponse(commission));
    }

    @PostMapping("commission")
    public ResponseEntity<VendorCommissionResponse> createCommission(
            @Valid @RequestBody VendorCommissionRequest request) {
        VendorCommissionDTO dto = mapper.fromRequest(request);
        VendorCommissionDTO created = commissionService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(created));
    }

    @PutMapping("commission/{id}")
    public ResponseEntity<VendorCommissionResponse> updateCommission(
            @PathVariable Long id,
            @Valid @RequestBody VendorCommissionRequest request) {
        VendorCommissionDTO dto = mapper.fromRequest(request);
        VendorCommissionDTO updated = commissionService.update(id, dto);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("commission/{id}")
    public ResponseEntity<Void> deleteCommission(@PathVariable Long id) {
        commissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

#### Controller Annotations

| Annotation | Purpose |
|------------|---------|
| `@RestController` | `@Controller` + `@ResponseBody` - returns JSON directly |
| `@RequestMapping("/vendor")` | Base path for all methods in this controller |
| `@GetMapping`, `@PostMapping`, etc. | HTTP method mapping |
| `@PathVariable` | Extracts value from URL path |
| `@RequestBody` | Deserializes JSON body to object |
| `@Valid` | Triggers validation of request body |
| `@Slf4j` (Lombok) | Generates `private static final Logger log = ...` |

### Request Validation

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorCommissionRequest {

    @NotBlank(message = "Vendor ID is required")
    private String vendorId;

    @NotNull(message = "Commission rate is required")
    @DecimalMin(value = "0.00", message = "Commission rate must be at least 0.00")
    @DecimalMax(value = "100.00", message = "Commission rate must be at most 100.00")
    private BigDecimal commissionRate;

    @NotNull(message = "Effective from date is required")
    private LocalDateTime effectiveFrom;

    private Boolean active = true;
}
```

#### Validation Annotations (Jakarta Bean Validation)

| Annotation | Validates |
|------------|-----------|
| `@NotNull` | Field must not be null |
| `@NotBlank` | String must not be null, empty, or whitespace |
| `@NotEmpty` | Collection/String must not be null or empty |
| `@DecimalMin` / `@DecimalMax` | Numeric range |
| `@Min` / `@Max` | Integer range |
| `@Size(min, max)` | String/Collection length |
| `@Email` | Valid email format |
| `@Pattern(regex)` | Matches regex |

---

## 9. Infrastructure Layer

### Redis Profile Store

```java
@Slf4j
@Service
public class RedisProfileStore {

    private static final String PROFILE_PREFIX = "profile:";
    private static final String STATUS_SUFFIX = ":status";
    private static final String DEVICES_SUFFIX = ":devices";
    private static final String IPS_SUFFIX = ":ips";
    private static final String HOURS_SUFFIX = ":hours";
    private static final String AGENTS_SUFFIX = ":agents";

    private final StringRedisTemplate redisTemplate;

    public RedisProfileStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveProfile(AdminProfileDTO profile) {
        String adminId = profile.getAdminId();
        String keyPrefix = PROFILE_PREFIX + adminId;

        // Save status as string
        redisTemplate.opsForValue().set(
            keyPrefix + STATUS_SUFFIX,
            profile.getStatus().name()
        );

        // Clear and repopulate sets
        clearProfileSets(adminId);

        if (profile.getTrustedContexts() != null) {
            for (AdminTrustedContextDTO context : profile.getTrustedContexts()) {
                String setKey = getSetKeyForContextType(adminId, context.getType());
                if (setKey != null) {
                    redisTemplate.opsForSet().add(setKey, context.getValue());
                }
            }
        }
    }

    public Optional<AdminProfileStatus> getStatus(String adminId) {
        String key = PROFILE_PREFIX + adminId + STATUS_SUFFIX;
        String status = redisTemplate.opsForValue().get(key);
        return status == null ? Optional.empty()
                              : Optional.of(AdminProfileStatus.valueOf(status));
    }

    public Set<String> getDevices(String adminId) {
        return redisTemplate.opsForSet().members(
            PROFILE_PREFIX + adminId + DEVICES_SUFFIX
        );
    }

    public boolean profileExists(String adminId) {
        String key = PROFILE_PREFIX + adminId + STATUS_SUFFIX;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
```

#### Redis Data Structure

```
Redis Keys for admin-001:
─────────────────────────────────────────────────────────
profile:admin-001:status    → "ACTIVE"           (String)
profile:admin-001:devices   → {dev-1, dev-2}     (Set)
profile:admin-001:ips       → {192.168.1.0/24}   (Set)
profile:admin-001:hours     → {7, 8, 9, ..., 18} (Set)
profile:admin-001:agents    → {Mozilla/5.0...}   (Set)
```

#### Why Redis Sets?

```java
// O(1) membership check - perfect for "is this device trusted?"
redisTemplate.opsForSet().isMember("profile:admin-001:devices", "device-123");
```

### Profile Cache Loader

```java
@Slf4j
@Component
public class ProfileCacheLoader {

    private final AdminProfileRepository adminProfileRepository;
    private final RedisProfileStore redisProfileStore;

    @EventListener(ApplicationReadyEvent.class)
    public void loadProfilesToRedis() {
        log.info("Starting to load admin profiles into Redis cache...");

        try {
            // Load ACTIVE profiles
            List<AdminProfileDTO> activeProfiles =
                adminProfileRepository.findByStatus(AdminProfileStatus.ACTIVE);
            int activeCount = loadProfiles(activeProfiles);

            // Load LEARNING profiles
            List<AdminProfileDTO> learningProfiles =
                adminProfileRepository.findByStatus(AdminProfileStatus.LEARNING);
            int learningCount = loadProfiles(learningProfiles);

            // Load BLOCKED profiles
            List<AdminProfileDTO> blockedProfiles =
                adminProfileRepository.findByStatus(AdminProfileStatus.BLOCKED);
            int blockedCount = loadProfiles(blockedProfiles);

            log.info("Finished loading {} total profiles into Redis",
                    activeCount + learningCount + blockedCount);

        } catch (Exception e) {
            log.error("Failed to load profiles into Redis cache", e);
        }
    }

    private int loadProfiles(List<AdminProfileDTO> profiles) {
        int count = 0;
        for (AdminProfileDTO profile : profiles) {
            try {
                redisProfileStore.saveProfile(profile);
                count++;
            } catch (Exception e) {
                log.error("Failed to load profile: {}", profile.getAdminId(), e);
            }
        }
        return count;
    }
}
```

| Annotation | Purpose |
|------------|---------|
| `@EventListener(ApplicationReadyEvent.class)` | Executes after Spring context is fully initialized |

#### Why Load on Startup?

```
Per Spec: "PostgreSQL is the source of truth. Redis is the in-memory projection."

Application Start
       │
       ▼
┌──────────────────┐
│ ApplicationReady │
│     Event        │
└────────┬─────────┘
         │
         ▼
┌─────────────────────────────┐
│ ProfileCacheLoader          │
│ - Query PostgreSQL          │
│ - Load all profiles to Redis│
└─────────────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ Redis Ready                 │
│ - Fast lookups available    │
│ - No cold start             │
└─────────────────────────────┘
```

---

## 10. Security: Context Scorer Filter

### Filter Implementation

```java
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ContextScorerFilter extends OncePerRequestFilter {

    private final ContextScoringService scoringService;
    private final ContextScorerProperties properties;

    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/actuator", "/health", "/metrics", "/vendor/ping"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Skip if disabled
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI()
            .substring(request.getContextPath().length());

        // Skip excluded paths
        if (shouldSkipPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract admin ID from header
        String adminId = request.getHeader(properties.getHeaders().getUserId());

        // Block if missing X-User-Id
        if (adminId == null || adminId.isBlank()) {
            log.warn("UNAUTHORIZED - Missing X-User-Id for path: {}", path);
            sendUnauthorizedResponse(response, "Missing required X-User-Id header");
            return;
        }

        // Build context and evaluate
        RequestContextDTO context = buildRequestContext(request, adminId);
        long startTime = System.currentTimeMillis();

        ScoringResult result = scoringService.evaluateContext(context);

        // Persist event asynchronously
        scoringService.persistEventAsync(context, result);

        // Check decision
        if (result.getDecision() == Decision.BLOCKED) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.warn("BLOCKED - Admin: {}, Score: {}, Time: {}ms",
                    adminId, result.getScore(), responseTime);
            sendForbiddenResponse(response, result);
            return;
        }

        // Allow through
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}
```

#### Filter Annotations

| Annotation | Purpose |
|------------|---------|
| `@Component` | Registers filter with Spring |
| `@Order(1)` | Filter execution order (lower = earlier) |
| `@RequiredArgsConstructor` | Lombok: generates constructor for final fields |

#### OncePerRequestFilter

Ensures the filter executes exactly once per request (important for forwarded requests).

### Scoring Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextScoringService {

    private final RedisProfileStore redisProfileStore;
    private final ContextScorerProperties properties;
    private final AdminContextEventRepository contextEventRepository;

    public ScoringResult evaluateContext(RequestContextDTO context) {
        String adminId = context.getAdminId();

        // Check profile exists
        if (!redisProfileStore.profileExists(adminId)) {
            return ScoringResult.builder()
                    .adminId(adminId)
                    .score(1.0)
                    .decision(Decision.BLOCKED)
                    .reason("Profile not found")
                    .build();
        }

        // Check status
        Optional<AdminProfileStatus> statusOpt = redisProfileStore.getStatus(adminId);
        AdminProfileStatus status = statusOpt.orElse(null);

        if (status == AdminProfileStatus.BLOCKED) {
            return ScoringResult.builder()
                    .adminId(adminId)
                    .score(1.0)
                    .decision(Decision.BLOCKED)
                    .reason("Admin profile is blocked")
                    .build();
        }

        // Calculate score
        double score = calculateScore(context);
        double threshold = properties.getThreshold();

        // LEARNING mode: allow but log
        if (status == AdminProfileStatus.LEARNING) {
            return ScoringResult.builder()
                    .adminId(adminId)
                    .score(score)
                    .decision(Decision.ALLOWED)
                    .reason("Learning mode - data collection")
                    .build();
        }

        // ACTIVE mode: apply threshold
        Decision decision = score >= threshold ? Decision.BLOCKED : Decision.ALLOWED;
        return ScoringResult.builder()
                .adminId(adminId)
                .score(score)
                .decision(decision)
                .reason(decision == Decision.BLOCKED
                    ? "Anomaly score exceeds threshold"
                    : "Context within normal parameters")
                .build();
    }

    private double calculateScore(RequestContextDTO context) {
        ContextScorerProperties.Weights weights = properties.getWeights();
        double score = 0.0;

        if (isDeviceAnomalous(context)) score += weights.getDevice();
        if (isIpAnomalous(context)) score += weights.getIp();
        if (isHourAnomalous(context)) score += weights.getHour();
        if (isUserAgentAnomalous(context)) score += weights.getUserAgent();

        return Math.min(score, 1.0);
    }

    @Async
    public void persistEventAsync(RequestContextDTO context, ScoringResult result) {
        try {
            AdminContextEventDTO eventDTO = AdminContextEventDTO.builder()
                    .adminId(context.getAdminId())
                    .device(context.getDevice())
                    .ip(context.getIp())
                    .hour(context.getHour())
                    .agent(context.getUserAgent())
                    .score(result.getScore())
                    .decision(result.getDecision())
                    .timestamp(LocalDateTime.now())
                    .build();

            contextEventRepository.save(eventDTO);
        } catch (Exception e) {
            log.error("Failed to persist event: {}", e.getMessage());
        }
    }
}
```

#### @Async Annotation

```java
@Async
public void persistEventAsync(...) { ... }
```

| Requirement | Setup |
|-------------|-------|
| Enable async | `@EnableAsync` on main class |
| Method modifier | Must be `public` |
| Return type | `void` or `Future<T>` |
| Proxy behavior | Called via Spring proxy (not from same class) |

**Why Async?** Database writes don't block the HTTP response:

```
Without @Async:                  With @Async:
─────────────────                ─────────────────
Request                          Request
   │                                │
   ▼                                ▼
Score (10ms)                     Score (10ms)
   │                                │
   ▼                                ├──► DB Write (async)
DB Write (50ms)                     │
   │                                ▼
   ▼                             Response (10ms)
Response (60ms)                     │
                                    ▼
                                 DB Write completes (50ms)
```

---

## 11. Redis Integration

### Spring Data Redis Configuration

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      connect-timeout: 2000ms
      timeout: 1000ms
```

### StringRedisTemplate

Spring Boot auto-configures `StringRedisTemplate` when `spring-boot-starter-data-redis` is on classpath.

```java
@Service
public class RedisProfileStore {

    private final StringRedisTemplate redisTemplate;

    // String operations
    redisTemplate.opsForValue().set("key", "value");
    redisTemplate.opsForValue().get("key");

    // Set operations
    redisTemplate.opsForSet().add("set-key", "member1", "member2");
    redisTemplate.opsForSet().members("set-key");
    redisTemplate.opsForSet().isMember("set-key", "member1");

    // Key operations
    redisTemplate.hasKey("key");
    redisTemplate.delete("key");
}
```

### Redis Data Model

```
┌─────────────────────────────────────────────────────────────────┐
│                         REDIS                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  profile:admin-001:status  ──────►  "ACTIVE"                    │
│                                                                  │
│  profile:admin-001:devices ──────►  Set {                       │
│                                       "device-corporate-001",    │
│                                       "device-corporate-002"     │
│                                     }                            │
│                                                                  │
│  profile:admin-001:ips ──────────►  Set {                       │
│                                       "192.168.1.0/24",          │
│                                       "10.0.0.0/8"               │
│                                     }                            │
│                                                                  │
│  profile:admin-001:hours ────────►  Set {                       │
│                                       "7", "8", "9", ..., "18"   │
│                                     }                            │
│                                                                  │
│  profile:admin-001:agents ───────►  Set {                       │
│                                       "Mozilla/5.0..."          │
│                                     }                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 12. Data Flow Examples

### Example 1: GET Commission (Allowed)

```
1. HTTP Request
   GET /admin-api/vendor/commission/5
   Headers: X-User-Id: admin-001
            X-Device-Id: device-corporate-001
            X-Forwarded-For: 192.168.1.100
            User-Agent: Mozilla/5.0...

2. ContextScorerFilter
   ├── Extract headers
   ├── Build RequestContextDTO
   ├── Call scoringService.evaluateContext()
   │   ├── Check Redis: profile:admin-001:status → "ACTIVE"
   │   ├── Check device in profile:admin-001:devices → MATCH
   │   ├── Check IP in profile:admin-001:ips → MATCH
   │   ├── Check hour in profile:admin-001:hours → MATCH
   │   ├── Check agent in profile:admin-001:agents → MATCH
   │   └── Score: 0.0 (all match)
   ├── Decision: ALLOWED (0.0 < 0.7)
   ├── Async: persist event to DB
   └── filterChain.doFilter() → continue

3. VendorController.getCommission(5)
   └── commissionService.findById(5)
       └── repository.findById(5)
           └── crudRepository.findById(5)
               └── SELECT * FROM vendor_commission WHERE id = 5

4. HTTP Response
   200 OK
   {"id": 5, "vendorId": "vendor-004", ...}
```

### Example 2: PUT Commission (Blocked - Anomalous)

```
1. HTTP Request
   PUT /admin-api/vendor/commission/5
   Headers: X-User-Id: admin-001
            X-Device-Id: unknown-device    ← NOT in trusted devices
            X-Forwarded-For: 45.33.32.156  ← NOT in trusted IPs
            User-Agent: python-requests    ← NOT in trusted agents
   Time: 3:00 AM                           ← NOT in trusted hours

2. ContextScorerFilter
   ├── Extract headers
   ├── Build RequestContextDTO
   ├── Call scoringService.evaluateContext()
   │   ├── Check Redis: profile:admin-001:status → "ACTIVE"
   │   ├── Check device → NO MATCH (+0.30)
   │   ├── Check IP → NO MATCH (+0.30)
   │   ├── Check hour → NO MATCH (+0.20)
   │   └── Check agent → NO MATCH (+0.20)
   │   └── Score: 1.0 (no matches)
   ├── Decision: BLOCKED (1.0 >= 0.7)
   ├── Async: persist event to DB
   └── sendForbiddenResponse()

3. HTTP Response
   403 Forbidden
   {
     "status": 403,
     "error": "Forbidden",
     "message": "Access denied due to anomalous context",
     "score": 1.0,
     "reason": "Anomaly score 1.00 exceeds threshold 0.70"
   }

4. Controller NEVER reached
```

---

## 13. Testing the API

### Postman Collection

#### Test 1: Health Check (No Auth Required)
```
GET http://localhost:8080/admin-api/vendor/ping

Expected: 200 OK, "pong"
```

#### Test 2: Missing X-User-Id (Unauthorized)
```
GET http://localhost:8080/admin-api/vendor/commission/5

Expected: 401 Unauthorized
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing required X-User-Id header"
}
```

#### Test 3: Valid Request (Allowed)
```
GET http://localhost:8080/admin-api/vendor/commission/5
Headers:
  X-User-Id: admin-001
  X-Device-Id: device-corporate-macbook-001
  X-Forwarded-For: 192.168.1.100
  User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)

Expected: 200 OK
{
  "id": 5,
  "vendorId": "vendor-004",
  "commissionRate": 6.00,
  ...
}
```

#### Test 4: Anomalous Request (Blocked)
```
PUT http://localhost:8080/admin-api/vendor/commission/5
Headers:
  X-User-Id: admin-001
  X-Device-Id: unknown-device-xyz
  X-Forwarded-For: 45.33.32.156
  User-Agent: python-requests/2.28.0

Expected: 403 Forbidden
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied due to anomalous context",
  "score": 1.00
}
```

#### Test 5: Create Commission
```
POST http://localhost:8080/admin-api/vendor/commission
Headers:
  X-User-Id: admin-001
  X-Device-Id: device-corporate-macbook-001
  Content-Type: application/json
Body:
{
  "vendorId": "vendor-new",
  "commissionRate": 5.5,
  "effectiveFrom": "2026-04-01T00:00:00",
  "createdBy": "admin-001"
}

Expected: 201 Created
```

---

---

## 14. Prometheus Metrics Implementation

### Why Metrics Matter

Metrics are essential for understanding system behavior in production. Without metrics, you're flying blind when questions arise like:

- "Why is the API slow sometimes?"
- "How many requests are being blocked by the security filter?"
- "What's the p95 latency for different profile statuses?"

### Observability Stack

```
┌─────────────────────────────────────────────────────────────────┐
│                     OBSERVABILITY STACK                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐     │
│  │  Admin API   │────►│  Prometheus  │────►│   Grafana    │     │
│  │  (Metrics)   │     │  (Storage)   │     │ (Dashboards) │     │
│  └──────────────┘     └──────────────┘     └──────────────┘     │
│        │                     │                    │              │
│        │                     │                    │              │
│        ▼                     ▼                    ▼              │
│  Exposes metrics       Scrapes every 15s    Visualizes data     │
│  at /actuator/         Stores time series   Queries Prometheus  │
│  prometheus            Runs PromQL          Creates dashboards  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Spring Boot Actuator + Micrometer

Spring Boot uses **Micrometer** as its metrics facade, similar to SLF4J for logging. Micrometer provides a vendor-neutral API that works with various monitoring systems.

```yaml
# application.yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus  # Expose metrics endpoint
  metrics:
    export:
      prometheus:
        enabled: true                    # Enable Prometheus format
```

#### Maven Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Metric Types

Micrometer provides several metric types, each suited for different use cases:

| Type | Purpose | Example |
|------|---------|---------|
| **Counter** | Counts events (only increases) | Total requests, errors |
| **Timer** | Measures duration + count | Response time, processing time |
| **Gauge** | Current value (can increase/decrease) | Active connections, queue size |
| **DistributionSummary** | Distribution of values | Request sizes, scores |

### ContextScorerMetrics Component

This component centralizes all metrics for the Context Scorer Filter:

```java
@Slf4j
@Component
public class ContextScorerMetrics {

    private static final String METRIC_PREFIX = "context_scorer";
    private static final String TAG_STATUS = "status";
    private static final String TAG_DECISION = "decision";
    private static final String TAG_SCORE_LEVEL = "score_level";
    private static final String TAG_REASON = "reason";

    private final MeterRegistry registry;

    // Cached counters for performance
    private final Map<String, Counter> requestCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> durationTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> activeDurationTimers = new ConcurrentHashMap<>();

    // Score distribution for ACTIVE profiles
    private final DistributionSummary activeScoreDistribution;

    // Special case counters
    private final Counter profileNotFoundCounter;
    private final Counter missingHeaderCounter;

    public ContextScorerMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Score distribution for ACTIVE profiles
        this.activeScoreDistribution = DistributionSummary.builder(METRIC_PREFIX + "_active_score")
                .description("Distribution of anomaly scores for ACTIVE profile evaluations")
                .baseUnit("score")
                .publishPercentiles(0.5, 0.75, 0.90, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        // Special case counters
        this.profileNotFoundCounter = Counter.builder(METRIC_PREFIX + "_errors_total")
                .description("Total errors during context scoring")
                .tag(TAG_REASON, "profile_not_found")
                .register(registry);

        this.missingHeaderCounter = Counter.builder(METRIC_PREFIX + "_errors_total")
                .description("Total errors during context scoring")
                .tag(TAG_REASON, "missing_header")
                .register(registry);

        log.info("ContextScorerMetrics initialized with Prometheus registry");
    }
}
```

#### Annotations and Design Decisions

| Element | Purpose |
|---------|---------|
| `MeterRegistry` | Micrometer's central registry - Spring auto-configures it |
| `ConcurrentHashMap` | Thread-safe cache for lazily-created metrics |
| `DistributionSummary` | Records score distribution with percentiles |
| `publishPercentiles(...)` | Pre-calculated percentiles (p50, p75, p90, p95, p99) |
| `publishPercentileHistogram()` | Enables histogram_quantile() in Prometheus |

### Recording Evaluations

The main method records all metrics for a scoring evaluation:

```java
/**
 * Records a complete scoring evaluation with all relevant metrics.
 *
 * @param status      Profile status (LEARNING, ACTIVE, BLOCKED, or null if not found)
 * @param decision    The decision (ALLOWED, BLOCKED)
 * @param score       The calculated anomaly score
 * @param durationMs  Evaluation duration in milliseconds
 * @param threshold   The configured threshold (for determining high/low score)
 */
public void recordEvaluation(AdminProfileStatus status, Decision decision,
                              double score, long durationMs, double threshold) {
    String statusTag = status != null ? status.name() : "UNKNOWN";
    String decisionTag = decision.name();

    // Record request count
    getOrCreateRequestCounter(statusTag, decisionTag).increment();

    // Record duration by status
    getOrCreateDurationTimer(statusTag).record(Duration.ofMillis(durationMs));

    // For ACTIVE profiles, record detailed metrics
    if (status == AdminProfileStatus.ACTIVE) {
        recordActiveEvaluation(score, durationMs, threshold, decision);
    }
}
```

### Score Level Categorization

For ACTIVE profiles, we categorize scores into levels for deeper analysis:

```java
/**
 * Determines the score level category for ACTIVE profile evaluations.
 *
 * Categories:
 * - low: score < 50% of threshold (clearly safe)
 * - medium: score between 50% and 100% of threshold (approaching risk)
 * - high: score >= threshold (blocked)
 */
private String determineScoreLevel(double score, double threshold) {
    double halfThreshold = threshold / 2.0;

    if (score >= threshold) {
        return "high";       // Blocked - score exceeded threshold
    } else if (score >= halfThreshold) {
        return "medium";     // Approaching threshold
    } else {
        return "low";        // Clearly safe
    }
}
```

#### Score Level Visualization

```
Score Range (threshold = 0.70):
─────────────────────────────────────────────────────────────
0.0                0.35               0.70               1.0
│                   │                  │                  │
│◄─── LOW (safe) ──►│◄─── MEDIUM ────►│◄─── HIGH ───────►│
│    score < 0.35   │  0.35 ≤ s < 0.70 │   score ≥ 0.70   │
│     ALLOWED       │    ALLOWED       │     BLOCKED      │
─────────────────────────────────────────────────────────────
```

### Lazy Metric Creation

Metrics are created lazily to avoid registering unused combinations:

```java
private Counter getOrCreateRequestCounter(String status, String decision) {
    String key = status + "_" + decision;
    return requestCounters.computeIfAbsent(key, k ->
            Counter.builder(METRIC_PREFIX + "_requests_total")
                    .description("Total requests processed by context scorer")
                    .tag(TAG_STATUS, status)
                    .tag(TAG_DECISION, decision)
                    .register(registry)
    );
}

private Timer getOrCreateDurationTimer(String status) {
    return durationTimers.computeIfAbsent(status, k ->
            Timer.builder(METRIC_PREFIX + "_duration_seconds")
                    .description("Time taken to evaluate request context")
                    .tag(TAG_STATUS, status)
                    .publishPercentiles(0.5, 0.75, 0.90, 0.95, 0.99)
                    .publishPercentileHistogram()
                    .register(registry)
    );
}
```

#### Why Lazy Creation?

| Approach | Pros | Cons |
|----------|------|------|
| **Eager (at startup)** | All metrics available immediately | Creates unused metrics, wastes memory |
| **Lazy (on first use)** | Only creates needed metrics | First call slightly slower |

We use **lazy creation** because not all tag combinations may occur in practice.

### Convenience Methods

The `ContextScorerMetrics` class provides convenience methods for common scenarios:

```java
/**
 * Records a BLOCKED status evaluation (immediate denial).
 */
public void recordBlockedStatus(long durationMs) {
    recordEvaluation(AdminProfileStatus.BLOCKED, Decision.BLOCKED, 1.0, durationMs, 0.7);
}

/**
 * Records a LEARNING status evaluation (always allowed).
 */
public void recordLearningEvaluation(double score, long durationMs) {
    recordEvaluation(AdminProfileStatus.LEARNING, Decision.ALLOWED, score, durationMs, 0.7);
}
```

These methods simplify recording metrics for the most common scenarios where the decision is predetermined by the profile status.

### Integration with ContextScorerFilter

The filter records metrics after each evaluation:

```java
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ContextScorerFilter extends OncePerRequestFilter {

    private final ContextScoringService scoringService;
    private final ContextScorerProperties properties;
    private final ContextScorerMetrics metrics;  // Injected metrics component

    @Override
    protected void doFilterInternal(...) {
        // ... extract context and build request ...

        // Record start time
        long startTime = System.currentTimeMillis();

        // Evaluate context
        ScoringResult result = scoringService.evaluateContext(context);

        // Calculate duration
        long durationMs = System.currentTimeMillis() - startTime;

        // Record metrics for this evaluation
        metrics.recordEvaluation(
                result.getProfileStatus(),
                result.getDecision(),
                result.getScore(),
                durationMs,
                properties.getThreshold()
        );

        // ... handle decision ...
    }
}
```

### ScoringResult Modification

The `ScoringResult` was modified to include `profileStatus` for metrics tracking:

```java
@Data
@Builder
public static class ScoringResult {
    private String adminId;
    private double score;
    private Decision decision;
    private String reason;

    // Added for metrics tracking
    private AdminProfileStatus profileStatus;

    // Individual feature match results
    @Builder.Default
    private boolean deviceMatch = false;
    @Builder.Default
    private boolean ipMatch = false;
    @Builder.Default
    private boolean hourMatch = false;
    @Builder.Default
    private boolean userAgentMatch = false;
}
```

### Prometheus Configuration

Configure Prometheus to scrape the admin-api metrics:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'admin-api'
    metrics_path: '/admin-api/actuator/prometheus'
    static_configs:
      - targets: ["host.docker.internal:8080"]  # Docker
      # - targets: ['localhost:8080']           # Local
        labels:
          app: "admin-api"
          service: "context-scorer"
```

#### Configuration Explained

| Setting | Purpose |
|---------|---------|
| `metrics_path` | Path where metrics are exposed (context-path + actuator path) |
| `host.docker.internal` | Special DNS for Docker to reach host machine |
| `labels` | Additional labels added to all scraped metrics |

### Metrics Exposed

The implementation exposes these metrics:

```
┌─────────────────────────────────────────────────────────────────┐
│                    CONTEXT SCORER METRICS                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  context_scorer_requests_total                                   │
│  ├── status: ACTIVE, LEARNING, BLOCKED, UNKNOWN                 │
│  └── decision: ALLOWED, BLOCKED                                  │
│                                                                  │
│  context_scorer_duration_seconds                                 │
│  └── status: ACTIVE, LEARNING, BLOCKED                          │
│      (histogram with p50, p75, p90, p95, p99)                   │
│                                                                  │
│  context_scorer_active_duration_seconds                          │
│  ├── score_level: low, medium, high                             │
│  └── decision: ALLOWED, BLOCKED                                  │
│      (histogram with p50, p75, p90, p95, p99)                   │
│                                                                  │
│  context_scorer_active_score                                     │
│  └── (distribution summary with percentiles)                     │
│                                                                  │
│  context_scorer_errors_total                                     │
│  └── reason: profile_not_found, missing_header                   │
│                                                                  │
│  context_scorer_skipped_total                                    │
│  └── reason: disabled, excluded_path                             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Prometheus Queries (PromQL)

#### Request Counts

```promql
# Total requests by status and decision
context_scorer_requests_total

# Requests by status (summed across decisions)
sum by (status) (context_scorer_requests_total)

# Only blocked requests
context_scorer_requests_total{decision="BLOCKED"}
```

#### Latency Analysis

```promql
# p95 latency by status (IMPORTANT: Use TABLE view!)
histogram_quantile(0.95,
  sum by (status, le) (context_scorer_duration_seconds_bucket))

# p95 latency in milliseconds
histogram_quantile(0.95,
  sum by (status, le) (context_scorer_duration_seconds_bucket)) * 1000

# p95 latency for ACTIVE profile by score level
histogram_quantile(0.95,
  sum by (score_level, le) (context_scorer_active_duration_seconds_bucket))
```

#### Score Distribution

```promql
# Average score for ACTIVE profiles
context_scorer_active_score_sum / context_scorer_active_score_count

# p95 score for ACTIVE profiles
context_scorer_active_score{quantile="0.95"}
```

### Expected Latency Order

Based on the implementation, expected latency order is:

```
BLOCKED < LEARNING < ACTIVE

Why:
┌──────────────────────────────────────────────────────────────────┐
│  BLOCKED (~3ms)                                                   │
│  └── Check Redis status → Return immediately (no scoring)        │
│                                                                   │
│  LEARNING (~8ms)                                                  │
│  └── Check Redis status → Calculate score → Return (no threshold)│
│                                                                   │
│  ACTIVE (~11ms)                                                   │
│  └── Check Redis status → Calculate score → Compare threshold    │
│      → Check all context features (device, IP, hour, agent)      │
└──────────────────────────────────────────────────────────────────┘
```

### Important Lessons Learned

#### 1. Minimum Sample Size

```
⚠️ CRITICAL: You need 50+ samples per status for accurate percentiles!

With only 2-5 samples:
- JVM warmup noise dominates
- p95 values are unreliable
- Results can be misleading

With 50+ samples:
- Statistical significance
- JVM warmed up
- Accurate percentiles
```

#### 2. Prometheus Table View

```
⚠️ Always use TABLE view in Prometheus for p95 analysis!

GRAPH view:
- Shows stacked areas
- Can be visually misleading
- Hard to compare exact values

TABLE view:
- Shows exact numeric values
- Easy to compare
- Accurate for latency analysis
```

#### 3. Prometheus Restart After Config Change

```bash
# After modifying prometheus.yml, restart Prometheus:
docker compose restart prometheus

# Verify scraping (wait 15-30 seconds):
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job, health}'
```

### Testing the Metrics

#### Generate Test Data

```bash
# Quick test - ACTIVE profile (low score)
curl http://localhost:8080/admin-api/vendor/commission/1 \
  -H "X-User-Id: admin-001" \
  -H "X-Device-Id: device-corporate-macbook-001" \
  -H "X-Forwarded-For: 192.168.1.100" \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"

# ACTIVE profile (high score - will be blocked)
curl http://localhost:8080/admin-api/vendor/commission/1 \
  -H "X-User-Id: admin-001" \
  -H "X-Device-Id: unknown-device" \
  -H "X-Forwarded-For: 45.33.32.156" \
  -H "User-Agent: python-requests/2.28.0"

# LEARNING profile
curl http://localhost:8080/admin-api/vendor/commission/1 \
  -H "X-User-Id: admin-003" \
  -H "X-Device-Id: any-device" \
  -H "X-Forwarded-For: 1.2.3.4"

# BLOCKED profile
curl http://localhost:8080/admin-api/vendor/commission/1 \
  -H "X-User-Id: admin-004" \
  -H "X-Device-Id: any-device"
```

#### Verify Metrics Endpoint

```bash
# Check metrics are exposed
curl http://localhost:8080/admin-api/actuator/prometheus | grep context_scorer
```

### Project Structure Update

```
src/main/java/com/uniandes/admin_api/
└── infrastructure/
    ├── config/
    │   └── ContextScorerProperties.java
    ├── redis/
    │   ├── RedisProfileStore.java
    │   └── ProfileCacheLoader.java
    └── metrics/                          # NEW
        └── ContextScorerMetrics.java     # Prometheus metrics component
```

### Load Testing Scripts

For generating sufficient test samples, use the provided scripts located in `scripts/`:

```
scripts/
├── load_test_configurable.sh   # Flexible load testing with multiple modes
├── load_test_equal.sh          # Equal distribution across all statuses
└── load_test_realistic.sh      # Realistic traffic patterns
```

#### Configurable Load Test

The main script supports multiple modes and options:

```bash
# Quick validation (100 requests per status)
./scripts/load_test_configurable.sh quick

# Standard testing (500 requests per status)
./scripts/load_test_configurable.sh standard

# Production experiment (1000 requests per status)
./scripts/load_test_configurable.sh full

# Custom count per scenario
./scripts/load_test_configurable.sh custom 250

# Parallel execution (4 concurrent requests)
./scripts/load_test_configurable.sh full --parallel

# Export results to file
./scripts/load_test_configurable.sh full --export

# Custom URL target
./scripts/load_test_configurable.sh full --url http://localhost:8080/admin-api
```

#### Equal Distribution

For A/B testing with equal samples across all statuses:

```bash
./scripts/load_test_equal.sh http://localhost:8080/admin-api 1000
```

#### Realistic Traffic

Simulates production-like traffic distribution:

```bash
./scripts/load_test_realistic.sh
```

#### Test Scenarios

The load tests cover these scenarios using admin profiles from `data.sql`:

| Scenario | Admin ID | Expected Behavior |
|----------|----------|-------------------|
| ACTIVE - Low Score | admin-001 | All context matches, ALLOWED |
| ACTIVE - Medium Score | admin-001 | Device mismatch only, ALLOWED |
| ACTIVE - High Score | admin-001 | All context mismatches, BLOCKED |
| LEARNING | admin-003 | Always ALLOWED (data collection) |
| BLOCKED | admin-004 | Always BLOCKED (lockdown) |

---

## Summary

This guide covered:

1. **Architecture**: Layered design separating concerns
2. **Spring Annotations**: Purpose of each annotation and when to use them
3. **Configuration**: YAML properties and custom configuration classes
4. **JPA/Hibernate**: Entities, relationships, and the N+1 problem
5. **Repository Pattern**: Interfaces in domain, implementations in persistence
6. **REST Controllers**: HTTP mapping, validation, response handling
7. **Redis Integration**: Caching profiles for fast security checks
8. **Security Filter**: Defense in Depth with anomaly scoring
9. **Async Processing**: Non-blocking event persistence
10. **Prometheus Metrics**: Performance monitoring with Micrometer, PromQL queries, and observability best practices

The key principle throughout is **separation of concerns** - each layer has a specific responsibility and communicates with other layers through well-defined interfaces.
