function isTokenExpired(token) {
  if (!token || typeof token !== "string") return true;
  const parts = token.split(".");
  if (parts.length !== 3) return true;
  try {
    let base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    while (base64.length % 4 !== 0) {
      base64 += "=";
    }
    const payload = JSON.parse(atob(base64));
    if (!payload || typeof payload.exp !== "number") {
      return true;
    }
    return payload.exp * 1000 <= Date.now();
  } catch (e) {
    return true;
  }
}

function getRole() {
  return localStorage.getItem("role") || "";
}

function getUsername() {
  return localStorage.getItem("username") || "";
}

function clearSession() {
  localStorage.removeItem("jwtToken");
  localStorage.removeItem("username");
  localStorage.removeItem("role");
}

function requireAuth() {
  const token = localStorage.getItem("jwtToken");
  if (!token || isTokenExpired(token)) {
    clearSession();
    window.location.href = "login.html";
    return false;
  }
  return true;
}

function requireRole(allowedRoles) {
  if (!requireAuth()) return false;
  const role = getRole();
  if (!allowedRoles.includes(role)) {
    window.location.href = "dashboard.html";
    return false;
  }
  return true;
}

async function syncSession() {
  if (!requireAuth()) return false;
  try {
    const response = await api.get("/auth/me");
    const { username, role } = response.data || {};
    if (username) localStorage.setItem("username", username);
    if (role) localStorage.setItem("role", role);
    return true;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      clearSession();
      window.location.href = "login.html";
      return false;
    }
    return true;
  }
}

function applyRoleVisibility(explicitRole) {
  const role = explicitRole || getRole();

  document.querySelectorAll("[data-roles]").forEach((el) => {
    const allowed = el.getAttribute("data-roles").split(",").map((r) => r.trim());
    if (allowed.includes(role)) {
      el.classList.remove("d-none");
    } else {
      el.classList.add("d-none");
    }
  });

  document.querySelectorAll(".admin-only").forEach((el) => {
    if (role === "ADMIN") {
      el.classList.remove("d-none");
    } else {
      el.classList.add("d-none");
    }
  });
}

function initNavbar() {
  const username = getUsername();
  const role = getRole();

  const userLabel = document.getElementById("navUserLabel");
  if (userLabel) {
    userLabel.textContent = username ? `${username} (${role})` : "";
  }

  const logoutBtn = document.getElementById("logoutBtn");
  if (logoutBtn && !logoutBtn.dataset.bound) {
    logoutBtn.dataset.bound = "true";
    logoutBtn.addEventListener("click", (e) => {
      e.preventDefault();
      logout();
    });
  }

  if (typeof initLayout === "function") {
    initLayout();
  } else {
    applyRoleVisibility(role);
    initSidebarToggle();
  }
}

function logout() {
  clearSession();
  window.location.href = "login.html";
}

document.addEventListener("DOMContentLoaded", () => {
  const loginForm = document.getElementById("loginForm");
  if (!loginForm) return;

  const existingToken = localStorage.getItem("jwtToken");
  if (existingToken) {
    if (!isTokenExpired(existingToken)) {
      window.location.href = "dashboard.html";
      return;
    }
    clearSession();
  }

  loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearAlert("loginAlert");

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;

    if (!username || !password) {
      showAlert("loginAlert", "Please enter both username and password.");
      return;
    }

    const submitBtn = loginForm.querySelector('button[type="submit"]');
    await withButtonLoading(submitBtn, async () => {
      try {
        const response = await api.post("/auth/login", { username, password });
        const { token, username: returnedUsername, role } = response.data;

        localStorage.setItem("jwtToken", token);
        localStorage.setItem("username", returnedUsername);
        localStorage.setItem("role", role);

        window.location.href = "dashboard.html";
      } catch (error) {
        showAlert("loginAlert", getErrorMessage(error) || "Invalid username or password.");
      }
    });
  });
});

document.addEventListener("DOMContentLoaded", () => {
  const registerForm = document.getElementById("registerForm");
  if (!registerForm) return;

  const existingToken = localStorage.getItem("jwtToken");
  if (existingToken) {
    if (!isTokenExpired(existingToken)) {
      window.location.href = "dashboard.html";
      return;
    }
    clearSession();
  }

  registerForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearAlert("registerAlert");

    const username = document.getElementById("regUsername").value.trim();
    const email = document.getElementById("regEmail").value.trim();
    const password = document.getElementById("regPassword").value;

    if (!username || !email || !password) {
      showAlert("registerAlert", "Please fill in all fields.");
      return;
    }

    const submitBtn = registerForm.querySelector('button[type="submit"]');
    await withButtonLoading(submitBtn, async () => {
      try {
        await api.post("/auth/register", { username, email, password });

        showAlert(
          "registerAlert",
          "Account created successfully. Redirecting to login&hellip;",
          "success"
        );

        setTimeout(() => {
          window.location.href = "login.html";
        }, 1200);
      } catch (error) {
        showAlert("registerAlert", getErrorMessage(error) || "Registration failed. Please try again.");
      }
    });
  });
});
