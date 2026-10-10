/**
 * Online Project Management Tool - Client Application Controller
 * Handles role-based views, dynamic task transitions, progress calculations,
 * and responsive UI modals.
 */

let currentUser = null;
let currentView = "dashboard";
let currentTaskViewMode = "kanban";
let currentActiveTaskId = null;
let cachedProjects = [];
let cachedUsers = [];
let searchDebounceTimeout = null;

// Initialize on DOM Ready
document.addEventListener("DOMContentLoaded", () => {
    initApp();
});

window.addEventListener("pmtool:unauthorized", () => {
    showLoginView();
    showToast("Session expired. Please sign in.", "warning");
});

async function initApp() {
    setupEventListeners();
    const token = Api.getToken();
    if (token) {
        try {
            const status = await Api.auth.getStatus();
            if (status.authenticated && status.user) {
                currentUser = status.user;
                showMainLayout();
                return;
            }
        } catch (e) {
            console.warn("Auto-login check failed:", e);
        }
    }
    showLoginView();
}

function setupEventListeners() {
    const loginForm = document.getElementById("login-form");
    if (loginForm) {
        loginForm.addEventListener("submit", handleLoginSubmit);
    }
}

// -------------------------------------------------------------------------
// Authentication Handlers
// -------------------------------------------------------------------------
async function handleLoginSubmit(e) {
    e.preventDefault();
    const email = document.getElementById("login-email").value.trim();
    const password = document.getElementById("login-password").value;
    const btn = document.getElementById("btn-login-submit");

    if (!email || !password) {
        showToast("Please enter email and password.", "danger");
        return;
    }

    try {
        btn.disabled = true;
        btn.textContent = "Signing In...";
        const res = await Api.auth.login(email, password);
        Api.setToken(res.token || res.sessionId);
        currentUser = res.user;
        showToast("Signed in as " + currentUser.fullName, "success");
        showMainLayout();
    } catch (err) {
        showToast(err.message || "Invalid credentials", "danger");
    } finally {
        btn.disabled = false;
        btn.textContent = "Sign In";
    }
}

function prefillLogin(email, password) {
    document.getElementById("login-email").value = email;
    document.getElementById("login-password").value = password;
    document.getElementById("btn-login-submit").click();
}

async function logout() {
    try {
        await Api.auth.logout();
    } catch (ignored) {}
    Api.setToken(null);
    currentUser = null;
    showLoginView();
    showToast("Signed out successfully.", "success");
}

function showLoginView() {
    document.getElementById("app-layout").classList.add("hidden");
    document.getElementById("view-login").classList.remove("hidden");
}

function showMainLayout() {
    document.getElementById("view-login").classList.add("hidden");
    document.getElementById("app-layout").classList.remove("hidden");

    // Populate user profile info in sidebar
    document.getElementById("current-user-name").textContent = currentUser.fullName;
    document.getElementById("current-user-role").textContent = currentUser.roleName || currentUser.role;
    document.getElementById("user-avatar").textContent = getInitials(currentUser.fullName);

    // Toggle role-based sidebar items
    const adminNav = document.getElementById("admin-nav-section");
    if (currentUser.role === "ADMIN") {
        adminNav.classList.remove("hidden");
    } else {
        adminNav.classList.add("hidden");
    }

    // Toggle header action buttons based on permissions
    const btnNewProject = document.getElementById("btn-header-new-project");
    const btnNewTask = document.getElementById("btn-header-new-task");

    if (currentUser.role === "ADMIN" || currentUser.role === "PROJECT_MANAGER") {
        btnNewProject.classList.remove("hidden");
        btnNewTask.classList.remove("hidden");
    } else {
        btnNewProject.classList.add("hidden");
        btnNewTask.classList.remove("hidden");
    }

    loadInitialData();
    switchNav("dashboard");
}

function getInitials(name) {
    if (!name) return "U";
    const parts = name.trim().split(" ");
    if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
    return name.substring(0, 2).toUpperCase();
}

// -------------------------------------------------------------------------
// Navigation & View Routing
// -------------------------------------------------------------------------
function switchNav(viewName) {
    currentView = viewName;

    // Update active nav link
    document.querySelectorAll(".sidebar-nav .nav-item").forEach(item => {
        if (item.getAttribute("data-view") === viewName) {
            item.classList.add("active");
        } else {
            item.classList.remove("active");
        }
    });

    // Hide all view sections
    document.getElementById("section-dashboard").classList.add("hidden");
    document.getElementById("section-projects").classList.add("hidden");
    document.getElementById("section-tasks").classList.add("hidden");
    document.getElementById("section-admin-users").classList.add("hidden");
    document.getElementById("section-admin-audit").classList.add("hidden");
    document.getElementById("section-admin-settings").classList.add("hidden");

    const headerTitle = document.getElementById("view-header-title");

    switch (viewName) {
        case "dashboard":
            headerTitle.textContent = "Dashboard Overview";
            document.getElementById("section-dashboard").classList.remove("hidden");
            loadDashboard();
            break;
        case "projects":
            headerTitle.textContent = "Projects Directory";
            document.getElementById("section-projects").classList.remove("hidden");
            loadProjects();
            break;
        case "tasks":
            headerTitle.textContent = "Tasks & Work Queue";
            document.getElementById("section-tasks").classList.remove("hidden");
            loadTasks();
            break;
        case "admin-users":
            headerTitle.textContent = "System User Directory";
            document.getElementById("section-admin-users").classList.remove("hidden");
            loadAdminUsers();
            break;
        case "admin-audit":
            headerTitle.textContent = "System Security & Audit Trail";
            document.getElementById("section-admin-audit").classList.remove("hidden");
            loadAdminAuditLogs();
            break;
        case "admin-settings":
            headerTitle.textContent = "System Configuration";
            document.getElementById("section-admin-settings").classList.remove("hidden");
            loadAdminSettings();
            break;
    }
}

// -------------------------------------------------------------------------
// Data Initializers & Pre-loaders
// -------------------------------------------------------------------------
async function loadInitialData() {
    try {
        const [projects, users] = await Promise.all([
            Api.projects.list(),
            Api.user.getAssignable()
        ]);
        cachedProjects = projects;
        cachedUsers = users;

        document.getElementById("nav-count-projects").textContent = projects.length;

        // Populate task filters dropdowns
        populateDropdown("task-project-filter", projects, "id", "title", "All Projects");
        populateDropdown("task-assignee-filter", users, "id", "fullName", "All Assignees");

        // Populate modal selects
        populateDropdown("task-project-select", projects, "id", "title", "Select Project");
        populateDropdown("task-assignee-select", users, "id", "fullName", "Unassigned");
        populateDropdown("project-manager-select", users, "id", "fullName", "Select Manager");
    } catch (err) {
        console.error("Initial data load error:", err);
    }
}

function populateDropdown(elemId, items, valKey, textKey, defaultLabel) {
    const el = document.getElementById(elemId);
    if (!el) return;
    el.innerHTML = "";
    if (defaultLabel) {
        const opt = document.createElement("option");
        opt.value = elemId.includes("filter") ? "ALL" : "";
        opt.textContent = defaultLabel;
        el.appendChild(opt);
    }
    items.forEach(it => {
        const opt = document.createElement("option");
        opt.value = it[valKey];
        opt.textContent = it[textKey];
        el.appendChild(opt);
    });
}

// -------------------------------------------------------------------------
// Dashboard View Controller
// -------------------------------------------------------------------------
async function loadDashboard() {
    const metricsContainer = document.getElementById("dashboard-metrics");
    const widgetsContainer = document.getElementById("dashboard-role-widgets");
    metricsContainer.innerHTML = "<p class='text-muted'>Calculating metrics...</p>";
    widgetsContainer.innerHTML = "";

    try {
        if (currentUser.role === "ADMIN") {
            const overview = await Api.admin.getOverview();
            metricsContainer.innerHTML = `
                <div class="metric-card">
                    <div class="metric-label">Registered Users</div>
                    <div class="metric-value">${overview.totalUsers || 0}</div>
                    <div class="metric-subtext">${overview.activeUsers || 0} active accounts</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Active Projects</div>
                    <div class="metric-value">${overview.activeProjects || 0}</div>
                    <div class="metric-subtext">${overview.totalProjects || 0} total registered</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Total Tasks</div>
                    <div class="metric-value">${overview.totalTasks || 0}</div>
                    <div class="metric-subtext">${overview.completedTasks || 0} completed</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Audit Events</div>
                    <div class="metric-value">${overview.auditLogEvents || 0}</div>
                    <div class="metric-subtext">Security & event log</div>
                </div>
            `;

            const auditLogs = await Api.admin.getAuditLogs(5);
            widgetsContainer.innerHTML = `
                <div class="table-container" style="padding: 20px;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                        <h3 style="font-size: 15px; font-weight: 700;">Recent System Audit Log Stream</h3>
                        <button class="btn btn-secondary btn-sm" onclick="switchNav('admin-audit')">View Full Log</button>
                    </div>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Timestamp</th>
                                <th>User</th>
                                <th>Action</th>
                                <th>Entity</th>
                                <th>Details</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${auditLogs.map(l => `
                                <tr>
                                    <td class="font-mono" style="font-size: 12px;">${formatDate(l.createdAt)}</td>
                                    <td><strong>${escapeHtml(l.userName)}</strong></td>
                                    <td><span class="badge badge-admin">${escapeHtml(l.action)}</span></td>
                                    <td>${escapeHtml(l.entityType)} #${escapeHtml(l.entityId || "-")}</td>
                                    <td class="text-muted">${escapeHtml(l.details || "-")}</td>
                                </tr>
                            `).join("")}
                        </tbody>
                    </table>
                </div>
            `;
        } else if (currentUser.role === "PROJECT_MANAGER") {
            const projects = await Api.projects.list({ managerId: currentUser.id });
            const totalTasks = projects.reduce((acc, p) => acc + (p.taskCount || 0), 0);
            const compTasks = projects.reduce((acc, p) => acc + (p.completedTaskCount || 0), 0);
            const overallProgress = totalTasks > 0 ? Math.round((compTasks / totalTasks) * 100) : 0;

            metricsContainer.innerHTML = `
                <div class="metric-card">
                    <div class="metric-label">Managed Projects</div>
                    <div class="metric-value">${projects.length}</div>
                    <div class="metric-subtext">Assigned to you</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Total Tasks</div>
                    <div class="metric-value">${totalTasks}</div>
                    <div class="metric-subtext">${compTasks} completed</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Overall Completion</div>
                    <div class="metric-value">${overallProgress}%</div>
                    <div class="metric-subtext">Across managed projects</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Team Velocity</div>
                    <div class="metric-value">${compTasks}/${totalTasks}</div>
                    <div class="metric-subtext">Deliverables shipped</div>
                </div>
            `;

            widgetsContainer.innerHTML = `
                <div class="table-container" style="padding: 20px;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                        <h3 style="font-size: 15px; font-weight: 700;">Managed Projects Progress Status</h3>
                        <button class="btn btn-primary btn-sm" onclick="openNewProjectModal()">+ New Project</button>
                    </div>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Code</th>
                                <th>Project</th>
                                <th>Status</th>
                                <th>Progress Bar</th>
                                <th>Ratio</th>
                                <th>Due Date</th>
                                <th class="text-right">Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${projects.map(p => `
                                <tr>
                                    <td class="font-mono"><strong>${escapeHtml(p.code)}</strong></td>
                                    <td><strong>${escapeHtml(p.title)}</strong></td>
                                    <td>${renderProjectStatusBadge(p.status)}</td>
                                    <td style="min-width: 140px;">
                                        <div class="progress-container">
                                            <div class="progress-bar ${p.progressPercentage === 100 ? 'progress-bar-success' : ''}" style="width: ${p.progressPercentage}%;"></div>
                                        </div>
                                    </td>
                                    <td><strong>${p.progressPercentage}%</strong> <span class="text-muted">(${p.completedTaskCount}/${p.taskCount})</span></td>
                                    <td>${formatDateOnly(p.dueDate)}</td>
                                    <td class="text-right">
                                        <button class="btn btn-secondary btn-sm" onclick="viewProjectTasks(${p.id})">Tasks</button>
                                    </td>
                                </tr>
                            `).join("")}
                        </tbody>
                    </table>
                </div>
            `;
        } else {
            // TEAM_MEMBER Dashboard
            const myTasks = await Api.tasks.list({ myTasksOnly: "true" });
            const inProg = myTasks.filter(t => t.status === "IN_PROGRESS").length;
            const inRev = myTasks.filter(t => t.status === "IN_REVIEW").length;
            const comp = myTasks.filter(t => t.status === "COMPLETED").length;
            const totalLogged = myTasks.reduce((acc, t) => acc + (t.loggedHours || 0), 0);

            metricsContainer.innerHTML = `
                <div class="metric-card">
                    <div class="metric-label">Assigned Work Items</div>
                    <div class="metric-value">${myTasks.length}</div>
                    <div class="metric-subtext">Active tasks allocated</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">In Progress</div>
                    <div class="metric-value">${inProg}</div>
                    <div class="metric-subtext">Currently executing</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">In Code Review</div>
                    <div class="metric-value">${inRev}</div>
                    <div class="metric-subtext">Awaiting sign-off</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Logged Hours</div>
                    <div class="metric-value">${totalLogged.toFixed(1)}h</div>
                    <div class="metric-subtext">Effort recorded</div>
                </div>
            `;

            widgetsContainer.innerHTML = `
                <div class="table-container" style="padding: 20px;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                        <h3 style="font-size: 15px; font-weight: 700;">My Assigned Tasks</h3>
                        <button class="btn btn-primary btn-sm" onclick="switchNav('tasks')">Open Kanban Board</button>
                    </div>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Code</th>
                                <th>Task Title</th>
                                <th>Project</th>
                                <th>Status</th>
                                <th>Priority</th>
                                <th>Logged / Est</th>
                                <th class="text-right">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${myTasks.length === 0 ? `<tr><td colspan="7" class="text-center text-muted">No tasks currently assigned to you.</td></tr>` :
                            myTasks.map(t => `
                                <tr>
                                    <td class="font-mono"><strong>${escapeHtml(t.code)}</strong></td>
                                    <td><a href="javascript:void(0)" onclick="openTaskDetailModal(${t.id})" style="font-weight: 600; text-decoration: none; color: var(--primary);">${escapeHtml(t.title)}</a></td>
                                    <td class="text-muted">${escapeHtml(t.projectName)}</td>
                                    <td>${renderTaskStatusBadge(t.status, t.statusLabel)}</td>
                                    <td>${renderPriorityBadge(t.priority, t.priorityLabel)}</td>
                                    <td>${t.loggedHours}h / ${t.estimatedHours}h</td>
                                    <td class="text-right">
                                        <button class="btn btn-secondary btn-sm" onclick="openLogHoursModal(${t.id})">Log Hours</button>
                                        <button class="btn btn-secondary btn-sm" onclick="openTaskDetailModal(${t.id})">Details</button>
                                    </td>
                                </tr>
                            `).join("")}
                        </tbody>
                    </table>
                </div>
            `;
        }
    } catch (err) {
        metricsContainer.innerHTML = `<p class="text-muted" style="color: var(--danger);">Failed to load dashboard: ${err.message}</p>`;
    }
}

// -------------------------------------------------------------------------
// Projects View Controller
// -------------------------------------------------------------------------
async function loadProjects() {
    const tbody = document.getElementById("projects-table-body");
    tbody.innerHTML = `<tr><td colspan="9" class="text-center text-muted">Loading projects...</td></tr>`;

    const search = document.getElementById("project-search-input").value.trim();
    const status = document.getElementById("project-status-filter").value;
    const priority = document.getElementById("project-priority-filter").value;

    const params = {};
    if (search) params.search = search;
    if (status !== "ALL") params.status = status;
    if (priority !== "ALL") params.priority = priority;

    try {
        const projects = await Api.projects.list(params);
        document.getElementById("nav-count-projects").textContent = projects.length;

        if (projects.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" class="text-center text-muted">No projects found matching the criteria.</td></tr>`;
            return;
        }

        tbody.innerHTML = projects.map(p => `
            <tr>
                <td class="font-mono"><strong>${escapeHtml(p.code)}</strong></td>
                <td>
                    <div style="font-weight: 600;">${escapeHtml(p.title)}</div>
                    <div class="text-muted" style="font-size: 11px;">${escapeHtml(p.description || "No description")}</div>
                </td>
                <td>${escapeHtml(p.managerName || "Unassigned")}</td>
                <td>${renderProjectStatusBadge(p.status)}</td>
                <td>${renderPriorityBadge(p.priority, p.priority)}</td>
                <td style="min-width: 140px;">
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <div class="progress-container" style="flex: 1;">
                            <div class="progress-bar ${p.progressPercentage === 100 ? 'progress-bar-success' : ''}" style="width: ${p.progressPercentage}%;"></div>
                        </div>
                        <span style="font-weight: 700; font-size: 12px;">${p.progressPercentage}%</span>
                    </div>
                </td>
                <td><strong>${p.completedTaskCount}</strong> / ${p.taskCount}</td>
                <td>${formatDateOnly(p.dueDate)}</td>
                <td class="text-right">
                    <button class="btn btn-secondary btn-sm" onclick="viewProjectTasks(${p.id})">Tasks</button>
                    ${currentUser.role !== "TEAM_MEMBER" ? `
                        <button class="btn btn-secondary btn-sm" onclick="openEditProjectModal(${p.id})">Edit</button>
                        <button class="btn btn-danger btn-sm" onclick="deleteProject(${p.id})">Del</button>
                    ` : ""}
                </td>
            </tr>
        `).join("");
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center" style="color: var(--danger);">Failed to load projects: ${err.message}</td></tr>`;
    }
}

function debounceProjectSearch() {
    clearTimeout(searchDebounceTimeout);
    searchDebounceTimeout = setTimeout(loadProjects, 300);
}

function viewProjectTasks(projectId) {
    switchNav("tasks");
    const projFilter = document.getElementById("task-project-filter");
    if (projFilter) {
        projFilter.value = projectId;
        loadTasks();
    }
}

// -------------------------------------------------------------------------
// Tasks & Kanban View Controller
// -------------------------------------------------------------------------
async function loadTasks() {
    const search = document.getElementById("task-search-input").value.trim();
    const projectId = document.getElementById("task-project-filter").value;
    const assigneeId = document.getElementById("task-assignee-filter").value;
    const priority = document.getElementById("task-priority-filter").value;

    const params = {};
    if (search) params.search = search;
    if (projectId !== "ALL") params.projectId = projectId;
    if (assigneeId !== "ALL") params.assigneeId = assigneeId;
    if (priority !== "ALL") params.priority = priority;

    try {
        const tasks = await Api.tasks.list(params);
        document.getElementById("nav-count-tasks").textContent = tasks.length;

        renderKanbanBoard(tasks);
        renderTasksTable(tasks);
    } catch (err) {
        showToast("Failed to load tasks: " + err.message, "danger");
    }
}

function debounceTaskSearch() {
    clearTimeout(searchDebounceTimeout);
    searchDebounceTimeout = setTimeout(loadTasks, 300);
}

function setTaskViewMode(mode) {
    currentTaskViewMode = mode;
    const kanban = document.getElementById("tasks-kanban-container");
    const table = document.getElementById("tasks-table-container");
    const btnKanban = document.getElementById("btn-toggle-kanban");
    const btnTable = document.getElementById("btn-toggle-table");

    if (mode === "kanban") {
        kanban.classList.remove("hidden");
        table.classList.add("hidden");
        btnKanban.classList.add("btn-primary");
        btnKanban.classList.remove("btn-secondary");
        btnTable.classList.remove("btn-primary");
        btnTable.classList.add("btn-secondary");
    } else {
        kanban.classList.add("hidden");
        table.classList.remove("hidden");
        btnTable.classList.add("btn-primary");
        btnTable.classList.remove("btn-secondary");
        btnKanban.classList.remove("btn-primary");
        btnKanban.classList.add("btn-secondary");
    }
}

function renderKanbanBoard(tasks) {
    const colTodo = document.getElementById("cards-col-todo");
    const colProg = document.getElementById("cards-col-progress");
    const colRev = document.getElementById("cards-col-review");
    const colDone = document.getElementById("cards-col-completed");

    colTodo.innerHTML = "";
    colProg.innerHTML = "";
    colRev.innerHTML = "";
    colDone.innerHTML = "";

    const counts = { TODO: 0, IN_PROGRESS: 0, IN_REVIEW: 0, COMPLETED: 0 };

    tasks.forEach(t => {
        const card = createKanbanCard(t);
        switch (t.status) {
            case "TODO":
                colTodo.appendChild(card);
                counts.TODO++;
                break;
            case "IN_PROGRESS":
                colProg.appendChild(card);
                counts.IN_PROGRESS++;
                break;
            case "IN_REVIEW":
                colRev.appendChild(card);
                counts.IN_REVIEW++;
                break;
            case "COMPLETED":
                colDone.appendChild(card);
                counts.COMPLETED++;
                break;
            case "BLOCKED":
                colTodo.appendChild(card);
                counts.TODO++;
                break;
        }
    });

    document.getElementById("count-col-todo").textContent = counts.TODO;
    document.getElementById("count-col-progress").textContent = counts.IN_PROGRESS;
    document.getElementById("count-col-review").textContent = counts.IN_REVIEW;
    document.getElementById("count-col-completed").textContent = counts.COMPLETED;
}

function createKanbanCard(task) {
    const div = document.createElement("div");
    div.className = "kanban-card";
    div.onclick = (e) => {
        if (!e.target.closest("button") && !e.target.closest("select")) {
            openTaskDetailModal(task.id);
        }
    };

    let transitionButtons = "";
    if (task.status === "TODO") {
        transitionButtons = `<button class="btn btn-secondary btn-sm" onclick="transitionTaskStatus(${task.id}, 'IN_PROGRESS', event)">Start Work</button>`;
    } else if (task.status === "IN_PROGRESS") {
        transitionButtons = `
            <button class="btn btn-secondary btn-sm" onclick="transitionTaskStatus(${task.id}, 'IN_REVIEW', event)">Submit Review</button>
            <button class="btn btn-secondary btn-sm" onclick="transitionTaskStatus(${task.id}, 'COMPLETED', event)">Done</button>
        `;
    } else if (task.status === "IN_REVIEW") {
        transitionButtons = `
            <button class="btn btn-primary btn-sm" onclick="transitionTaskStatus(${task.id}, 'COMPLETED', event)">Approve & Complete</button>
        `;
    } else if (task.status === "COMPLETED") {
        transitionButtons = `
            <button class="btn btn-secondary btn-sm" onclick="transitionTaskStatus(${task.id}, 'IN_PROGRESS', event)">Reopen</button>
        `;
    }

    div.innerHTML = `
        <div class="card-top">
            <span class="card-code">${escapeHtml(task.code)}</span>
            ${renderPriorityBadge(task.priority, task.priorityLabel)}
        </div>
        <div class="card-title">${escapeHtml(task.title)}</div>
        <div class="card-project">${escapeHtml(task.projectName)}</div>
        <div class="card-footer">
            <span>${escapeHtml(task.assigneeName)}</span>
            <span>${task.loggedHours}h / ${task.estimatedHours}h</span>
        </div>
        <div class="card-actions" style="margin-top: 8px;">
            ${transitionButtons}
            <button class="btn btn-secondary btn-sm" onclick="openLogHoursModal(${task.id}, event)" title="Log Hours">Log Time</button>
        </div>
    `;
    return div;
}

function renderTasksTable(tasks) {
    const tbody = document.getElementById("tasks-table-body");
    if (tasks.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center text-muted">No tasks found.</td></tr>`;
        return;
    }

    tbody.innerHTML = tasks.map(t => `
        <tr>
            <td class="font-mono"><strong>${escapeHtml(t.code)}</strong></td>
            <td><a href="javascript:void(0)" onclick="openTaskDetailModal(${t.id})" style="font-weight: 600; color: var(--primary); text-decoration: none;">${escapeHtml(t.title)}</a></td>
            <td>${escapeHtml(t.projectName)}</td>
            <td>${escapeHtml(t.assigneeName)}</td>
            <td>${renderTaskStatusBadge(t.status, t.statusLabel)}</td>
            <td>${renderPriorityBadge(t.priority, t.priorityLabel)}</td>
            <td>${t.loggedHours}h / ${t.estimatedHours}h</td>
            <td>${formatDateOnly(t.dueDate)}</td>
            <td class="text-right">
                <button class="btn btn-secondary btn-sm" onclick="openLogHoursModal(${t.id})">Log Time</button>
                <button class="btn btn-secondary btn-sm" onclick="openEditTaskModal(${t.id})">Edit</button>
                ${currentUser.role !== "TEAM_MEMBER" ? `<button class="btn btn-danger btn-sm" onclick="deleteTask(${t.id})">Del</button>` : ""}
            </td>
        </tr>
    `).join("");
}

// -------------------------------------------------------------------------
// Dynamic Task Status Transition & Automatic Progress Sync
// -------------------------------------------------------------------------
async function transitionTaskStatus(taskId, newStatus, event) {
    if (event) event.stopPropagation();
    try {
        const updatedTask = await Api.tasks.updateStatus(taskId, newStatus);
        showToast(`Task ${updatedTask.code} moved to ${updatedTask.statusLabel || newStatus}`, "success");
        // Reload tasks and update active view
        loadTasks();
        if (currentView === "dashboard") {
            loadDashboard();
        }
        if (document.getElementById("modal-task-detail").classList.contains("hidden") === false) {
            openTaskDetailModal(taskId);
        }
    } catch (err) {
        showToast("Status update failed: " + err.message, "danger");
    }
}

// -------------------------------------------------------------------------
// Task Detail Modal & Discussions
// -------------------------------------------------------------------------
async function openTaskDetailModal(taskId) {
    currentActiveTaskId = taskId;
    try {
        const [task, comments] = await Promise.all([
            Api.tasks.get(taskId),
            Api.tasks.getComments(taskId)
        ]);

        document.getElementById("detail-task-code").textContent = task.code;
        document.getElementById("detail-task-title").textContent = task.title;
        document.getElementById("detail-task-desc").textContent = task.description || "No description provided.";
        document.getElementById("detail-task-assignee").textContent = task.assigneeName || "Unassigned";
        document.getElementById("detail-task-time").textContent = `${task.loggedHours}h logged / ${task.estimatedHours}h estimated`;
        document.getElementById("detail-task-duedate").textContent = formatDateOnly(task.dueDate);

        document.getElementById("detail-task-status-container").innerHTML = renderTaskStatusBadge(task.status, task.statusLabel);
        document.getElementById("detail-task-priority-container").innerHTML = renderPriorityBadge(task.priority, task.priorityLabel);

        renderComments(comments);
        showModal("modal-task-detail");
    } catch (err) {
        showToast("Failed to load task details: " + err.message, "danger");
    }
}

function renderComments(comments) {
    const feed = document.getElementById("detail-task-comments");
    if (!comments || comments.length === 0) {
        feed.innerHTML = `<p class="text-muted" style="font-size: 12px;">No comments yet. Start the conversation!</p>`;
        return;
    }

    feed.innerHTML = comments.map(c => `
        <div class="comment-card">
            <div class="comment-header">
                <span class="comment-author">${escapeHtml(c.userName)}</span>
                <span class="comment-date font-mono">${formatDate(c.createdAt)}</span>
            </div>
            <div class="comment-content">${escapeHtml(c.content)}</div>
        </div>
    `).join("");

    feed.scrollTop = feed.scrollHeight;
}

async function submitComment(e) {
    e.preventDefault();
    const input = document.getElementById("new-comment-input");
    const content = input.value.trim();
    if (!content || !currentActiveTaskId) return;

    try {
        await Api.tasks.addComment(currentActiveTaskId, content);
        input.value = "";
        const comments = await Api.tasks.getComments(currentActiveTaskId);
        renderComments(comments);
        showToast("Comment posted", "success");
    } catch (err) {
        showToast("Failed to post comment: " + err.message, "danger");
    }
}

// -------------------------------------------------------------------------
// Work Hours Logging
// -------------------------------------------------------------------------
async function openLogHoursModal(taskId, event) {
    if (event) event.stopPropagation();
    document.getElementById("timelog-task-id").value = taskId;
    document.getElementById("timelog-hours").value = "1.0";
    document.getElementById("timelog-note").value = "";

    try {
        const history = await Api.tasks.getTimeLogs(taskId);
        const historyContainer = document.getElementById("timelog-history");
        if (history.length === 0) {
            historyContainer.innerHTML = `<p class="text-muted" style="font-size: 12px;">No logged hours yet.</p>`;
        } else {
            historyContainer.innerHTML = `
                <table class="data-table" style="font-size: 12px;">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Member</th>
                            <th>Hours</th>
                            <th>Note</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${history.map(h => `
                            <tr>
                                <td class="font-mono">${formatDate(h.logDate || h.createdAt)}</td>
                                <td>${escapeHtml(h.userName)}</td>
                                <td><strong>${h.hoursSpent}h</strong></td>
                                <td class="text-muted">${escapeHtml(h.note || "-")}</td>
                            </tr>
                        `).join("")}
                    </tbody>
                </table>
            `;
        }
        showModal("modal-timelog");
    } catch (err) {
        showToast("Failed to load time logs: " + err.message, "danger");
    }
}

async function submitTimeLog() {
    const taskId = document.getElementById("timelog-task-id").value;
    const hours = parseFloat(document.getElementById("timelog-hours").value);
    const note = document.getElementById("timelog-note").value.trim();

    if (!hours || hours <= 0) {
        showToast("Please enter valid hours.", "danger");
        return;
    }
    if (!note) {
        showToast("Please enter a brief note describing the activity.", "danger");
        return;
    }

    try {
        await Api.tasks.logHours(taskId, hours, note);
        showToast(`Recorded ${hours}h successfully`, "success");
        closeModal();
        if (currentView === "tasks") loadTasks();
        if (currentView === "dashboard") loadDashboard();
        if (currentActiveTaskId == taskId) openTaskDetailModal(taskId);
    } catch (err) {
        showToast("Failed to log hours: " + err.message, "danger");
    }
}

// -------------------------------------------------------------------------
// Project CRUD Modals
// -------------------------------------------------------------------------
function openNewProjectModal() {
    document.getElementById("modal-project-title").textContent = "Create New Project";
    document.getElementById("project-id").value = "";
    document.getElementById("project-name").value = "";
    document.getElementById("project-desc").value = "";
    document.getElementById("project-status").value = "PLANNING";
    document.getElementById("project-priority").value = "MEDIUM";
    document.getElementById("project-budget").value = "10000";
    document.getElementById("project-start-date").value = new Date().toISOString().substring(0, 10);
    const due = new Date();
    due.setMonth(due.getMonth() + 2);
    document.getElementById("project-due-date").value = due.toISOString().substring(0, 10);

    showModal("modal-project");
}

async function openEditProjectModal(projectId) {
    try {
        const p = await Api.projects.get(projectId);
        document.getElementById("modal-project-title").textContent = "Edit Project: " + p.code;
        document.getElementById("project-id").value = p.id;
        document.getElementById("project-name").value = p.title;
        document.getElementById("project-desc").value = p.description || "";
        document.getElementById("project-status").value = p.status;
        document.getElementById("project-priority").value = p.priority;
        document.getElementById("project-manager-select").value = p.managerId || "";
        document.getElementById("project-budget").value = p.budget || 0;
        document.getElementById("project-start-date").value = p.startDate ? p.startDate.substring(0, 10) : "";
        document.getElementById("project-due-date").value = p.dueDate ? p.dueDate.substring(0, 10) : "";

        showModal("modal-project");
    } catch (err) {
        showToast("Failed to load project: " + err.message, "danger");
    }
}

async function saveProject() {
    const id = document.getElementById("project-id").value;
    const title = document.getElementById("project-name").value.trim();
    const description = document.getElementById("project-desc").value.trim();
    const status = document.getElementById("project-status").value;
    const priority = document.getElementById("project-priority").value;
    const managerId = document.getElementById("project-manager-select").value;
    const budget = parseFloat(document.getElementById("project-budget").value) || 0;
    const startDate = document.getElementById("project-start-date").value;
    const dueDate = document.getElementById("project-due-date").value;

    if (!title) {
        showToast("Project title is required", "danger");
        return;
    }

    const payload = {
        title,
        description,
        status,
        priority,
        managerId: managerId ? parseInt(managerId) : null,
        budget,
        startDate: startDate || null,
        dueDate: dueDate || null
    };

    try {
        if (id) {
            await Api.projects.update(id, payload);
            showToast("Project updated successfully", "success");
        } else {
            await Api.projects.create(payload);
            showToast("Project created successfully", "success");
        }
        closeModal();
        loadInitialData();
        if (currentView === "projects") loadProjects();
        if (currentView === "dashboard") loadDashboard();
    } catch (err) {
        showToast("Failed to save project: " + err.message, "danger");
    }
}

async function deleteProject(id) {
    if (!confirm("Are you sure you want to delete this project? All associated tasks will be removed.")) return;
    try {
        await Api.projects.delete(id);
        showToast("Project deleted", "success");
        loadInitialData();
        loadProjects();
    } catch (err) {
        showToast("Delete failed: " + err.message, "danger");
    }
}

// -------------------------------------------------------------------------
// Task CRUD Modals
// -------------------------------------------------------------------------
function openNewTaskModal() {
    document.getElementById("modal-task-title").textContent = "Create New Task";
    document.getElementById("task-id").value = "";
    document.getElementById("task-title").value = "";
    document.getElementById("task-desc").value = "";
    document.getElementById("task-status").value = "TODO";
    document.getElementById("task-priority").value = "MEDIUM";
    document.getElementById("task-est-hours").value = "4.0";
    const due = new Date();
    due.setDate(due.getDate() + 7);
    document.getElementById("task-due-date").value = due.toISOString().substring(0, 10);

    showModal("modal-task");
}

async function openEditTaskModal(taskId) {
    try {
        const t = await Api.tasks.get(taskId);
        document.getElementById("modal-task-title").textContent = "Edit Task: " + t.code;
        document.getElementById("task-id").value = t.id;
        document.getElementById("task-title").value = t.title;
        document.getElementById("task-desc").value = t.description || "";
        document.getElementById("task-project-select").value = t.projectId;
        document.getElementById("task-assignee-select").value = t.assigneeId || "";
        document.getElementById("task-status").value = t.status;
        document.getElementById("task-priority").value = t.priority;
        document.getElementById("task-est-hours").value = t.estimatedHours || 0;
        document.getElementById("task-due-date").value = t.dueDate ? t.dueDate.substring(0, 10) : "";

        showModal("modal-task");
    } catch (err) {
        showToast("Failed to load task: " + err.message, "danger");
    }
}

async function saveTask() {
    const id = document.getElementById("task-id").value;
    const title = document.getElementById("task-title").value.trim();
    const description = document.getElementById("task-desc").value.trim();
    const projectId = document.getElementById("task-project-select").value;
    const assigneeId = document.getElementById("task-assignee-select").value;
    const status = document.getElementById("task-status").value;
    const priority = document.getElementById("task-priority").value;
    const estimatedHours = parseFloat(document.getElementById("task-est-hours").value) || 0;
    const dueDate = document.getElementById("task-due-date").value;

    if (!title) {
        showToast("Task title is required", "danger");
        return;
    }
    if (!projectId && !id) {
        showToast("Project selection is required", "danger");
        return;
    }

    const payload = {
        title,
        description,
        projectId: projectId ? parseInt(projectId) : null,
        assigneeId: assigneeId ? parseInt(assigneeId) : null,
        status,
        priority,
        estimatedHours,
        dueDate: dueDate || null
    };

    try {
        if (id) {
            await Api.tasks.update(id, payload);
            showToast("Task updated successfully", "success");
        } else {
            await Api.tasks.create(payload);
            showToast("Task created successfully", "success");
        }
        closeModal();
        if (currentView === "tasks") loadTasks();
        if (currentView === "dashboard") loadDashboard();
        if (currentView === "projects") loadProjects();
    } catch (err) {
        showToast("Failed to save task: " + err.message, "danger");
    }
}

async function deleteTask(id) {
    if (!confirm("Are you sure you want to delete this task?")) return;
    try {
        await Api.tasks.delete(id);
        showToast("Task deleted", "success");
        if (currentView === "tasks") loadTasks();
        if (currentView === "dashboard") loadDashboard();
    } catch (err) {
        showToast("Delete failed: " + err.message, "danger");
    }
}

// -------------------------------------------------------------------------
// Admin Section Controllers (Users, Audit, Settings)
// -------------------------------------------------------------------------
async function loadAdminUsers() {
    const tbody = document.getElementById("admin-users-table-body");
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">Loading users...</td></tr>`;

    const search = document.getElementById("admin-user-search-input").value.trim();
    const role = document.getElementById("admin-user-role-filter").value;

    const params = {};
    if (search) params.search = search;
    if (role !== "ALL") params.role = role;

    try {
        const users = await Api.admin.listUsers(params);
        tbody.innerHTML = users.map(u => `
            <tr>
                <td class="font-mono">#${u.id}</td>
                <td><strong>${escapeHtml(u.fullName)}</strong></td>
                <td>${escapeHtml(u.email)}</td>
                <td>${renderRoleBadge(u.role, u.roleName)}</td>
                <td>${escapeHtml(u.department || "-")}</td>
                <td>${escapeHtml(u.jobTitle || "-")}</td>
                <td>
                    <span class="badge ${u.status === 'ACTIVE' ? 'badge-completed' : 'badge-blocked'}">
                        ${u.status}
                    </span>
                </td>
                <td class="text-right">
                    <button class="btn btn-secondary btn-sm" onclick="toggleUserStatus(${u.id}, '${u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}')">
                        ${u.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                    </button>
                    <button class="btn btn-secondary btn-sm" onclick="openEditUserModal(${u.id})">Edit</button>
                </td>
            </tr>
        `).join("");
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center" style="color: var(--danger);">Failed to load users: ${err.message}</td></tr>`;
    }
}

function debounceUserSearch() {
    clearTimeout(searchDebounceTimeout);
    searchDebounceTimeout = setTimeout(loadAdminUsers, 300);
}

function openNewUserModal() {
    document.getElementById("modal-user-title").textContent = "Add New User";
    document.getElementById("user-id").value = "";
    document.getElementById("user-fullname").value = "";
    document.getElementById("user-email").value = "";
    document.getElementById("user-password-group").classList.remove("hidden");
    document.getElementById("user-role").value = "TEAM_MEMBER";
    document.getElementById("user-status").value = "ACTIVE";
    document.getElementById("user-department").value = "Engineering";
    document.getElementById("user-jobtitle").value = "Software Engineer";

    showModal("modal-user");
}

async function openEditUserModal(userId) {
    try {
        const u = await Api.admin.getUser(userId);
        document.getElementById("modal-user-title").textContent = "Edit User: " + u.fullName;
        document.getElementById("user-id").value = u.id;
        document.getElementById("user-fullname").value = u.fullName;
        document.getElementById("user-email").value = u.email;
        document.getElementById("user-password-group").classList.add("hidden");
        document.getElementById("user-role").value = u.role;
        document.getElementById("user-status").value = u.status;
        document.getElementById("user-department").value = u.department || "";
        document.getElementById("user-jobtitle").value = u.jobTitle || "";

        showModal("modal-user");
    } catch (err) {
        showToast("Failed to load user: " + err.message, "danger");
    }
}

async function saveUser() {
    const id = document.getElementById("user-id").value;
    const fullName = document.getElementById("user-fullname").value.trim();
    const email = document.getElementById("user-email").value.trim();
    const password = document.getElementById("user-password").value;
    const role = document.getElementById("user-role").value;
    const status = document.getElementById("user-status").value;
    const department = document.getElementById("user-department").value.trim();
    const jobTitle = document.getElementById("user-jobtitle").value.trim();

    if (!fullName || !email) {
        showToast("Full Name and Email are required", "danger");
        return;
    }

    const payload = { fullName, email, role, status, department, jobTitle };
    if (!id) payload.password = password;

    try {
        if (id) {
            await Api.admin.updateUser(id, payload);
            showToast("User updated successfully", "success");
        } else {
            await Api.admin.createUser(payload);
            showToast("User created successfully", "success");
        }
        closeModal();
        loadAdminUsers();
        loadInitialData();
    } catch (err) {
        showToast("Failed to save user: " + err.message, "danger");
    }
}

async function toggleUserStatus(userId, newStatus) {
    try {
        await Api.admin.toggleUserStatus(userId, newStatus);
        showToast("User status changed to " + newStatus, "success");
        loadAdminUsers();
    } catch (err) {
        showToast("Failed to update status: " + err.message, "danger");
    }
}

async function loadAdminAuditLogs() {
    const tbody = document.getElementById("admin-audit-table-body");
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">Loading audit trail...</td></tr>`;

    try {
        const logs = await Api.admin.getAuditLogs(100);
        if (logs.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">No audit events logged yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = logs.map(l => `
            <tr>
                <td class="font-mono">#${l.id}</td>
                <td class="font-mono" style="font-size: 12px;">${formatDate(l.createdAt)}</td>
                <td><strong>${escapeHtml(l.userName)}</strong></td>
                <td><span class="badge badge-admin">${escapeHtml(l.action)}</span></td>
                <td>${escapeHtml(l.entityType)}</td>
                <td class="font-mono">#${escapeHtml(l.entityId || "-")}</td>
                <td class="text-muted">${escapeHtml(l.details || "-")}</td>
                <td class="font-mono" style="font-size: 11px;">${escapeHtml(l.ipAddress || "-")}</td>
            </tr>
        `).join("");
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center" style="color: var(--danger);">Failed to load audit logs: ${err.message}</td></tr>`;
    }
}

async function loadAdminSettings() {
    const container = document.getElementById("settings-form-container");
    container.innerHTML = `<p class="text-muted">Loading settings...</p>`;

    try {
        const settings = await Api.admin.getSettings();
        const keys = Object.keys(settings);

        if (keys.length === 0) {
            container.innerHTML = `<p class="text-muted">No configuration properties defined.</p>`;
            return;
        }

        container.innerHTML = `
            <div style="display: flex; flex-direction: column; gap: 16px;">
                ${keys.map(k => `
                    <div class="form-group" style="margin-bottom: 0;">
                        <label class="form-label font-mono" style="font-size: 13px;">${escapeHtml(k)}</label>
                        <div style="display: flex; gap: 8px;">
                            <input type="text" id="setting-${escapeHtml(k)}" class="form-control" style="flex: 1;" value="${escapeHtml(settings[k])}">
                            <button class="btn btn-secondary btn-sm" onclick="saveSetting('${escapeHtml(k)}')">Save</button>
                        </div>
                    </div>
                `).join("")}
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<p style="color: var(--danger);">Failed to load configuration: ${err.message}</p>`;
    }
}

async function saveSetting(key) {
    const input = document.getElementById("setting-" + key);
    if (!input) return;
    const value = input.value.trim();

    try {
        await Api.admin.updateSetting(key, value);
        showToast(`Setting '${key}' updated`, "success");
    } catch (err) {
        showToast("Failed to update setting: " + err.message, "danger");
    }
}

// -------------------------------------------------------------------------
// Modal Controller Utilities
// -------------------------------------------------------------------------
function showModal(modalId) {
    document.getElementById("modal-backdrop").classList.remove("hidden");
    document.querySelectorAll(".modal-container").forEach(m => m.classList.add("hidden"));
    document.getElementById(modalId).classList.remove("hidden");
}

function closeModal() {
    document.getElementById("modal-backdrop").classList.add("hidden");
    document.querySelectorAll(".modal-container").forEach(m => m.classList.add("hidden"));
}

// -------------------------------------------------------------------------
// UI Toast Notifications
// -------------------------------------------------------------------------
function showToast(message, type = "primary") {
    const container = document.getElementById("toast-container");
    if (!container) return;

    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    toast.textContent = message;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.transition = "opacity 0.25s ease";
        toast.style.opacity = "0";
        setTimeout(() => toast.remove(), 250);
    }, 3500);
}

// -------------------------------------------------------------------------
// Render Badges & Formatters
// -------------------------------------------------------------------------
function renderTaskStatusBadge(status, label) {
    const display = label || status;
    switch (status) {
        case "TODO": return `<span class="badge badge-todo">${display}</span>`;
        case "IN_PROGRESS": return `<span class="badge badge-progress">${display}</span>`;
        case "IN_REVIEW": return `<span class="badge badge-review">${display}</span>`;
        case "COMPLETED": return `<span class="badge badge-completed">${display}</span>`;
        case "BLOCKED": return `<span class="badge badge-blocked">${display}</span>`;
        default: return `<span class="badge">${display}</span>`;
    }
}

function renderProjectStatusBadge(status) {
    switch (status) {
        case "PLANNING": return `<span class="badge badge-todo">PLANNING</span>`;
        case "IN_PROGRESS": return `<span class="badge badge-progress">IN PROGRESS</span>`;
        case "ON_HOLD": return `<span class="badge badge-review">ON HOLD</span>`;
        case "COMPLETED": return `<span class="badge badge-completed">COMPLETED</span>`;
        case "CANCELLED": return `<span class="badge badge-blocked">CANCELLED</span>`;
        default: return `<span class="badge">${status}</span>`;
    }
}

function renderPriorityBadge(priority, label) {
    const display = label || priority;
    switch (priority) {
        case "LOW": return `<span class="badge badge-low">${display}</span>`;
        case "MEDIUM": return `<span class="badge badge-medium">${display}</span>`;
        case "HIGH": return `<span class="badge badge-high">${display}</span>`;
        case "CRITICAL": return `<span class="badge badge-critical">${display}</span>`;
        default: return `<span class="badge">${display}</span>`;
    }
}

function renderRoleBadge(role, label) {
    const display = label || role;
    switch (role) {
        case "ADMIN": return `<span class="badge badge-admin">${display}</span>`;
        case "PROJECT_MANAGER": return `<span class="badge badge-manager">${display}</span>`;
        case "TEAM_MEMBER": return `<span class="badge badge-member">${display}</span>`;
        default: return `<span class="badge">${display}</span>`;
    }
}

function formatDate(ts) {
    if (!ts) return "-";
    const d = new Date(ts);
    if (isNaN(d.getTime())) return ts;
    return d.toISOString().replace("T", " ").substring(0, 19);
}

function formatDateOnly(ts) {
    if (!ts) return "-";
    const d = new Date(ts);
    if (isNaN(d.getTime())) return ts.substring(0, 10);
    return d.toISOString().substring(0, 10);
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
