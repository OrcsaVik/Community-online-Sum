
# Core Architecture and High-Level Feature Design

This project follows a layered, modular architecture, typical for a Spring Boot application, with clear separation of concerns across its Maven modules:

1.  **`paicoding-api` (Contract Layer):**
    *   **Role:** Defines the public interfaces, data transfer objects (DTOs), and shared models used across all other modules. It acts as the contract for inter-module communication.
    *   **Dependencies:** Minimal, primarily `lombok` for boilerplate reduction. `mybatis-plus-boot-starter`, `jackson-dataformat-xml`, `knife4j-openapi2-spring-boot-starter`, and `transmittable-thread-local` are marked as `provided`, indicating they are used for compilation/API definition but not bundled, relying on downstream modules to provide them.
    *   **Key Principle:** Ensures loose coupling and clear API boundaries. Changes here impact all consumers.

2.  **`paicoding-core` (Foundation/Utility Layer):**
    *   **Role:** Houses core utilities, common components, infrastructure services, and cross-cutting concerns that are fundamental to the application but not specific to any single business domain. This includes caching, logging, messaging, and network utilities.
    *   **Dependencies:** Depends on `paicoding-api` for shared contracts. Integrates with Spring Context, Servlet API, SLF4J, Logback, Guava, Jackson, Spring Web, Spring Boot, Spring Data Redis, Caffeine (for local caching), IP2Region (for IP lookup), Knife4j (for API documentation), Spring Mail, and RabbitMQ (for asynchronous messaging).
    *   **Key Principle:** Provides a robust, reusable foundation for business logic without direct business domain implementation.

3.  **`paicoding-service` (Business Logic Layer):**
    *   **Role:** Implements the core business logic of the forum application. This layer orchestrates operations, interacts with data stores, and applies business rules.
    *   **Dependencies:** Depends on `paicoding-core` for foundational services and `paicoding-api` for data models. Integrates with Mybatis-Plus (for database interaction), MySQL Connector, Aliyun OSS (for object storage), MapStruct (for object mapping), Elasticsearch (for search functionality), Hutool (general utilities), Java JWT (for token handling), Spring Security Crypto (for password encoding), and Thymeleaf (for server-side rendering, though marked `provided` here, suggesting it's primarily for template processing within services if needed).
    *   **Key Principle:** Contains the "what" of the application's functionality, translating user requests into system actions and data manipulations.

4.  **`paicoding-ui` (Frontend Resources Layer):**
    *   **Role:** Holds frontend resources, primarily Thymeleaf templates, which are used for server-side rendering of the web interface.
    *   **Dependencies:** Depends on `spring-boot-starter-thymeleaf`.
    *   **Key Principle:** Provides the presentation layer assets, tightly coupled with the server-side rendering approach.

5.  **`paicoding-web` (Presentation/Application Layer):**
    *   **Role:** The entry point of the application. It exposes RESTful APIs, serves web pages, handles HTTP requests, and integrates all other modules to deliver the complete forum experience.
    *   **Dependencies:** Depends on `paicoding-ui` (for templates) and `paicoding-service` (for business logic). Includes `spring-boot-starter-web` (for web capabilities), `spring-boot-starter-actuator` (for monitoring), `micrometer-registry-prometheus` (for metrics), Liquibase (for database schema migration), Spring Boot Configuration Processor, Commons IO, Hutool, and iTextPDF.
    *   **Key Principle:** The user-facing component, responsible for request routing, response generation, and overall application orchestration.

**High-Level Feature Design (Inferred):**

Based on the dependencies and module structure, the `paicoding-forum` project likely supports the following high-level features:

*   **User Management:**
    *   User registration, login, and authentication (likely using JWT).
    *   User profiles.
    *   Password management (Spring Security Crypto).
*   **Content Management (Forum Posts/Articles):**
    *   Creation, editing, and deletion of forum posts/articles.
    *   Categorization and tagging of content.
    *   Rich text editing (implied by general forum functionality).
*   **Commenting and Interaction:**
    *   Commenting on posts.
    *   Likes/upvotes/downvotes (common forum features).
*   **Search Functionality:**
    *   Full-text search across forum content (Elasticsearch)
*   **Monitoring and Metrics:**
    *   Application health and performance monitoring (Spring Boot Actuator, Prometheus).
*   **Database Management:**
    *   Database schema evolution and migration (Liquibase).
*   **Internationalization/Localization:**
    *   Potentially supported by Spring's capabilities, though not explicitly called out in dependencies.
*   **Config repair Support**

*   **Proxy Support:**
    *   As discussed, the `ProxyCenter` in `paicoding-core` indicates the ability to make external requests through rotating proxies, useful for scraping, API integrations, or bypassing geo-restrictions.