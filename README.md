# Collaborative Task Platform

A real-time collaborative task management platform built with modern technologies and clean architecture principles.

## 🚀 Tech Stack

### Frontend
- **Next.js 15** with React 19 and Turbopack
- **TypeScript 5.3+** for type safety
- **Tailwind CSS** with Shadcn UI components
- **Zustand** for state management
- **TanStack Query** for server state
- **Socket.IO** for real-time communication
- **Framer Motion** for animations

### Backend
- **Spring Boot 3.2+** with Java 21
- **PostgreSQL 16** for relational data
- **MongoDB 7.0** for analytics
- **Redis 7.2** for caching and sessions
- **WebSocket** for real-time features
- **GraalVM Native Image** for performance

### Monorepo & Tooling
- **Nx** for monorepo management
- **pnpm** for package management
- **Biome** for linting and formatting
- **Docker** for containerization
- **GitHub Actions** for CI/CD

## 🏗️ Project Structure

```
collaborative-task-platform/
├── apps/
│   ├── web/                    # Next.js frontend
│   └── api/                    # Spring Boot backend
├── libs/
│   ├── shared-types/           # TypeScript API contracts
│   ├── ui-components/          # Shared UI components
│   ├── utils/                  # Shared utilities
│   └── constants/              # Shared constants
├── tools/
│   └── generators/             # Nx code generators
├── docs/                       # Documentation
└── .kiro/specs/               # Feature specifications
```

## 🛠️ Development Setup

### Prerequisites
- Node.js 18.17.0+
- pnpm 8.0.0+
- Java 21
- Docker & Docker Compose

### Quick Start

1. **Clone and install dependencies:**
   ```bash
   git clone <repository-url>
   cd collaborative-task-platform
   pnpm install
   ```

2. **Start development environment:**
   ```bash
   # Start databases
   docker-compose up postgres mongodb redis -d
   
   # Start backend
   cd apps/api
   ./gradlew bootRun
   
   # Start frontend (in another terminal)
   pnpm dev --filter=@collaborative-task-platform/web
   ```

3. **Access the application:**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080/api/v1
   - WebSocket: http://localhost:8081

### Full Docker Development

```bash
# Start all services including app
docker-compose --profile development up

# Or start just infrastructure
docker-compose up postgres mongodb redis -d
```

## 📝 Available Scripts

### Root Level
- `pnpm dev` - Start all applications in development mode
- `pnpm build` - Build all applications
- `pnpm test` - Run all tests
- `pnpm lint` - Lint all code
- `pnpm format` - Format all code
- `pnpm affected:build` - Build only affected projects
- `pnpm affected:test` - Test only affected projects

### Frontend (apps/web)
- `pnpm dev` - Start Next.js development server
- `pnpm build` - Build for production
- `pnpm start` - Start production server
- `pnpm type-check` - Run TypeScript checks

### Backend (apps/api)
- `./gradlew bootRun` - Start Spring Boot application
- `./gradlew build` - Build application
- `./gradlew test` - Run tests
- `./gradlew nativeCompile` - Build native image

## 🧪 Testing

The project uses a comprehensive testing strategy:

- **Unit Tests**: Jest for frontend, JUnit for backend
- **Integration Tests**: Testcontainers for database testing
- **Property-Based Tests**: For correctness validation
- **E2E Tests**: Playwright for end-to-end testing

```bash
# Run all tests
pnpm test

# Run tests for specific project
pnpm test --filter=@collaborative-task-platform/web
```

## 🔧 Code Generation

Use Nx generators for consistent code scaffolding:

```bash
# Generate a new component
nx generate @nx/react:component MyComponent --project=web

# Generate a new library
nx generate @nx/js:library my-lib

# Use custom generators
nx generate component MyComponent --project=web
```

## 🚀 Deployment

### Production Build

```bash
# Build all applications
pnpm build

# Build native image for backend
cd apps/api
./gradlew nativeCompile
```

### Docker Production

```bash
# Build production images
docker-compose -f docker-compose.prod.yml build

# Deploy
docker-compose -f docker-compose.prod.yml up -d
```

## 📊 Monitoring & Observability

- **Health Checks**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`
- **OpenTelemetry**: Distributed tracing enabled

## 🤝 Contributing

1. Create a feature branch: `git checkout -b feature/amazing-feature`
2. Make your changes following the coding standards
3. Run tests: `pnpm test`
4. Run linting: `pnpm lint`
5. Commit using conventional commits: `git commit -m "feat: add amazing feature"`
6. Push and create a Pull Request

### Coding Standards

- Use **Biome** for linting and formatting
- Follow **SOLID principles** and **Clean Architecture**
- Write comprehensive tests for new features
- Use **TypeScript** strictly (no `any` types)
- Follow **conventional commits** format

## 📚 Documentation

- [Architecture Decision Records](./docs/adr/)
- [API Documentation](./docs/api/)
- [Deployment Guide](./docs/deployment/)
- [Contributing Guide](./docs/contributing/)

## 🔒 Security

- JWT authentication with refresh tokens
- CORS configuration
- Security headers
- Input validation and sanitization
- Rate limiting
- SQL injection protection

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the documentation in `/docs`
- Review the specifications in `/.kiro/specs`