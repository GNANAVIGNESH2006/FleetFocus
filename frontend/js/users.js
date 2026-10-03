let createUserModal;
let roleModal;
let userDeleteModal;
let userIdPendingDelete = null;
let userCurrentPage = 0;
const USER_PAGE_SIZE = 20;

document.addEventListener("DOMContentLoaded", async () => {
  if (!requireAuth()) return;
  if (!(await syncSession())) return;
  if (!requireRole(["ADMIN"])) return;
  initNavbar();

  createUserModal = new bootstrap.Modal(document.getElementById("createUserModal"));
  roleModal = new bootstrap.Modal(document.getElementById("roleModal"));
  userDeleteModal = new bootstrap.Modal(document.getElementById("userDeleteModal"));

  document.getElementById("addUserBtn").addEventListener("click", openCreateUserModal);
  document.getElementById("createUserForm").addEventListener("submit", createUser);
  document.getElementById("roleForm").addEventListener("submit", saveRole);
  document.getElementById("confirmUserDeleteBtn").addEventListener("click", confirmDeleteUser);

  const searchInput = document.getElementById("userSearch");
  const roleFilter = document.getElementById("userRoleFilter");
  const sortSelect = document.getElementById("userSort");

  if (searchInput) {
    searchInput.addEventListener("input", () => {
      userCurrentPage = 0;
      loadUsers();
    });
  }
  if (roleFilter) {
    roleFilter.addEventListener("change", () => {
      userCurrentPage = 0;
      loadUsers();
    });
  }
  if (sortSelect) {
    sortSelect.addEventListener("change", () => {
      userCurrentPage = 0;
      loadUsers();
    });
  }

  loadUsers();
});

function openCreateUserModal() {
  document.getElementById("createUserForm").reset();
  clearAlert("createUserFormAlert");
  createUserModal.show();
}

async function createUser(e) {
  e.preventDefault();
  clearAlert("createUserFormAlert");

  const payload = {
    username: document.getElementById("newUsername").value.trim(),
    email: document.getElementById("newEmail").value.trim(),
    password: document.getElementById("newPassword").value,
    role: document.getElementById("createUserRole").value
  };

  const submitBtn = document.querySelector('#createUserForm button[type="submit"]');
  await withButtonLoading(submitBtn, async () => {
    try {
      await api.post("/users", payload);
      createUserModal.hide();
      showAlert("usersAlert", "User created successfully.", "success");
      loadUsers();
    } catch (error) {
      showAlert("createUserFormAlert", getErrorMessage(error));
    }
  });
}

async function loadUsers() {
  showSpinner();
  clearAlert("usersAlert");

  const q = (document.getElementById("userSearch")?.value || "").trim();
  const role = document.getElementById("userRoleFilter")?.value || "";
  const sort = document.getElementById("userSort")?.value || "id,asc";

  const params = { page: userCurrentPage, size: USER_PAGE_SIZE, sort };
  if (q) params.q = q;
  if (role) params.role = role;

  try {
    const response = await api.get("/users", { params });
    const total = Number(response.headers["x-total-count"] ?? response.data.length);
    renderUsersTable(response.data);
    if (typeof renderPaginationBar === "function") {
      renderPaginationBar("usersPagination", userCurrentPage, USER_PAGE_SIZE, total, (newPage) => {
        userCurrentPage = newPage;
        loadUsers();
      });
    }
  } catch (error) {
    showAlert("usersAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

function renderUsersTable(users) {
  const tbody = document.getElementById("usersTableBody");
  tbody.innerHTML = "";

  if (!users || users.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-4">No users found.</td></tr>`;
    return;
  }

  const currentUsername = (getUsername() || "").toLowerCase();

  users.forEach((u) => {
    const isSelf = u.username && u.username.toLowerCase() === currentUsername;
    const youBadge = isSelf ? ` <span class="badge bg-info text-dark ms-1">You</span>` : "";
    const actionsHtml = isSelf
      ? `<span class="text-muted small">Current account</span>`
      : `<button class="btn btn-sm btn-outline-primary me-1" onclick="openRoleModal(${u.id}, '${escapeHtml(u.role)}')"><i class="bi bi-arrow-repeat"></i> Role</button>
         <button class="btn btn-sm btn-outline-danger" onclick="openDeleteUserModal(${u.id})"><i class="bi bi-trash"></i> Delete</button>`;

    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${u.id}</td>
      <td>${escapeHtml(u.username)}${youBadge}</td>
      <td>${escapeHtml(u.email)}</td>
      <td><span class="badge bg-secondary">${escapeHtml(u.role)}</span></td>
      <td class="text-end">${actionsHtml}</td>`;
    tbody.appendChild(row);
  });
}

function openRoleModal(id, currentRole) {
  clearAlert("roleFormAlert");
  document.getElementById("roleUserId").value = id;
  document.getElementById("newRole").value = currentRole;
  roleModal.show();
}

async function saveRole(e) {
  e.preventDefault();
  clearAlert("roleFormAlert");

  const id = document.getElementById("roleUserId").value;
  const role = document.getElementById("newRole").value;

  const submitBtn = document.querySelector('#roleForm button[type="submit"]');
  await withButtonLoading(submitBtn, async () => {
    try {
      await api.put(`/users/${id}/role`, { role });
      roleModal.hide();
      showAlert("usersAlert", "User role updated.", "success");
      loadUsers();
    } catch (error) {
      showAlert("roleFormAlert", getErrorMessage(error));
    }
  });
}

function openDeleteUserModal(id) {
  userIdPendingDelete = id;
  userDeleteModal.show();
}

async function confirmDeleteUser() {
  if (!userIdPendingDelete) return;
  const btn = document.getElementById("confirmUserDeleteBtn");
  await withButtonLoading(btn, async () => {
    try {
      await api.delete(`/users/${userIdPendingDelete}`);
      showAlert("usersAlert", "User deleted successfully.", "success");
      loadUsers();
    } catch (error) {
      showAlert("usersAlert", getErrorMessage(error));
    } finally {
      userIdPendingDelete = null;
      userDeleteModal.hide();
    }
  });
}
