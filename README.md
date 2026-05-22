# StreamForge

A cloud-native distributed video streaming backend built with Spring Boot.

StreamForge is a scalable backend platform that supports secure video uploads, asynchronous video processing, object storage integration, caching, JWT authentication, role-based authorization, FFmpeg transcoding, thumbnail generation, and HTTP video streaming.

This project was built to demonstrate real-world backend engineering concepts beyond basic CRUD applications.

---

# Features

## Authentication & Security

* JWT Authentication
* Spring Security Integration
* Role-Based Access Control (RBAC)
* Method-Level Authorization using `@PreAuthorize`
* BCrypt Password Encryption
* Secure Protected APIs

## Video Management

* Video Upload API
* Multipart File Handling
* Metadata Persistence in PostgreSQL
* Video Listing APIs
* HTTP Range Video Streaming
* Secure Presigned Video URLs

## Distributed System Features

* RabbitMQ Event-Driven Architecture
* Asynchronous Video Processing
* Producer-Consumer Workflow
* Background FFmpeg Processing

## Video Processing

* FFmpeg Video Transcoding (720p)
* Thumbnail Generation
* Async Media Processing Pipeline

## Cloud-Native Storage

* MinIO Object Storage
* S3-Compatible Architecture
* Processed Video Storage
* Thumbnail Storage

## Performance & Scalability

* Redis Caching
* Cache Eviction Strategy
* Stateless JWT Architecture
* Distributed Processing Support

## API Reliability

* DTO-Based Architecture
* Request Validation
* Global Exception Handling
* Structured Error Responses

---

# Tech Stack

| Technology      | Purpose                        |
| --------------- | ------------------------------ |
| Java 21         | Core Language                  |
| Spring Boot     | Backend Framework              |
| Spring Security | Authentication & Authorization |
| JWT             | Stateless Authentication       |
| PostgreSQL      | Relational Database            |
| Redis           | Caching Layer                  |
| RabbitMQ        | Message Queue                  |
| MinIO           | Object Storage                 |
| FFmpeg          | Video Processing               |
| Maven           | Dependency Management          |
| Lombok          | Boilerplate Reduction          |
| Docker          | Containerization               |

---

# Architecture Overview

```text
Client
   ↓
Spring Boot API
   ↓
JWT Authentication
   ↓
Redis Cache
   ↓
PostgreSQL Metadata Storage
   ↓
RabbitMQ Event Queue
   ↓
FFmpeg Processing Worker
   ↓
MinIO Object Storage
```

---

# Project Structure

```text
src/main/java/com/streamforge
│
├── config
├── controller
├── dto
├── entity
├── exception
├── repository
├── security
├── service
└── StreamforgeApplication
```

---

# Authentication Flow

```text
User Login
    ↓
JWT Token Generated
    ↓
Client Stores Token
    ↓
Authorization: Bearer <token>
    ↓
JWT Filter Validates Token
    ↓
Protected APIs Accessible
```

---

# Video Processing Workflow

```text
Upload Video
     ↓
Store Original Video in MinIO
     ↓
Save Metadata in PostgreSQL
     ↓
Publish RabbitMQ Event
     ↓
Consumer Downloads Video
     ↓
FFmpeg Transcoding
     ↓
Thumbnail Generation
     ↓
Upload Processed Files to MinIO
     ↓
Update Database
```

---

# Roles & Authorization

| Role      | Permissions                       |
| --------- | --------------------------------- |
| USER      | View and stream videos            |
| MODERATOR | Upload, view, and stream videos   |
| ADMIN     | Full upload and management access |

Authorization is implemented using:

```java
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
```

---

# API Endpoints

## Authentication APIs

### Register

```http
POST /api/auth/register
```

### Login

```http
POST /api/auth/login
```

---

## Video APIs

### Upload Video

```http
POST /api/videos/upload
```

### Get All Videos

```http
GET /api/videos
```

### Stream Video

```http
GET /api/videos/stream/{videoId}
```

### Get Secure Video URL

```http
GET /api/videos/{videoId}/url
```

---

# Redis Caching

The video listing API is cached using Redis.

```text
First Request → PostgreSQL
Next Requests → Redis Cache
```

Cache invalidation occurs automatically when new videos are uploaded.

---

# RabbitMQ Integration

RabbitMQ is used for asynchronous background processing.

### Producer

* Publishes video processing events

### Consumer

* Listens for processing jobs
* Executes FFmpeg commands
* Uploads processed media to MinIO

---

# MinIO Object Storage

MinIO is used as an S3-compatible object storage system.

Stored Objects:

* Original Videos
* Processed Videos
* Generated Thumbnails

Benefits:

* Cloud-native architecture
* Scalable media storage
* S3 compatibility
* Distributed storage support

---

# Validation & Error Handling

## Request Validation

Implemented using:

```java
@NotBlank
@Email
@Size
```

## Global Exception Handling

Implemented using:

```java
@RestControllerAdvice
```

Provides:

* Consistent error responses
* Clean API structure
* Better debugging experience

---

# Video Streaming

Supports HTTP Range Requests for efficient streaming.

Features:

* Partial Content Responses (`206`)
* Browser Video Seeking
* Progressive Streaming
* Efficient Chunk-Based Delivery

---

# Setup Instructions

## 1. Clone Repository

```bash
git clone <repository-url>
cd streamforge
```

---

## 2. Start Required Services

### PostgreSQL

```bash
brew services start postgresql
```

### Redis

```bash
brew services start redis
```

### RabbitMQ

```bash
brew services start rabbitmq
```

RabbitMQ Dashboard:

```text
http://localhost:15672
```

---

## 3. Start MinIO

```bash
docker run -p 9000:9000 -p 9001:9001 \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  quay.io/minio/minio server /data --console-address ":9001"
```

MinIO Console:

```text
http://localhost:9001
```

Create bucket:

```text
streamforge-videos
```

---

## 4. Configure Application

Update `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/streamforge
    username: postgres
    password: password

  data:
    redis:
      host: localhost
      port: 6379

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

jwt:
  secret: your-secret-key
  expiration: 86400000

minio:
  url: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: streamforge-videos
```

---

## 5. Run Application

```bash
mvn spring-boot:run
```

---

# Future Improvements

* HLS Adaptive Streaming
* Docker Compose Setup
* Kubernetes Deployment
* CDN Integration
* Swagger/OpenAPI Documentation
* CI/CD Pipeline
* Microservice Separation
* AI-Based Video Moderation
* Analytics Dashboard
* Monitoring & Observability

---

# Learning Outcomes

This project demonstrates practical knowledge of:

* Distributed Systems
* Event-Driven Architecture
* Backend Scalability
* Cloud-Native Design
* Object Storage Systems
* Async Processing
* Secure API Development
* Media Streaming Architecture
* Enterprise Spring Boot Development

---

# Author

Built as a backend engineering project focused on scalable distributed media systems using Spring Boot and modern backend technologies.
