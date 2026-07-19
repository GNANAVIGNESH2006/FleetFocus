function requireAuth() {
  const token = localStorage.getItem("jwtToken");
  if (!token) {
    window.location.href = "login.html";
  }
}

function requireRole(allowedRoles) {
  requireAuth();
  const role = localStorage.getItem("role");
  if (!allowedRoles.includes(role)) {
    window.location.href = "dashboard.html";
  }
}

function applyRoleVisibility() {
  const role = localStorage.getItem("role");

  document.querySelectorAll("[data-roles]").forEach((el) => {
    const allowed = el.getAttribute("data-roles").split(",").map((r) => r.trim());
    if (!allowed.includes(role)) {
      el.classList.add("d-none");
    }
  });

  if (role !== "ADMIN") {
    document.querySelectorAll(".admin-only").forEach((el) => el.classList.add("d-none"));
  }
}

function initNavbar() {
  const username = localStorage.getItem("username");
  const role = localStorage.getItem("role");

  const userLabel = document.getElementById("navUserLabel");
  if (userLabel) {
    userLabel.textContent = username ? `${username} (${role})` : "";
  }

  const logoutBtn = document.getElementById("logoutBtn");
  if (logoutBtn) {
    logoutBtn.addEventListener("click", (e) => {
      e.preventDefault();
      logout();
    });
  }

  applyRoleVisibility();
  initSidebarToggle();
}

function logout() {
  localStorage.removeItem("jwtToken");
  localStorage.removeItem("username");
  localStorage.removeItem("role");
  window.location.href = "login.html";
}

document.addEventListener("DOMContentLoaded", () => {
  const loginForm = document.getElementById("loginForm");
  if (!loginForm) return;

  if (localStorage.getItem("jwtToken")) {
    window.location.href = "dashboard.html";
    return;
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

    showSpinner();
    try {
      const response = await api.post("/auth/login", { username, password });
      const { token, username: returnedUsername, role } = response.data;

      localStorage.setItem("jwtToken", token);
      localStorage.setItem("username", returnedUsername);
      localStorage.setItem("role", role);

      window.location.href = "dashboard.html";
    } catch (error) {
      showAlert("loginAlert", getErrorMessage(error) || "Invalid username or password.");
    } finally {
      hideSpinner();
    }
  });
});

document.addEventListener("DOMContentLoaded", () => {
  const registerForm = document.getElementById("registerForm");
  if (!registerForm) return;

  if (localStorage.getItem("jwtToken")) {
    window.location.href = "dashboard.html";
    return;
  }

  registerForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearAlert("registerAlert");

    const username = document.getElementById("regUsername").value.trim();
    const email = document.getElementById("regEmail").value.trim();
    const password = document.getElementById("regPassword").value;
    const role = document.getElementById("regRole").value;

    if (!username || !email || !password || !role) {
      showAlert("registerAlert", "Please fill in all fields.");
      return;
    }

    showSpinner();
    try {
      await api.post("/auth/register", { username, email, password, role });

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
    } finally {
      hideSpinner();
    }
  });
});
