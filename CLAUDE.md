# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Gyeonggi Partners** is a civic participation governance platform where citizens and government officials in Gyeonggi Province can discuss regional issues and propose solutions together. Built with Spring Boot 3.5.6, Java 21, PostgreSQL, and Redis.

## Build & Development Commands

### Build & Run
```bash
# Build the project
./gradlew build

# Run the application (local profile)
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# Clean and rebuild
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "org.example.gyeonggipartners.domain.user.application.UserServiceTest"

# Run tests with specific pattern
./gradlew test --tests "*ServiceTest"

# Run tests with logging
./gradlew test --info
```

### Docker
```bash
# Start PostgreSQL and Redis (development)
docker-compose -f docker-compose.dev.yml up -d

# Stop services
docker-compose -f docker-compose.dev.yml down

# View logs
docker-compose -f docker-compose.dev.yml logs -f
```

### Database Migration
```bash
# Flyway runs automatically on application startup
# Migration files are in: src/main/resources/db/migration/
# Naming: V{version}__{description}.sql (e.g., V1__init.sql)
```

## Architecture

This project follows **Clean Architecture** and **Domain-Driven Design (DDD)** principles with a strict separation of concerns.

### Layered Architecture

```
API Layer (Controllers)
    ↓
Application Layer (Services)
    ↓
Domain Layer (Models + Repository Interfaces)
    ↓
Infrastructure Layer (JPA Entities + Implementations)
```

### Domain Module Structure

Each domain follows this 4-layer structure:

```
domain/{domainName}/
├── api/                    # REST Controllers + DTOs
├── application/            # Use case services
├── domain/                 # Pure domain models + repository interfaces
│   ├── model/
│   └── repository/
├── exception/              # Domain-specific error codes
└── infra/                  # Infrastructure implementations
    ├── persistence/        # JPA entities + repositories
    ├── cache/              # Redis cache (if applicable)
    └── notification/       # External integrations (if applicable)
```

### Key Architectural Principles

1. **Domain Independence**: Domain models are pure Java - no JPA, no Spring dependencies
2. **Entity-Domain Separation**:
   - `UserEntity` (JPA) ↔ `User` (Domain) via `fromDomain()` / `toDomain()`
   - Entities are only used in the infrastructure layer
3. **Repository Abstraction**:
   - Domain defines `UserRepository` interface
   - Infrastructure provides `UserRepositoryImpl` using `UserJpaRepository`
4. **Dependency Direction**: Infrastructure → Application → Domain (never reversed)

## Domain Models & Business Logic

### User Domain
- **Purpose**: Authentication, authorization, user management
- **Key Classes**: `User` (domain), `UserEntity` (JPA), `AuthService`, `UserService`
- **Features**: JWT-based authentication (Access + Refresh tokens), email verification via Redis
- **Roles**: `USER` (citizen), `OFFICIAL` (government official), `ADMIN`

### DiscussionRoom Domain
- **Purpose**: Regional discussion rooms where citizens and officials collaborate
- **Key Classes**: `DiscussionRoom` (aggregate root), `Member` (join table), `DiscussionRoomService`
- **Features**: Redis caching strategy with 4 key types (room details, total list, user's joined rooms, room members)
- **Access Levels**: `PUBLIC`, `OFFICIALS_ONLY`, `USER_ONLY`
- **Regions**: 28 cities/counties in Gyeonggi Province (ENUM)

### Message Domain (Not Implemented)
- Planned for real-time chat via WebSocket
- Only `MessageErrorCode` exists currently

### Proposal Domain (Not Implemented)
- Planned for collaborative proposal creation and consent process
- Only `ProposalErrorCode` exists currently

## Code Patterns & Conventions

### Domain Model Pattern
```java
public class User {
    // Factory method for new instances
    public static User create(...) {
        // Validate business rules
        // Return new instance
    }

    // Factory method for DB restoration
    public static User restore(...) {
        // No validation (already validated)
        // Return instance
    }

    // Private constructor to enforce factory methods
    private User(...) {}
}
```

### Service Layer Pattern
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    // Use @Transactional (without readOnly) for write operations
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        // 1. Validate (duplicates, business rules)
        // 2. Transform (e.g., encrypt password)
        // 3. Create domain model via User.create()
        // 4. Save via repository
        // 5. Return DTO
    }
}
```

### Exception Handling Pattern
```java
// Throw BusinessException with domain-specific ErrorCode
if (userRepository.existsByLoginId(loginId)) {
    throw new BusinessException(UserErrorCode.DUPLICATE_LOGIN_ID);
}
```

### Repository Pattern
```java
// Domain layer defines interface
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
}

// Infrastructure layer implements
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository jpaRepository;

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.fromDomain(user);
        UserEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
}
```

## Authentication & Security

### JWT Token Flow
1. Login → Generate Access Token (1h) + Refresh Token (7d)
2. Store Refresh Token in Redis with key `RT:{userId}`
3. All protected endpoints validate Access Token via `JwtAuthenticationFilter`
4. Token refresh: Validate Refresh Token in Redis → Issue new Access Token
5. Logout: Delete Refresh Token from Redis

### Getting Current User in Controllers
```java
@PostMapping("/some-endpoint")
public ResponseEntity<?> someMethod(
    @AuthenticationPrincipal CustomUserDetails userDetails
) {
    Long userId = userDetails.getUserId();
    String role = userDetails.getRole();
    // ...
}
```

### Security Configuration
- Development profile uses `SecurityConfigDev` with relaxed CORS
- JWT filter runs before `UsernamePasswordAuthenticationFilter`
- Public endpoints: `/api/auth/login`, `/api/users/signup`, `/swagger-ui/**`, `/actuator/health/**`

## Redis Usage

### Key Patterns
1. **Refresh Tokens**: `RT:{userId}` (TTL: 7 days)
2. **Email Verification**: `email:verification:{email}` (TTL: 5 minutes)
3. **Discussion Room Info**: `room:{roomId}` (Hash, TTL: 24 hours)
4. **Latest Rooms List**: `list:latest` (ZSet, TTL: 1 hour)
5. **User's Joined Rooms**: `user:{userId}:joined` (ZSet, TTL: 12 hours)
6. **Room Members**: `room:{roomId}:members` (List, TTL: 24 hours)

### Caching Strategy
- **Write-Through**: Create operations immediately cache to Redis
- **Cache-Aside**: Read operations check Redis first, fall back to DB
- **Atomic Updates**: Use Redis transactions (MULTI/EXEC) for consistency
- **Graceful Degradation**: Log Redis failures but continue execution

## Database

### Schema Management
- **Migration Tool**: Flyway (runs automatically on startup)
- **Migration Path**: `src/main/resources/db/migration/`
- **Database**: PostgreSQL 15+
- **Dialect**: PostgreSQL (Hibernate)

### Soft Delete Pattern
- All main tables have `deleted_at` column
- Use `softDelete()` methods instead of physical deletion
- JPA queries should filter out soft-deleted records

### Key Tables
- `users`: User accounts with roles
- `discussion_rooms`: Discussion rooms with region and access level
- `members`: Junction table (user ↔ room many-to-many)
- `chat`: Chat messages (schema only, not implemented)
- `proposals`: Proposals with JSON content (schema only, not implemented)

## Configuration Profiles

- **`local`**: Local development (default)
- **`dev`**: Development server
- Profile-specific files: `application-{profile}.properties`
- Sensitive values (JWT secret, DB password) must be in environment-specific files (not committed to Git)

## API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- All controllers should have proper OpenAPI annotations

## Common Issues & Solutions

### Issue: "Failed to configure a DataSource"
**Solution**: Ensure PostgreSQL is running via Docker Compose and `application-local.properties` exists with correct DB credentials.

### Issue: "Could not connect to Redis"
**Solution**: Start Redis via `docker-compose -f docker-compose.dev.yml up -d`. Check Redis is running on port 6379.

### Issue: JWT token validation fails
**Solution**: Check `jwt.secret` is properly set in application properties (must be Base64-encoded and sufficiently long).

### Issue: Flyway migration fails
**Solution**: Check PostgreSQL enums are created before tables. If migration is broken, you may need to clean the database and restart.

## Important Implementation Notes

### When Adding New Domains
1. Follow the 4-layer structure (api/application/domain/infra)
2. Create domain model with `create()` and `restore()` factory methods
3. Define repository interface in domain layer
4. Implement repository in infrastructure layer with JPA
5. Add domain-specific `ErrorCode` enum
6. Use `@Transactional` on service methods

### When Working with JPA
- Never expose JPA entities outside infrastructure layer
- Always convert: Entity → Domain when reading, Domain → Entity when writing
- Use `@EntityListeners(AuditingEntityListener.class)` for `createdAt`/`updatedAt`
- Extend `BaseEntity` for common audit fields

### When Using Redis
- Always set appropriate TTL
- Use transactions for operations that must be atomic
- Handle Redis failures gracefully (log error, continue with DB)
- Don't cache sensitive data without encryption

### Validation Strategy
1. **DTO Validation**: Use `@Valid` + Bean Validation annotations on request DTOs
2. **Domain Validation**: Domain models validate business rules in factory methods
3. **Service Validation**: Services check external constraints (e.g., uniqueness, authorization)

## Testing Guidelines

### Test Structure
```
src/test/java/org/example/gyeonggi_partners/
└── domain/
    ├── user/
    │   ├── application/
    │   │   └── UserServiceTest.java
    │   └── domain/
    │       └── model/
    │           └── UserTest.java
    └── discussionRoom/
        └── ...
```

### Test Types
- **Unit Tests**: Test domain models and service logic with mocked dependencies
- **Integration Tests**: Use `@SpringBootTest` with test containers (if configured)
- **Security Tests**: Use `@WithMockUser` or `@WithUserDetails` for authenticated endpoints

## Key Dependencies

- **Spring Boot**: 3.5.6
- **Java**: 21
- **PostgreSQL**: 15+ (runtime)
- **Redis**: 7.x (runtime)
- **JWT**: jjwt 0.12.3
- **Flyway**: Database migration
- **Lombok**: Reduce boilerplate
- **SpringDoc OpenAPI**: API documentation

## Code Style

- Use **Lombok** annotations (`@Getter`, `@Builder`, `@RequiredArgsConstructor`) to reduce boilerplate
- Use constructor injection via `@RequiredArgsConstructor` (never field injection)
- Service methods should return DTOs, not domain models or entities
- Keep controllers thin - delegate to services
- Use meaningful variable names (avoid abbreviations)
- Write self-documenting code with clear method names
