# Online Project Management Tool

A multithreaded enterprise project and task management web application built with **Java 21**, **JDBC**, a **thread-safe connection pool**, an **embedded relational SQL engine**, and a lightweight **HTTP Servlet architecture**.

The platform provides dedicated, role-separated workspaces for **Administrators**, **Project Managers**, and **Team Members**, featuring automated real-time project progress synchronization, an interactive Kanban board, precision work time logging, per-task technical discussions, and non-blocking security audit logging.

---

## Key Features

This project demonstrates production-grade enterprise software architecture built on core Java standards:

| Architectural Component | Engineering Pattern / Concept | Implementation in Codebase |
| :--- | :--- | :--- |
| **Object-Oriented Design** | Polymorphism, Inheritance, Interfaces, Exception Hierarchy | `BaseEntity`, `Identifiable<ID>`, `Auditable`, polymorphic `AbstractJdbcDao<T, ID>`, `GenericDao<T, ID>`, 5 domain services (`AuthService`, `ProjectService`, `TaskService`, `UserService`), and structured custom exception hierarchy (`AppException`, `ValidationException`, `ResourceNotFoundException`, `AuthenticationException`, `DatabaseException`). |
| **Collections & Generics** | Type-Safe Containers, Thread-Safe Maps & Queues | Extensive use of Java Collections Framework and Generics: `AbstractJdbcDao<T, ID>`, type-safe `Optional<T>` and `List<T>` repositories, `ConcurrentHashMap` for session registry and event subscriptions, `LinkedHashMap` for deterministic order preservation, and `BlockingQueue` in thread pool. |
| **Multithreading & Concurrency** | Worker Pools, Locks, Thread Isolation & Event Bus | Custom `AsyncWorkerPool` with `ThreadPoolExecutor`, bounded queue, and named daemon threads (`PMTool-Worker-N`); `ThreadSafeConnectionPool` with double-checked locking and synchronized pool acquisition; `ThreadLocal<User>` security context isolation; non-blocking asynchronous audit log worker via `EventBus`. |
| **Database Operations (DAO Pattern)** | Repository Abstraction, Encapsulated Query Layer | 8 dedicated DAO repositories (`UserDao`, `ProjectDao`, `MilestoneDao`, `TaskDao`, `TaskTimeLogDao`, `CommentDao`, `AuditLogDao`, `SystemSettingDao`) implementing the Data Access Object pattern with parameterized SQL queries and row mapping. |
| **Relational Database Connectivity (JDBC)** | Driver, Connection Pool & Transactions | Standard JDBC connectivity (`java.sql.Driver`, `Connection`, `PreparedStatement`, `ResultSet`, `Statement`) with transaction support, auto-commit management, and connection release mechanics. |
| **Servlets & Web Integration** | HTTP Request Routing, Middleware Pipeline & REST | Custom lightweight HTTP servlet engine (`HttpServerManager`), URL pattern router and dispatcher (`DispatcherServlet`), filter chaining (`AuthenticationFilter`, `RoleAuthorizationFilter`), and RESTful controller endpoints. |

---

## Key Features

### 1. Role-Specific Workspaces
- **Administrator Dashboard**:
  - Global system health overview (Total Users, Active Projects, Total Tasks, Completed Tasks, Audit Event volume).
  - User Directory: Add new user, update account information, activate or deactivate accounts, and filter by role.
  - Security Audit Stream: View live chronological audit events with operator username, action, entity type, ID, details, and client IP address.
  - System Configuration Editor: Live key-value setting modification without application restarts.

- **Project Manager Dashboard**:
  - Project portfolio catalog with budget tracking, priority tags, manager assignment, and calendar scheduling.
  - Automated visual progress bars and completion ratios (`Completed / Total Tasks`).
  - Project creation, editing, deletion, and deliverable assignment to team members.
  - Project analytics with logged vs estimated hours breakdown.

- **Team Member Dashboard**:
  - "My Assigned Tasks" queue filtered to current user deliverables.
  - Interactive 4-column **Kanban Board** (`To Do`, `In Progress`, `Code Review`, `Completed`).
  - One-click task state progression (`Start Work` -> `Submit Review` -> `Done`).
  - Precision Work Time Logging modal with date, hours spent, and activity descriptions.
  - Task Detail and Discussion modal with live chronological comments and technical notes feed.

### 2. Automated Dynamic Progress Synchronization
Whenever any task transitions its status (e.g. from `IN_PROGRESS` to `COMPLETED`), the system automatically:
1. Validates the state transition according to the defined lifecycle state machine.
2. Dispatches an asynchronous `TaskStatusChangedEvent` through the `EventBus`.
3. Recalculates the parent project's total task count and completed task count.
4. Computes the updated completion percentage:  
   $$\text{Progress (\%)} = \left( \frac{\text{Completed Tasks}}{\text{Total Project Tasks}} \right) \times 100$$
5. Updates the database and triggers real-time visual progress bar updates on the frontend.
6. When all tasks reach `COMPLETED`, the project status automatically updates to `COMPLETED`.

### 3. Human-Crafted Corporate Design Tokens
- Strict adherence to professional engineering aesthetics:
  - Deep Navy (`#0F172A`) and Slate Blue (`#1E293B`) headers and sidebars.
  - Precision Blue (`#2563EB`) interaction accents.
  - Crisp Slate canvas (`#F8FAFC`) and neutral borders (`#E2E8F0`).
  - Standard system font stack (`-apple-system`, `Segoe UI`, `Roboto`, `Helvetica`).
  - **Zero AI traces**: Strictly no emojis, no excessive gradients, no exaggerated rounded pill cards, and no distracting animations.

---

## Pre-Configured Demo Accounts

For immediate evaluation, the database automatically seeds with realistic test accounts:

| Role | Email Address | Password | Profile Description |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin@pmtool.local` | `Admin@1234` | Sarah Jenkins &bull; Full administrative and audit control |
| **Project Manager** | `manager@pmtool.local` | `Manager@1234` | David Vance &bull; Lead Technical Project Manager |
| **Team Member 1** | `dev1@pmtool.local` | `Dev@1234` | Alex Rivera &bull; Senior Distributed Systems Engineer |
| **Team Member 2** | `dev2@pmtool.local` | `Dev@1234` | Elena Rostova &bull; Full Stack & QA Automation Engineer |

> **Quick Access:** The sign-in page includes one-click demo profile buttons that automatically pre-fill credentials and log in for rapid assessment.

---

## System Architecture

```
+-------------------------------------------------------------------------------+
|                       Presentation Layer (HTML5 / CSS / JS)                   |
|          Single-Page Application Shell (Admin / Manager / Member Views)       |
+---------------------------------------+---------------------------------------+
                                        | (HTTP / REST JSON)
                                        v
+-------------------------------------------------------------------------------+
|                        Servlet & Routing Infrastructure                       |
|   HttpServerManager  <-->  DispatcherServlet  <-->  FilterChain              |
|        |                                                    |                 |
|        +--> AuthenticationFilter                            +--> RoutePattern |
|        +--> RoleAuthorizationFilter                              (/api/...)   |
+---------------------------------------+---------------------------------------+
                                        |
                                        v
+-------------------------------------------------------------------------------+
|                          Core Business Service Layer                          |
|         AuthService  |  UserService  |  ProjectService  |  TaskService        |
+---------------------------------------+---------------------------------------+
                    |                                       |
                    v (Async Events)                        v (Data Access)
+---------------------------------------+   +-----------------------------------+
|         Concurrency & Events          |   |          Data Access (DAO)        |
|  AsyncWorkerPool (ThreadPoolExecutor) |   |  GenericDao<T, ID>                |
|  EventBus (Publish / Subscribe)       |   |  AbstractJdbcDao<T, ID>           |
|  SecurityContext (ThreadLocal<User>)  |   |  8 Domain Repositories            |
+---------------------------------------+   +-----------------+-----------------+
                                                              |
                                                              v
+-------------------------------------------------------------------------------+
|                        Persistence & JDBC Layer                               |
|        ThreadSafeConnectionPool  <-->  TransactionManager                     |
|                                  v                                            |
|                  Embedded Relational JDBC Engine                              |
|           EmbeddedDriver / EmbeddedConnection / PreparedStatement             |
+-------------------------------------------------------------------------------+
```

---

## Prerequisites

- **Java Development Kit (JDK)**: Java 21 or higher (Microsoft OpenJDK 21 or Oracle JDK 21).
- **Operating System**: Windows 10/11, Linux, or macOS.
- **External Database**: None required! The application includes a self-contained embedded relational SQL engine implementing standard JDBC interfaces.

---

## How to Run

### Method 1: Windows One-Click Batch Script
Double-click `run.bat` or execute in Command Prompt:
```cmd
run.bat
```

### Method 2: Windows PowerShell Script
Run the PowerShell script:
```powershell
.un.ps1
```

### Method 3: Manual Compilation & Execution
Compile and start the server manually using standard Java commands:
```powershell
# 1. Create target directory
mkdir target\classes -Force

# 2. Compile Java sources
Get-ChildItem -Path "src/main/java" -Filter "*.java" -Recurse | ForEach-Object { '"' + ($_.FullName -replace '\', '/') + '"' } | Set-Content sources.txt
javac -encoding UTF-8 -d target/classes @sources.txt
Remove-Item sources.txt

# 3. Copy resources
Copy-Item "src/main/resources/*" "target/classes/" -Force

# 4. Start Server
java -cp target/classes com.pmtool.AppServer
```

Once started, open your web browser and navigate to:
```
http://localhost:8080
```

---

## REST API Endpoint Reference

### Authentication Endpoints (`/api/auth`)
| Method | Endpoint | Description | Role Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticate credentials and obtain session token | Public |
| `POST` | `/api/auth/logout` | Invalidate active session | Authenticated |
| `GET` | `/api/auth/status` | Check session validity and retrieve current user | Public |
| `GET` | `/api/auth/demo-accounts` | Retrieve evaluation demo accounts | Public |

### User Endpoints (`/api/users`)
| Method | Endpoint | Description | Role Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/users/profile` | Retrieve profile of authenticated user | Authenticated |
| `PUT` | `/api/users/profile` | Update profile details | Authenticated |
| `GET` | `/api/users/assignable` | Retrieve active members available for task allocation | Authenticated |

### Project Endpoints (`/api/projects`)
| Method | Endpoint | Description | Role Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/projects` | List projects with search, status, and priority filters | Authenticated |
| `POST` | `/api/projects` | Create a new project portfolio item | Manager, Admin |
| `GET` | `/api/projects/{id}` | Retrieve project details and progress metrics | Authenticated |
| `PUT` | `/api/projects/{id}` | Update project details, schedule, or budget | Manager, Admin |
| `DELETE` | `/api/projects/{id}` | Delete project and cascade associated tasks | Manager, Admin |
| `GET` | `/api/projects/{id}/analytics` | Retrieve project performance and hour breakdown | Authenticated |

### Task Endpoints (`/api/tasks`)
| Method | Endpoint | Description | Role Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/tasks` | List tasks with multi-criteria filtering | Authenticated |
| `POST` | `/api/tasks` | Create a new task deliverable | Authenticated |
| `GET` | `/api/tasks/{id}` | Retrieve task detail record | Authenticated |
| `PUT` | `/api/tasks/{id}` | Update task title, description, assignee, priority | Authenticated |
| `DELETE` | `/api/tasks/{id}` | Delete task record | Manager, Admin |
| `POST` | `/api/tasks/{id}/status` | Transition task status (triggers auto progress sync) | Authenticated |
| `GET` | `/api/tasks/{id}/time-logs` | Retrieve chronological work hour logs | Authenticated |
| `POST` | `/api/tasks/{id}/time-logs` | Log hours worked with activity note | Authenticated |
| `GET` | `/api/tasks/{id}/comments` | Retrieve task discussion comments | Authenticated |
| `POST` | `/api/tasks/{id}/comments` | Post discussion comment or technical note | Authenticated |

### Administration Endpoints (`/api/admin`)
| Method | Endpoint | Description | Role Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/overview` | Retrieve system summary statistics | Admin |
| `GET` | `/api/admin/users` | List all system users with role/status filters | Admin |
| `POST` | `/api/admin/users` | Provision a new user account | Admin |
| `GET` | `/api/admin/users/{id}` | Retrieve detailed user record | Admin |
| `PUT` | `/api/admin/users/{id}` | Update user details or toggle active status | Admin |
| `GET` | `/api/admin/audit-logs` | Query chronological security audit trail | Admin |
| `GET` | `/api/admin/settings` | Retrieve system configuration properties | Admin |
| `PUT` | `/api/admin/settings` | Update system configuration property | Admin |

---

## Presentation Deliverable

A 13-slide executive presentation file is located in the repository under:
```
presentation/Online_Project_Management_Tool_Presentation.pptx
```
The presentation covers:
1. Executive Project Overview & Objectives
2. Problem Statement & Motivation
3. Proposed Solution & Core Features
4. Stakeholder Roles & Access Control Matrix
5. System Architecture & Layered Design
6. Relational Database Schema & Entity Relationships
7. Core Java & Object-Oriented Architecture
8. Collections Framework, Generics & Data Structures
9. Multithreading, Concurrency & Synchronization
10. Automated Progress Synchronization Engine
11. Security, Password Hashing & RBAC Enforcement
12. Enterprise User Interface & Dashboard Showcase
13. Conclusion, Deliverables & Architectural Summary

---

## Repository Structure

```
Online Project Management Tool/
├── pom.xml                                      # Maven project configuration
├── run.bat                                      # One-click Windows batch launcher
├── run.ps1                                      # One-click PowerShell launcher
├── README.md                                    # Comprehensive project documentation
├── presentation/
│   └── Online_Project_Management_Tool_Presentation.pptx # 13-slide presentation deck
├── src/
│   └── main/
│       ├── resources/
│       │   ├── application.properties           # Application configuration
│       │   └── schema.sql                       # Relational DDL schema script
│       ├── webapp/
│       │   ├── index.html                       # Single-page application shell
│       │   ├── css/
│       │   │   └── app.css                      # Corporate design tokens & styling
│       │   └── js/
│       │       ├── api.js                       # REST HTTP client wrapper
│       │       └── app.js                       # Dashboard & Kanban controller logic
│       └── java/com/pmtool/
│           ├── AppServer.java                   # Main HTTP application entry point
│           ├── DatabaseSeeder.java              # Initial realistic data seeder
│           ├── config/                          # Configuration loader
│           ├── db/                              # Connection pool, transactions, driver
│           │   ├── embedded/                    # Pure Java relational SQL engine
│           │   ├── pool/                        # Thread-safe connection pool
│           │   └── transaction/                 # Transaction manager
│           ├── model/                           # Domain entities & enums
│           ├── dao/                             # Generic DAO & JDBC implementations
│           ├── concurrency/                     # AsyncWorkerPool & EventBus
│           ├── security/                        # Password hashing & security context
│           ├── service/                         # Business logic services
│           └── web/                             # Lightweight HTTP server & controllers
│               ├── servlet/                     # DispatcherServlet & interfaces
│               ├── filter/                      # Authentication & RBAC filters
│               ├── controller/                  # REST API controllers
│               └── util/                        # JSON serializer/deserializer
```
