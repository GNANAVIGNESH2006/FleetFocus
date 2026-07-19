/* ==========================================================================
   api.js
   Central Axios instance for all calls to the FleetFocus backend.

   - Attaches the JWT (from localStorage) to every request.
   - Redirects to the login page on any 401 Unauthorized response.

   Change API_BASE_URL below if your backend runs on a different host/port.
   ========================================================================== */

const API_BASE_URL = "http://localhost:8080/api";

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

// ---- Response interceptor: handle 401 Unauthorized globally ----
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem("jwtToken");
      localStorage.removeItem("username");
      localStorage.removeItem("role");

      // Avoid an infinite redirect loop if we are already on the login page.
      if (!window.location.pathname.endsWith("login.html")) {
        window.location.href = "login.html";
      }
    }
    return Promise.reject(error);
  }
);
