/**
 * Online Project Management Tool - API Client
 * Enterprise REST client with session management, bearer authentication,
 * and robust error propagation.
 */

const Api = (function () {
    const TOKEN_KEY = "PMTOOL_SESSION_ID";

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function setToken(token) {
        if (token) {
            localStorage.setItem(TOKEN_KEY, token);
        } else {
            localStorage.removeItem(TOKEN_KEY);
        }
    }

    async function request(endpoint, options = {}) {
        const headers = {
            "Content-Type": "application/json",
            "X-Requested-With": "XMLHttpRequest",
            ...(options.headers || {})
        };

        const token = getToken();
        if (token) {
            headers["Authorization"] = "Bearer " + token;
        }

        const config = {
            ...options,
            headers
        };

        if (config.body && typeof config.body === "object" && !(config.body instanceof FormData)) {
            config.body = JSON.stringify(config.body);
        }

        try {
            const response = await fetch(endpoint, config);

            if (response.status === 401 && !endpoint.includes("/api/auth/login")) {
                setToken(null);
                window.dispatchEvent(new CustomEvent("pmtool:unauthorized"));
                throw new Error("Session expired. Please sign in again.");
            }

            const contentType = response.headers.get("content-type");
            let data = null;
            if (contentType && contentType.includes("application/json")) {
                data = await response.json();
            } else {
                data = await response.text();
            }

            if (!response.ok) {
                const message = (data && data.message) ? data.message : ("HTTP " + response.status + ": " + response.statusText);
                throw new Error(message);
            }

            if (data && typeof data === "object" && data.success !== undefined && data.data !== undefined) {
                return data.data;
            }
            return data;
        } catch (err) {
            throw err;
        }
    }

    return {
        getToken,
        setToken,

        auth: {
            login: (email, password) => request("/api/auth/login", { method: "POST", body: { email, password } }),
            logout: () => request("/api/auth/logout", { method: "POST" }),
            getStatus: () => request("/api/auth/status", { method: "GET" }),
            getDemoAccounts: () => request("/api/auth/demo-accounts", { method: "GET" })
        },

        user: {
            getProfile: () => request("/api/users/profile", { method: "GET" }),
            updateProfile: (data) => request("/api/users/profile", { method: "PUT", body: data }),
            getAssignable: () => request("/api/users/assignable", { method: "GET" })
        },

        projects: {
            list: (params = {}) => {
                const q = new URLSearchParams(params).toString();
                return request("/api/projects" + (q ? "?" + q : ""), { method: "GET" });
            },
            get: (id) => request("/api/projects/" + id, { method: "GET" }),
            create: (data) => request("/api/projects", { method: "POST", body: data }),
            update: (id, data) => request("/api/projects/" + id, { method: "PUT", body: data }),
            delete: (id) => request("/api/projects/" + id, { method: "DELETE" }),
            getAnalytics: (id) => request("/api/projects/" + id + "/analytics", { method: "GET" })
        },

        tasks: {
            list: (params = {}) => {
                const q = new URLSearchParams(params).toString();
                return request("/api/tasks" + (q ? "?" + q : ""), { method: "GET" });
            },
            get: (id) => request("/api/tasks/" + id, { method: "GET" }),
            create: (data) => request("/api/tasks", { method: "POST", body: data }),
            update: (id, data) => request("/api/tasks/" + id, { method: "PUT", body: data }),
            delete: (id) => request("/api/tasks/" + id, { method: "DELETE" }),
            updateStatus: (id, status) => request("/api/tasks/" + id + "/status", { method: "POST", body: { status } }),
            logHours: (id, hours, note) => request("/api/tasks/" + id + "/time-logs", { method: "POST", body: { hours, note } }),
            getTimeLogs: (id) => request("/api/tasks/" + id + "/time-logs", { method: "GET" }),
            getComments: (id) => request("/api/tasks/" + id + "/comments", { method: "GET" }),
            addComment: (id, content) => request("/api/tasks/" + id + "/comments", { method: "POST", body: { content } })
        },

        admin: {
            listUsers: (params = {}) => {
                const q = new URLSearchParams(params).toString();
                return request("/api/admin/users" + (q ? "?" + q : ""), { method: "GET" });
            },
            getUser: (id) => request("/api/admin/users/" + id, { method: "GET" }),
            createUser: (data) => request("/api/admin/users", { method: "POST", body: data }),
            updateUser: (id, data) => request("/api/admin/users/" + id, { method: "PUT", body: data }),
            toggleUserStatus: (id, status) => request("/api/admin/users/" + id, { method: "PUT", body: { status } }),
            getAuditLogs: (limit = 50) => request("/api/admin/audit-logs?limit=" + limit, { method: "GET" }),
            getSettings: () => request("/api/admin/settings", { method: "GET" }),
            updateSetting: (key, value) => request("/api/admin/settings", { method: "PUT", body: { key, value } }),
            getOverview: () => request("/api/admin/overview", { method: "GET" })
        }
    };
})();
