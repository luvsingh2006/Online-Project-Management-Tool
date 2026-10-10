package com.pmtool;

import com.pmtool.model.*;
import com.pmtool.service.ProjectService;
import com.pmtool.service.TaskService;
import com.pmtool.service.UserService;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Logger;

public class DatabaseSeeder {

    private static final Logger LOGGER = Logger.getLogger(DatabaseSeeder.class.getName());

    public static void seedIfEmpty(UserService userService, ProjectService projectService, TaskService taskService) {
        // Check project count rather than user count — this handles partial seeds
        // where users were created but the seeder aborted before creating projects.
        List<Project> existingProjects = projectService.listProjects(null, null, null, null);
        if (!existingProjects.isEmpty()) {
            LOGGER.info("Database already seeded (" + existingProjects.size() + " projects found). Skipping.");
            return;
        }

        LOGGER.info("No projects found. Seeding realistic engineering project data...");

        try {
            // 1. Seed System Users across all 3 roles
            // Each createUser call is wrapped individually so that a partial seed from a
            // previous run (where some users already exist) can recover gracefully.
            User admin = findOrCreateUser(userService,
                    "Sarah Jenkins", "admin@pmtool.local", Role.ADMIN,
                    "Engineering Operations", "Director of Infrastructure",
                    "Admin@1234", 1L, null);

            User manager = findOrCreateUser(userService,
                    "David Vance", "manager@pmtool.local", Role.PROJECT_MANAGER,
                    "Platform Delivery", "Lead Technical Project Manager",
                    "Manager@1234", admin.getId(), admin.getId());

            User dev1 = findOrCreateUser(userService,
                    "Alex Rivera", "dev1@pmtool.local", Role.TEAM_MEMBER,
                    "Backend Engineering", "Senior Distributed Systems Engineer",
                    "Dev@1234", admin.getId(), admin.getId());

            User dev2 = findOrCreateUser(userService,
                    "Elena Rostova", "dev2@pmtool.local", Role.TEAM_MEMBER,
                    "Frontend & Quality", "Full Stack & QA Automation Engineer",
                    "Dev@1234", admin.getId(), admin.getId());

            LOGGER.info("User profiles ready (Admin, Manager, 2 Team Members).");

            Instant now = Instant.now();

            // 2. Seed Realistic Engineering Projects
            Project proj1 = new Project();
            proj1.setCode("PRJ-101");
            proj1.setTitle("Cloud Infrastructure Modernization");
            proj1.setDescription("Migrate legacy bare-metal microservices to a containerized Kubernetes cluster with automated CI/CD.");
            proj1.setManagerId(manager.getId());
            proj1.setStatus(ProjectStatus.IN_PROGRESS);
            proj1.setPriority(Priority.HIGH);
            proj1.setBudget(65000.0);
            proj1.setStartDate(Timestamp.from(now.minus(14, ChronoUnit.DAYS)));
            proj1.setDueDate(Timestamp.from(now.plus(45, ChronoUnit.DAYS)));
            proj1 = projectService.createProject(proj1, manager.getId());

            Project proj2 = new Project();
            proj2.setCode("PRJ-102");
            proj2.setTitle("Mobile Banking API Gateway");
            proj2.setDescription("Build an enterprise OAuth2 API gateway with distributed token caching, rate-limiting, and PCI-DSS compliance.");
            proj2.setManagerId(manager.getId());
            proj2.setStatus(ProjectStatus.IN_PROGRESS);
            proj2.setPriority(Priority.URGENT);
            proj2.setBudget(48000.0);
            proj2.setStartDate(Timestamp.from(now.minus(7, ChronoUnit.DAYS)));
            proj2.setDueDate(Timestamp.from(now.plus(30, ChronoUnit.DAYS)));
            proj2 = projectService.createProject(proj2, manager.getId());

            Project proj3 = new Project();
            proj3.setCode("PRJ-103");
            proj3.setTitle("Customer Identity & Access Migration");
            proj3.setDescription("Single sign-on consolidation across enterprise platforms with PBKDF2 credential migration and MFA support.");
            proj3.setManagerId(manager.getId());
            proj3.setStatus(ProjectStatus.PLANNING);
            proj3.setPriority(Priority.MEDIUM);
            proj3.setBudget(32000.0);
            proj3.setStartDate(Timestamp.from(now));
            proj3.setDueDate(Timestamp.from(now.plus(60, ChronoUnit.DAYS)));
            proj3 = projectService.createProject(proj3, manager.getId());

            LOGGER.info("Seeded 3 realistic engineering projects.");

            // 3. Seed Realistic Tasks
            // Project 1 Tasks
            Task t1 = createTask(taskService, proj1.getId(), dev1.getId(), manager.getId(),
                    "Configure Kubernetes deployment manifests and Helm charts",
                    "Author modular Helm charts for api-gateway, auth-service, and user-service with environment-specific values.",
                    TaskStatus.IN_PROGRESS, Priority.HIGH, 12.0, now.plus(5, ChronoUnit.DAYS));

            Task t2 = createTask(taskService, proj1.getId(), dev2.getId(), manager.getId(),
                    "Design database failover clustering and replication tests",
                    "Configure standby read replicas and verify automated failover behavior under simulated network partitions.",
                    TaskStatus.IN_REVIEW, Priority.HIGH, 8.0, now.plus(3, ChronoUnit.DAYS));

            Task t3 = createTask(taskService, proj1.getId(), dev1.getId(), manager.getId(),
                    "Setup centralized Prometheus & Grafana telemetry dashboards",
                    "Deploy Prometheus node exporters, cAdvisor metrics collectors, and Grafana cluster health dashboards.",
                    TaskStatus.COMPLETED, Priority.MEDIUM, 6.0, now.minus(2, ChronoUnit.DAYS));

            Task t4 = createTask(taskService, proj1.getId(), dev2.getId(), manager.getId(),
                    "Perform disaster recovery simulation in staging environment",
                    "Execute cold restore drill from nightly encrypted snapshots and measure recovery time objective (RTO).",
                    TaskStatus.TODO, Priority.MEDIUM, 10.0, now.plus(15, ChronoUnit.DAYS));

            // Project 2 Tasks
            Task t5 = createTask(taskService, proj2.getId(), dev1.getId(), manager.getId(),
                    "Implement JWT token validation filter and rate limiter",
                    "Create high-throughput token signature validator and IP-based token bucket rate limiter filter.",
                    TaskStatus.COMPLETED, Priority.URGENT, 14.0, now.minus(1, ChronoUnit.DAYS));

            Task t6 = createTask(taskService, proj2.getId(), dev2.getId(), manager.getId(),
                    "Create API contract test suite using Postman / REST assertions",
                    "Write automated test assertions validating JSON schema contracts, HTTP headers, and error codes.",
                    TaskStatus.IN_PROGRESS, Priority.HIGH, 8.0, now.plus(4, ChronoUnit.DAYS));

            Task t7 = createTask(taskService, proj2.getId(), dev1.getId(), manager.getId(),
                    "Implement webhook notifications for payment gateway transactions",
                    "Deliver reliable HMAC-signed webhooks with exponential backoff retry queue for partner bank callbacks.",
                    TaskStatus.TODO, Priority.HIGH, 10.0, now.plus(8, ChronoUnit.DAYS));

            Task t8 = createTask(taskService, proj2.getId(), dev2.getId(), manager.getId(),
                    "Conduct security penetration audit on OAuth2 refresh flow",
                    "Test refresh token replay attacks, revocation latency, and concurrent token swap vulnerability.",
                    TaskStatus.IN_REVIEW, Priority.URGENT, 6.0, now.plus(2, ChronoUnit.DAYS));

            // Project 3 Tasks
            Task t9 = createTask(taskService, proj3.getId(), dev1.getId(), manager.getId(),
                    "Audit legacy user permission roles and map to RBAC model",
                    "Extract user table from legacy system, map outdated permission masks to unified Role enum.",
                    TaskStatus.TODO, Priority.MEDIUM, 6.0, now.plus(12, ChronoUnit.DAYS));

            Task t10 = createTask(taskService, proj3.getId(), dev2.getId(), manager.getId(),
                    "Configure LDAP and SSO federation bridge for internal staff",
                    "Setup SAML 2.0 / OIDC federation bridge with corporate Active Directory directory.",
                    TaskStatus.TODO, Priority.HIGH, 12.0, now.plus(20, ChronoUnit.DAYS));

            Task t11 = createTask(taskService, proj3.getId(), dev1.getId(), manager.getId(),
                    "Define automated password expiry and lockout security policies",
                    "Implement brute force counter locking accounts after 5 failed password attempts.",
                    TaskStatus.COMPLETED, Priority.MEDIUM, 4.0, now.minus(1, ChronoUnit.DAYS));

            LOGGER.info("Seeded 11 realistic tasks across multiple workflows.");

            // 4. Seed Time Logs
            taskService.logWorkHours(t3.getId(), dev1.getId(), 6.0, "Deployed Prometheus server and configured Grafana dashboards with node exporters.");
            taskService.logWorkHours(t5.getId(), dev1.getId(), 8.0, "Implemented JWT signature verification with cryptographic key caching.");
            taskService.logWorkHours(t5.getId(), dev1.getId(), 6.0, "Added unit test coverage for token expiry edge cases and invalid signatures.");
            taskService.logWorkHours(t2.getId(), dev2.getId(), 5.5, "Provisioned secondary replica in staging cluster and verified replication delay.");
            taskService.logWorkHours(t6.getId(), dev2.getId(), 4.0, "Created automated test scripts for authentication and project REST endpoints.");
            taskService.logWorkHours(t1.getId(), dev1.getId(), 4.5, "Authored Helm chart templates for service deployments and cluster ingresses.");

            // 5. Seed Discussion Comments
            taskService.addComment(t1.getId(), manager.getId(), "Please ensure memory and CPU resource requests are defined explicitly in Helm values.");
            taskService.addComment(t1.getId(), dev1.getId(), "Added resource requests: 250m CPU and 512Mi memory per pod, tested in staging.");
            taskService.addComment(t5.getId(), manager.getId(), "Validated token verification throughput; performance meets 2500 req/sec benchmark.");
            taskService.addComment(t2.getId(), dev2.getId(), "Failover completed in under 2.4 seconds with zero data loss observed.");

            // 6. Seed System Settings
            userService.updateSystemSetting("app.name", "ProjectFlow Enterprise", admin.getId());
            userService.updateSystemSetting("app.environment", "Production", admin.getId());
            userService.updateSystemSetting("auth.session.timeout.minutes", "120", admin.getId());
            userService.updateSystemSetting("task.max.estimated.hours", "80", admin.getId());

            LOGGER.info("Initial data seeding completed successfully! All entities ready for evaluation.");

        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Data seeding encountered error: " + e.getMessage(), e);
        }
    }

    private static Task createTask(TaskService taskService, Long projectId, Long assigneeId, Long creatorId,
                                  String title, String desc, TaskStatus status, Priority priority,
                                  double estHours, Instant due) {
        Task t = new Task();
        t.setProjectId(projectId);
        t.setAssigneeId(assigneeId);
        t.setCreatorId(creatorId);
        t.setTitle(title);
        t.setDescription(desc);
        t.setStatus(status);
        t.setPriority(priority);
        t.setEstimatedHours(estHours);
        t.setDueDate(Timestamp.from(due));
        return taskService.createTask(t, creatorId);
    }

    /**
     * Creates a user with the given details if no user with that email exists yet.
     * If the email is already taken (e.g. from a prior partial seed), the existing
     * user record is fetched from the list and returned instead of throwing.
     */
    private static User findOrCreateUser(UserService userService,
                                         String fullName, String email, Role role,
                                         String department, String jobTitle,
                                         String password, Long createdById, Long adminId) {
        try {
            User u = new User();
            u.setFullName(fullName);
            u.setEmail(email);
            u.setRole(role);
            u.setStatus(UserStatus.ACTIVE);
            u.setDepartment(department);
            u.setJobTitle(jobTitle);
            return userService.createUser(u, password, createdById);
        } catch (Exception e) {
            // If creation failed because the email already exists, look up the existing record.
            LOGGER.info("User " + email + " already exists, fetching existing record.");
            return userService.listUsers(null, null).stream()
                    .filter(u -> email.equals(u.getEmail()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Cannot find or create user: " + email, e));
        }
    }
}
