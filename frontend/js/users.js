let roleModal;
let userDeleteModal;
let userIdPendingDelete = null;

document.addEventListener("DOMContentLoaded", () => {
  requireRole(["ADMIN"]);
  initNavbar();

  roleModal = new bootstrap.Modal(document.getElementById("roleModal"));
  userDeleteModal = new bootstrap.Modal(document.getElementById("userDeleteModal"));

  document.getElementById("roleForm").addEventListener("submit", saveRole);
  document.getElementById("confirmUserDeleteBtn").addEventListener("click", confirmDeleteUser);

  loadUsers();
});

async function loadUsers() {
  showSpinner();
  clearAlert("usersAlert");
  try {
    const response = await api.get("/users");
    renderUsersTable(response.data);
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

  users.forEach((u) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${u.id}</td>
      <td>${escapeHtml(u.username)}</td>
      <td>${escapeHtml(u.email)}</td>
      <td><span class="badge bg-secondary">${u.role}</span></td>
      <td class="text-end">
        <button class="btn btn-sm btn-outline-primary me-1" onclick="openRoleModal(${u.id}, '${u.role}')"><i class="bi bi-arrow-repeat"></i> Role</button>
        <button class="btn btn-sm btn-outline-danger" onclick="openDeleteUserModal(${u.id})"><i class="bi bi-trash"></i> Delete</button>
      </td>`;
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

  showSpinner();
  try {
    await api.put(`/users/${id}/role`, { role });
    roleModal.hide();
    showAlert("usersAlert", "User role updated.", "success");
    loadUsers();
  } catch (error) {
    showAlert("roleFormAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

function openDeleteUserModal(id) {
  userIdPendingDelete = id;
  userDeleteModal.show();
}

async function confirmDeleteUser() {
  if (!userIdPendingDelete) return;
  showSpinner();
  try {
    await api.delete(`/users/${userIdPendingDelete}`);
    showAlert("usersAlert", "User deleted successfully.", "success");
    loadUsers();
  } catch (error) {
    showAlert("usersAlert", getErrorMessage(error));
  } finally {
    userIdPendingDelete = null;
    userDeleteModal.hide();
    hideSpinner();
  }
}
