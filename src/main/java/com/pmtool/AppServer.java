package com.pmtool;

import com.pmtool.concurrency.AsyncWorkerPool;
import com.pmtool.concurrency.EventBus;
import com.pmtool.concurrency.event.AuditLogEvent;
import com.pmtool.concurrency.event.TaskStatusChangedEvent;
import com.pmtool.config.AppConfig;
import com.pmtool.dao.impl.*;
import com.pmtool.db.DatabaseManager;
import com.pmtool.db.DatabaseMigrationRunner;
import com.pmtool.model.AuditLog;
import com.pmtool.service.*;
import com.pmtool.service.impl.*;
import com.pmtool.web.HttpServerManager;
import com.pmtool.web.controller.*;
import com.pmtool.web.filter.AuthenticationFilter;
import com.pmtool.web.filter.RoleAuthorizationFilter;
import com.pmtool.web.servlet.DispatcherServlet;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppServer {

    private static final Logger LOGGER = Logger.getLogger(AppServer.class.getName());

    public static void main(String[] args) {
        try {
            LOGGER.info("Starting Online Project Management Tool Server...");

            // 1. Initialize configuration and database
            DatabaseManager dbManager = DatabaseManager.getInstance();
            DatabaseMigrationRunner.runMigrations(dbManager);

            // 2. Initialize DAOs
            UserDaoImpl userDao = new UserDaoImpl(dbManager);
            ProjectDaoImpl projectDao = new ProjectDaoImpl(dbManager);
            MilestoneDaoImpl milestoneDao = new MilestoneDaoImpl(dbManager);
            TaskDaoImpl taskDao = new TaskDaoImpl(dbManager);
            TaskTimeLogDaoImpl timeLogDao = new TaskTimeLogDaoImpl(dbManager);
            CommentDaoImpl commentDao = new CommentDaoImpl(dbManager);
            AuditLogDaoImpl auditLogDao = new AuditLogDaoImpl(dbManager);
            SystemSettingDaoImpl settingDao = new SystemSettingDaoImpl(dbManager);

            // 3. Initialize Concurrency & Event Bus
            AsyncWorkerPool asyncWorkerPool = AsyncWorkerPool.getInstance();
            EventBus eventBus = EventBus.getInstance();

            // Register asynchronous audit log persistence listener
            eventBus.subscribe(AuditLogEvent.class, event -> {
                asyncWorkerPool.execute(() -> {
                    AuditLog log = new AuditLog(null, event.getUserId(), event.getAction(),
                            event.getEntityType(), event.getEntityId(), event.getDetails(), event.getIpAddress());
                    auditLogDao.save(log);
                });
            });

            // Register task status event listener for notification and analytics
            eventBus.subscribe(TaskStatusChangedEvent.class, event -> {
                asyncWorkerPool.execute(() -> {
                    LOGGER.info(String.format("EventBus: Task #%d status updated: %s -> %s",
                            event.getTaskId(), event.getOldStatus(), event.getNewStatus()));
                });
            });

            // 4. Initialize Core Services
            UserService userService = new UserServiceImpl(userDao, projectDao, taskDao, auditLogDao, settingDao, eventBus);
            AuthService authService = new AuthServiceImpl(userDao, eventBus);
            ProjectService projectService = new ProjectServiceImpl(projectDao, milestoneDao, taskDao, userDao, eventBus);
            TaskService taskService = new TaskServiceImpl(taskDao, projectDao, userDao, timeLogDao, commentDao, eventBus);

            // Optional: call seeder via reflection if available on classpath
            try {
                Class<?> seederClass = Class.forName("com.pmtool.DatabaseSeeder");
                java.lang.reflect.Method seedMethod = seederClass.getMethod("seedIfEmpty",
                        UserService.class, ProjectService.class, TaskService.class);
                seedMethod.invoke(null, userService, projectService, taskService);
            } catch (ClassNotFoundException ignored) {
                // Seeder not yet compiled; will run in step 30
            }

            // 5. Configure Web Dispatcher and Filters
            DispatcherServlet dispatcher = new DispatcherServlet();
            dispatcher.addFilter(new AuthenticationFilter(authService));
            dispatcher.addFilter(new RoleAuthorizationFilter());

            // 6. Register REST Controllers
            dispatcher.registerServlet("/api/auth/login", new AuthServlet(authService));
            dispatcher.registerServlet("/api/auth/logout", new AuthServlet(authService));
            dispatcher.registerServlet("/api/auth/status", new AuthServlet(authService));
            dispatcher.registerServlet("/api/auth/demo-accounts", new AuthServlet(authService));
            dispatcher.registerServlet("/api/auth/change-password", new AuthServlet(authService));

            dispatcher.registerServlet("/api/users/profile", new UserProfileServlet(userService));
            dispatcher.registerServlet("/api/users/assignable", new AssignableUserServlet(userService));

            dispatcher.registerServlet("/api/projects", new ProjectServlet(projectService));
            dispatcher.registerServlet("/api/projects/{id}", new ProjectDetailServlet(projectService));
            dispatcher.registerServlet("/api/projects/{id}/analytics", new ProjectAnalyticsServlet(projectService));

            dispatcher.registerServlet("/api/tasks", new TaskServlet(taskService));
            dispatcher.registerServlet("/api/tasks/{id}", new TaskDetailServlet(taskService));
            dispatcher.registerServlet("/api/tasks/{id}/status", new TaskStatusServlet(taskService));
            dispatcher.registerServlet("/api/tasks/{id}/time-logs", new TaskTimeLogServlet(taskService));
            dispatcher.registerServlet("/api/tasks/{id}/comments", new TaskCommentServlet(taskService));

            dispatcher.registerServlet("/api/admin/users", new AdminUserServlet(userService));
            dispatcher.registerServlet("/api/admin/users/{id}", new AdminUserDetailServlet(userService));
            dispatcher.registerServlet("/api/admin/audit-logs", new AdminAuditLogServlet(userService));
            dispatcher.registerServlet("/api/admin/settings", new AdminSettingsServlet(userService));
            dispatcher.registerServlet("/api/admin/overview", new SystemOverviewServlet(userService));

            // 7. Start HTTP Server
            HttpServerManager serverManager = new HttpServerManager(dispatcher);
            serverManager.start();

            // 8. Graceful Shutdown Hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Initiating graceful shutdown sequence...");
                serverManager.stop();
                asyncWorkerPool.shutdown();
                dbManager.shutdown();
                LOGGER.info("Server and database resources stopped cleanly.");
            }));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal error while initializing AppServer", e);
            System.exit(1);
        }
    }
}
