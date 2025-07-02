# Smart Onboarding Service 🚀

A reactive Spring Boot microservice for streamlined onboarding processes.

## Key Features ✨
- [X] API is built using
  - ☕ Java 21
  - 🌱 Spring Boot 3.4.2
  - ⚡ Spring WebFlux
  - 🍃 Spring Data MongoDB Reactive
  - ✅ Spring Validation
  - 🎯 Spring AOP
- [X] 🗄️ MongoDB is used as the database
- [X] 📦 Gradle is used for dependency management
- [X] 📝 Swagger UI for API documentation
- [X] 🧪 Testing
  - JUnit 5 tests
  - Testcontainers for MongoDB integration tests
  - Reactor Test for reactive streams testing
- [X] 🎯 Quality Assurance
  - Jacoco for test coverage
  - SonarQube for code quality analysis
- [X] 📚 Documentation
  - OpenAPI/Swagger documentation
  - Detailed API documentation with examples
- [X] 🔄 Cross-cutting Concerns
  - Method execution logging using AOP
  - Custom metrics using Micrometer
  - Validation using Jakarta Validation

## Getting Started 🚦

### Prerequisites 📋
- ☕ Java 21+
- 🍃 MongoDB
- 📦 Gradle
- 🔍 SonarQube (optional, for code analysis)

### Building the Application 🏗️
```
    ./gradlew build
```
### Running the Application 🏃
```
./gradlew bootRun
```
### Running Tests 🧪
```
./gradlew test
```
### Running Code Coverage 📊
```
./gradlew jacocoTestReport
```
### Running SonarQube Analysis 🔍
```
./gradlew sonarqube
```

## API Documentation 📚
Swagger UI is available at: http://localhost:8080/swagger-ui.html

The API provides endpoints for:
- 🔄 Biller Management
  - Create, Read, Update, Delete operations
  - Pagination support
  - Status tracking
  - Document management
- 👥 User Management
  - Create, Read, Update, Delete operations
  - Bulk user creation
  - Role management
  - Pagination support

## Dependencies 📦
- 🌱 Spring Boot Starters
  - spring-boot-starter-webflux
  - spring-boot-starter-data-mongodb-reactive
  - spring-boot-starter-validation
  - spring-boot-starter-actuator
  - spring-boot-starter-aop
- 📝 Documentation
  - springdoc-openapi-starter-webflux-ui
- 📋 Logging
  - logback-classic
  - slf4j-api
- 🛠️ Utilities
  - modelmapper
  - lombok
- 🧪 Testing
  - spring-boot-starter-test
  - testcontainers
  - reactor-test
  - embedded mongodb

## Project Structure 📂

The API provides endpoints for:

```
📂 src/
├── main/
│ ├── java/
│ │ └── com/aci/
│ │ ├── config/ # Configuration classes
│ │ ├── controller/ # REST controllers
│ │ ├── dto/ # Data Transfer Objects
│ │ ├── exceptions/ # Custom exceptions
│ │ ├── logging/ # Logging configurations
│ │ ├── model/ # Domain models
│ │ ├── repository/ # MongoDB repositories
│ │ ├── service/ # Business logic
│ │ └── utils/ # Utility classes
│ └── resources/
└── test/
```

## Quality Assurance 🎯
- 📊 Test Coverage Reports: `build/jacocoHtml/`
- 🔍 SonarQube Dashboard: http://localhost:9000
- 🎨 Code Formatting: `./gradlew formatCode`

## Monitoring and Metrics 📈
- Health checks via Spring Actuator
- Custom metrics for business processes
- Performance monitoring endpoints

## Contributing 🤝
1. Ensure code formatting by running `./gradlew formatCode`
2. Maintain test coverage for new features
3. Update documentation as needed
4. Follow the established coding standards

## Support 💬
For support and questions, please contact the development team.