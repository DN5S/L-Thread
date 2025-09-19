# Product Requirements Document (PRD)
# L-Thread - Ephemeral Anonymous Imageboard System

**Version:** 1.0  
**Date:** 2025-01-09  
**Status:** Draft  
**Classification:** Internal

---

## Executive Summary

L-Thread is an ephemeral anonymous imageboard system based on the philosophy of Serial Experiments Lain. By intentionally rejecting data persistence, it implements users' complete anonymity and the 'right to be forgotten' at the system level. The system automatically purges old data based on memory constraints, and all components are containerized for perfect control.

---

## 1. Project Overview

### 1.1 Project Identity
- **Project Name:** L-Thread
- **Package Name:** `dev.dn5s.lthread`
- **Philosophy:** Serial Experiments Lain - "Layer of the Wired"

### 1.2 Vision
A new form of communication platform that rejects data persistence and embraces digital oblivion

### 1.3 Core Values
| Value | Description |
|-------|-------------|
| **Complete Anonymity** | No moderators except server operator, no user identification data stored |
| **Intentional Forgetting** | System autonomously destroys old memories for survival |
| **Ruthless Simplicity** | No convenience features, clear and simple system rules |
| **Controlled Lifecycle** | Perfect control over creation and destruction of all components |

### 1.3 Target Users
- Users seeking complete anonymity
- Users who don't want to leave digital footprints
- Users preferring temporary and ephemeral communication

### 1.4 Key Differentiators
- Complete absence of user account system
- Automatic data destruction mechanism
- Minimal UI in 90s textboard style
- Complete absence of user interaction metrics (likes, views, etc.)

---

## 2. Functional Requirements

### 2.1 Board System

#### 2.1.1 Board Structure
- **Type**: Static hardcoded boards (no dynamic creation)
- **Example Boards**: `/general`, `/tech`, `/random`
- **Policy Sharing**: All boards apply the same destruction policy

#### 2.1.2 Board Display Rules
| Item | Specification |
|------|---------------|
| Sort Order | Recent Activity Time (Bumped) |
| Threads per Page | 15 |
| Thread Preview | OP + 3 Recent Replies |
| Pagination | Supported |

### 2.2 Thread System

#### 2.2.1 Thread Creation Rules
| Category | Requirements |
|----------|--------------|
| Text | Maximum 2,000 characters, Unicode text only |
| Image | **Required**, 20MB or less |
| Allowed Formats | JPEG, PNG, GIF |
| Max Resolution | 10,000 x 10,000 pixels |

#### 2.2.2 Reply Rules
| Category | Requirements |
|----------|--------------|
| Text | Maximum 2,000 characters |
| Image | **Optional**, 20MB or less |
| Bumping | New reply moves thread to top of list |

### 2.3 Anonymity System

#### 2.3.1 Default Anonymity
- All posts displayed as "Anonymous" by default
- IP addresses and user identification data storage prohibited

#### 2.3.2 Tripcode System
| Item | Content |
|------|---------|
| Input Format | `Name#Password` |
| Processing | Hash password with salt |
| Display Format | `Name ◆tripcode` (e.g., `Lain ◆2gJdJ0p8Wx`) |
| Nature | Cryptographic signature, not an account |

### 2.4 Data Immutability
- Only CREATE and READ operations allowed
- No UPDATE or DELETE operations
- No user-level post modification/deletion features

---

## 3. Technical Architecture

### 3.1 System Architecture
```
┌─────────────────────────────────────┐
│         Client (Browser)            │
└─────────────┬───────────────────────┘
              │ HTTP/HTTPS
┌─────────────▼───────────────────────┐
│    Application Server (Spring Boot)  │
│         - Stateless                  │
│         - Kotlin                     │
│         - REST API                   │
└──────┬──────────────┬───────────────┘
       │              │
┌──────▼────┐  ┌──────▼────┐
│   Redis   │  │  Volume   │
│(Metadata) │  │ (Images)  │
└───────────┘  └───────────┘
```

### 3.2 Technology Stack
| Layer | Technology |
|-------|------------|
| Runtime | Microsoft OpenJDK 21 |
| Backend Framework | Spring Boot 3.5.5 with Kotlin 1.9.25 |
| Database | Redis (In-Memory, NO persistence) |
| File Storage | Container Named Volume |
| Containerization | Docker/Podman |
| Orchestration | Podman-compose |
| Image Processing | Thumbnailator (JVM-based) |

### 3.3 Container Configuration
```yaml
services:
  app:
    - Spring Boot Application
    - Port: 8080
    - Stateless
  
  db:
    - Redis (EPHEMERAL - no persistence)
    - Port: 6379
    - Memory Limit: 2GB
    - Policy: noeviction (app controls deletion)
  
  volumes:
    - images: All image storage (originals + thumbnails/)
```

---

## 4. Data Model

### 4.1 Redis Data Structure

#### 4.1.1 Global Counters
```
thread:id    → INTEGER (INCR)
post:id      → INTEGER (INCR)
```

#### 4.1.2 Post (Hash)
```
post:{post_id}
  ├── text         → STRING (max 2,000 chars)
  ├── image_path   → STRING (file path)
  ├── thumbnail    → STRING (thumbnail path)
  └── timestamp    → STRING (ISO 8601)
```

#### 4.1.3 Thread (List)
```
thread:{thread_id}
  └── [post_id1, post_id2, ...]  // First element is always OP
```

#### 4.1.4 Board (Sorted Set)
```
board:{board_name}
  └── member: thread_id
      score: last_post_timestamp
```

### 4.2 Data Flow

#### Thread Creation Flow
```mermaid
1. INCR thread:id, post:id
2. HSET post:{id} (save data)
3. LPUSH thread:{id} post_id
4. ZADD board:{name} timestamp thread_id
5. Save image → Volume
6. Generate thumbnail → Volume
```

#### Reply Creation Flow
```mermaid
1. INCR post:id
2. HSET post:{id} (save data)
3. RPUSH thread:{id} post_id
4. ZADD board:{name} new_timestamp thread_id (bumping)
5. Save image (optional)
```

---

## 5. API Specification

### 5.1 RESTful Endpoints

| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| POST | `/board/{board_name}` | Create thread | Multipart: text, image* | `{"thread_id": 123, "post_id": 456}` |
| POST | `/thread/{thread_id}` | Post reply | Multipart: text, image | `{"post_id": 789}` |
| GET | `/board/{board_name}` | List threads | Query: page | Thread array with previews |
| GET | `/thread/{thread_id}` | Thread detail | - | Thread with all posts |
| GET | `/static/{filename}` | Original image | - | Binary image data |
| GET | `/static/thumbnails/{filename}` | Thumbnail | - | Binary image data |

### 5.2 Error Responses
| Code | Message | Description |
|------|---------|-------------|
| 404 | "Signal Lost." | Accessing destroyed thread |
| 413 | "Data overflow." | File size exceeded |
| 400 | "Invalid transmission." | Invalid request format |

---

## 6. Non-Functional Requirements

### 6.1 Performance Requirements
| Metric | Target | Notes |
|--------|--------|-------|
| Response Time | < 200ms (P95) | Leveraging Java 21 Virtual Threads |
| Concurrent Users | 1,000 | |
| Throughput | 100 req/sec | |
| Redis Memory | Max 2GB | |
| JVM Heap | Max 1GB | Using ZGC for low-latency GC |

### 6.2 Security Requirements
- HTTPS-only communication
- SQL Injection prevention (parameter binding)
- XSS prevention (no HTML tags)
- CSRF token implementation
- Rate Limiting (30 requests per minute per IP)

### 6.3 UI/UX Requirements
| Principle | Implementation |
|-----------|----------------|
| Minimalism | 90s textboard style, monochrome background |
| Impersonality | Complete absence of likes, recommendations, view counts |
| Visualizing Destruction | Custom 404 messages ("Signal Lost.") |
| Minimal Loading | CSS/JS minimization, no CDN usage |

---

## 7. Data Lifecycle Management

### 7.1 Automatic Destruction Policy (Thread Pruning)

#### 7.1.1 Trigger Conditions
- Redis memory usage > 80% (1.6GB)

#### 7.1.2 Destruction Process
```python
1. Memory monitoring (5-minute intervals)
2. if memory_usage > threshold:
   3. target = ZRANGE board:* 0 10  # 10 oldest threads
   4. for thread in target:
      5. Delete all post data
      6. Delete all image files
      7. Delete thread metadata
   8. log("Pruned {count} threads")
```

**CRITICAL:** Only the application scheduler may delete data. Redis is configured with `noeviction` policy to prevent autonomous deletion that would orphan image files. The application is the sole authority for coordinated data destruction.

### 7.2 Backup Policy
| Data Type | Backup | Reason |
|-----------|--------|--------|
| Image Data | ❌ No Backup | Ephemeral philosophy |
| Redis Data | ❌ No Backup, No Persistence | Pure in-memory, dies with container |
| Source Code | ✅ Backup | System recovery |
| Config Files | ✅ Backup | System recovery |

**Critical:** Redis MUST run without ANY persistent volume. Data exists only in container memory.

---

## 8. Deployment & Operations

### 8.1 Deployment Strategy
```bash
# System initialization
podman-compose build

# System start
podman-compose up -d

# Complete system destruction
podman-compose down --volumes
```

### 8.2 Monitoring Metrics
| Metric | Threshold | Action |
|--------|-----------|--------|
| Redis Memory | > 80% | Execute Thread Pruning |
| Disk Usage | > 90% | Trigger Alert |
| Response Time | > 500ms | Performance Investigation |
| Error Rate | > 1% | Log Analysis |

### 8.3 Operational Principles
- **No Backup**: Data backup prohibited
- **No Recovery**: No data recovery attempts on failure
- **Clean Slate**: Complete reinitialization on system restart
- **Ephemeral Redis**: Redis runs purely in-memory with no disk persistence
- **Single Deletion Authority**: Only the application scheduler may delete data

**Philosophy:** The system embraces impermanence. When `podman-compose down` is executed, all memories vanish completely. This is not a bug - it is the core feature.

---

## 9. Development Milestones

### Phase 1: Foundation (2 weeks)
- [ ] Spring Boot 3.5.5 + Kotlin 1.9.25 project initialization with `dev.dn5s.lthread` package
- [ ] Java 21 development environment setup (Microsoft OpenJDK)
- [ ] Redis integration and data model implementation
- [ ] Docker/Podman containerization

### Phase 2: Core Features (3 weeks)
- [ ] Thread creation/retrieval API
- [ ] Reply posting API
- [ ] Image upload and thumbnail generation (Thumbnailator)
- [ ] Tripcode system

### Phase 3: Auto-Pruning (2 weeks)
- [ ] Memory monitoring system
- [ ] Automatic thread destruction logic
- [ ] Destruction logging system

### Phase 4: UI/UX (2 weeks)
- [ ] Minimal web interface
- [ ] Responsive design
- [ ] "Signal Lost" pages

### Phase 5: Stabilization (1 week)
- [ ] Load testing
- [ ] Security audit
- [ ] Documentation

---

## 10. Risk & Mitigation Strategy

| Risk | Impact | Mitigation Strategy |
|------|--------|---------------------|
| Malicious content upload | High | Image format validation, file size limits |
| DDoS attacks | High | Rate Limiting, Cloudflare |
| Excessive pruning due to memory shortage | Medium | Threshold adjustment, monitoring |
| Legal issues (illegal content) | High | **Acceptance** - System operates in philosophical gray area by design |

**Philosophy:** The system does not compromise its core principles for risk mitigation. Risks are acknowledged and accepted as part of the system's nature.

---

## 11. Constraints & Assumptions

### Development Environment Requirements
| Component | Version | Notes |
|-----------|---------|-------|
| JDK | 21 | Microsoft OpenJDK recommended |
| Kotlin | 1.9.25 | Via Gradle plugin |
| Spring Boot | 3.5.5 | |
| Gradle | 8.x | Wrapper included |
| Redis | 7.x | Alpine version for containers |

### Critical Design Decisions
1. **Redis Ephemerality**: Redis MUST run without persistent volumes. Data exists only in container RAM.
2. **Deletion Authority**: Only the application controls data deletion. Redis `noeviction` policy prevents orphaned files.
3. **No Moderation**: System has no report/delete features. Legal gray area is accepted, not mitigated.
4. **Unified Volume**: Single volume for all images (originals in root, thumbnails in subdirectory).

### Constraints
- Single server operation (no distributed system support)
- No search functionality
- No archiving features
- No external API integrations
- No user moderation tools

### Assumptions
- Users acknowledge and accept data destruction
- Malicious users are minority
- Traffic is at predictable levels
- Operator accepts legal risks

---

## 12. Glossary

| Term | Definition |
|------|------------|
| L-Thread | The project name, referencing "Layer" from Serial Experiments Lain |
| OP (Original Post) | First post that started the thread |
| Bumping | Moving thread to top of list with new reply |
| Pruning | Process of automatically deleting old threads |
| Tripcode | Password-based anonymous identifier |
| Signal Lost | Message displayed when accessing destroyed data |
| Layer | Reference to the conceptual space where data temporarily exists |

---

## Appendix A: Configuration Examples

### Package Structure
```
dev.dn5s.lthread/
├── controller/       # REST API endpoints
├── service/          # Business logic
├── repository/       # Redis data access
├── model/           # Data models
├── config/          # Spring configuration
├── scheduler/       # Auto-pruning scheduler
└── util/            # Image processing, tripcode generation
```

### Main Application Class
```kotlin
package dev.dn5s.lthread

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LThreadApplication

fun main(args: Array<String>) {
    runApplication<LThreadApplication>(*args)
}
```

### compose.yml
```yaml
version: '3.8'
services:
  app:
    build: .
    image: lthread:latest
    container_name: lthread-app
    ports:
      - "8080:8080"
    environment:
      - REDIS_HOST=db
      - REDIS_PORT=6379
      - MEMORY_THRESHOLD=0.8
    volumes:
      - images:/app/static
    depends_on:
      - db
  
  db:
    image: redis:7-alpine
    container_name: lthread-redis
    command: redis-server --maxmemory 2gb --maxmemory-policy noeviction
    ports:
      - "6379:6379"
    # WARNING: NO VOLUMES MOUNTED
    # Redis data exists only in container memory
    # All data is lost when container stops
    # This is intentional - not a mistake

volumes:
  images:  # Contains both originals and thumbnails/ subdirectory
```

### application.yml
```yaml
spring:
  application:
    name: lthread
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 25MB

lthread:
  boards:
    - general
    - tech
    - random
  thread:
    page-size: 15
    preview-replies: 3
  storage:
    images-path: /app/static
    thumbnails-subdir: thumbnails
  pruning:
    memory-threshold: ${MEMORY_THRESHOLD:0.8}
    check-interval: 300000  # 5 minutes in ms
```

## Appendix B: Container Configuration

### build.gradle.kts
```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.dn5s"
version = "0.0.1-SNAPSHOT"
description = "L-Thread - Anonymous Ephemeral Imageboard"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Image Processing
    implementation("net.coobird:thumbnailator:0.4.20")
    
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### settings.gradle.kts
```kotlin
rootProject.name = "lthread"
```

### .gitignore
```gitignore
# Gradle
.gradle
build/
!gradle/wrapper/gradle-wrapper.jar
!**/src/main/**/build/
!**/src/test/**/build/

# IDE
.idea
*.iws
*.iml
*.ipr
out/
!**/src/main/**/out/
!**/src/test/**/out/
.vscode/

# OS
.DS_Store
Thumbs.db

# Application
*.log
/static/
```

### gradle.properties
```properties
# Gradle performance optimization
org.gradle.jvmargs=-Xmx2048m -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.caching=true

# Kotlin
kotlin.code.style=official
kotlin.incremental=true
```

### Dockerfile
```dockerfile
# Build stage using Microsoft OpenJDK 21
FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu AS build
WORKDIR /workspace/app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test

# Runtime stage using Microsoft OpenJDK 21
FROM mcr.microsoft.com/openjdk/jdk:21-distroless
WORKDIR /app
COPY --from=build /workspace/app/build/libs/lthread-*.jar app.jar
EXPOSE 8080
# JVM optimizations for Java 21
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

### Dockerfile.alpine (Alternative)
```dockerfile
# Alternative using Eclipse Temurin for Alpine Linux
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S lthread && adduser -S lthread -G lthread
USER lthread:lthread
WORKDIR /app
COPY --from=build /workspace/app/build/libs/lthread-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

*"Present day, present time... and the system forgets."*

**Document End**