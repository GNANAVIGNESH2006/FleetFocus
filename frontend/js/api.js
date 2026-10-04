/* ==========================================================================
   api.js
   Central Axios instance for all calls to the FleetFocus backend.

   - Attaches the JWT (from localStorage) to every request.
   - Redirects to the login page on any 401 Unauthorized response.
   - Never logs out on 403 Forbidden.
   ========================================================================== */

const API_BASE_URL = (window.FLEETFOCUS_CONFIG && window.FLEETFOCUS_CONFIG.API_BASE_URL)
  || "https://fleetfocus-production.up.railway.app/api";

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json"
  }
});

// ---- Request interceptor: attach "Authorization: Bearer <token>" ----
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("jwtToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ---- Response interceptor: handle 401 Unauthorized globally (never logout on 403) ----
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem("jwtToken");
      localStorage.removeItem("username");
      localStorage.removeItem("role");

      const path = window.location.pathname || "";
      if (!path.endsWith("login.html") && !path.endsWith("register.html")) {
        window.location.href = "login.html";
      }
    }
    return Promise.reject(error);
  }
);
