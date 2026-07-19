let driverModal;
let driverStatusModal;
let driverDeleteModal;
let editingDriverId = null;
let driverIdPendingDelete = null;
let currentDrivers = [];

document.addEventListener("DOMContentLoaded", () => {
  requireAuth();
  initNavbar();

  const role = localStorage.getItem("role");

  if (role === "DRIVER") {
    loadMyProfile();
    return;
  }

  driverModal = new bootstrap.Modal(document.getElementById("driverModal"));
  driverStatusModal = new bootstrap.Modal(document.getElementById("driverStatusModal"));
  driverDeleteModal = new bootstrap.Modal(document.getElementById("driverDeleteModal"));

  document.getElementById("addDriverBtn").addEventListener("click", openAddDriverModal);
  document.getElementById("driverForm").addEventListener("submit", saveDriver);
  document.getElementById("availableOnlyToggle").addEventListener("change", loadDrivers);
  document.getElementById("driverStatusForm").addEventListener("submit", saveDriverStatus);
  document.getElementById("confirmDriverDeleteBtn").addEventListener("click", confirmDeleteDriver);

  loadDrivers();
});

async function loadMyProfile() {
  const container = document.getElementById("myProfileCard");
  try {
    const response = await api.get("/drivers/me");
    const d = response.data;
    container.innerHTML = `
      <div class="d-flex justify-content-between align-items-center">
        <div>
          <div class="fw-bold">${escapeHtml(d.name)}</div>
          <div class="text-muted small">License: ${escapeHtml(d.licenseNumber)}</div>
        </div>
        <span class="badge ${statusBadgeClass(d.status)}">${d.status}</span>
      </div>`;
  } catch (error) {
    container.innerHTML = `<span class="text-danger">${getErrorMessage(error)}</span>`;
  }
}

async function loadDrivers() {
  showSpinner();
  clearAlert("driversAlert");

  const availableOnly = document.getElementById("availableOnlyToggle").checked;
  const endpoint = availableOnly ? "/drivers/available" : "/drivers";

  try {
    const response = await api.get(endpoint);
    currentDrivers = response.data;
    renderDriversTable(currentDrivers);
  } catch (error) {
    showAlert("driversAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

function renderDriversTable(drivers) {
  const role = localStorage.getItem("role");
  const tbody = document.getElementById("driversTableBody");
  tbody.innerHTML = "";

  if (!drivers || drivers.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-4">No drivers found.</td></tr>`;
    return;
  }

  drivers.forEach((d) => {
    const row = document.createElement("tr");
    const adminActions = role === "ADMIN" ? `
        <button class="btn btn-sm btn-outline-primary me-1" onclick="editDriver(${d.id})"><i class="bi bi-pencil-square"></i> Edit</button>
        <button class="btn btn-sm btn-outline-danger" onclick="openDeleteDriverModal(${d.id})"><i class="bi bi-trash"></i> Delete</button>` : "";
    const dispatcherActions = role === "DISPATCHER" ? `
        <button class="btn btn-sm btn-outline-secondary" onclick="openDriverStatusModal(${d.id}, '${d.status}')"><i class="bi bi-arrow-repeat"></i> Status</button>` : "";

    row.innerHTML = `
      <td>${d.id}</td>
      <td>${escapeHtml(d.name)}</td>
      <td>${escapeHtml(d.licenseNumber)}</td>
      <td><span class="badge ${statusBadgeClass(d.status)}">${d.status}</span></td>
      <td class="text-end">${adminActions}${dispatcherActions}</td>`;
    tbody.appendChild(row);
  });
}

function openAddDriverModal() {
  editingDriverId = null;
  document.getElementById("driverForm").reset();
  document.getElementById("driverId").value = "";
  document.getElementById("driverModalTitle").textContent = "Add Driver";
  clearAlert("driverFormAlert");
  driverModal.show();
}

function editDriver(id) {
  const driver = currentDrivers.find((d) => d.id === id);
  if (!driver) return;

  editingDriverId = id;
  document.getElementById("driverModalTitle").textContent = "Update Driver";
  document.getElementById("driverId").value = driver.id;
  document.getElementById("driverName").value = driver.name;
  document.getElementById("licenseNumber").value = driver.licenseNumber;
  document.getElementById("driverUsername").value = driver.username || "";
  document.getElementById("driverStatus").value = driver.status;

  clearAlert("driverFormAlert");
  driverModal.show();
}

async function saveDriver(e) {
  e.preventDefault();
  clearAlert("driverFormAlert");

  const payload = {
    name: document.getElementById("driverName").value.trim(),
    licenseNumber: document.getElementById("licenseNumber").value.trim(),
    username: document.getElementById("driverUsername").value.trim() || null,
    status: document.getElementById("driverStatus").value
  };

  showSpinner();
  try {
    if (editingDriverId !== null) {
      await api.put(`/drivers/${editingDriverId}`, payload);
      showAlert("driversAlert", "Driver updated successfully.", "success");
    } else {
      await api.post("/drivers", payload);
      showAlert("driversAlert", "Driver created successfully.", "success");
    }
    driverModal.hide();
    editingDriverId = null;
    loadDrivers();
  } catch (error) {
    showAlert("driverFormAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

function openDriverStatusModal(id, currentStatus) {
  document.getElementById("statusDriverId").value = id;
  document.getElementById("newDriverStatus").value = currentStatus;
  driverStatusModal.show();
}

async function saveDriverStatus(e) {
  e.preventDefault();
  const id = document.getElementById("statusDriverId").value;
  const status = document.getElementById("newDriverStatus").value;

  showSpinner();
  try {
    await api.patch(`/drivers/${id}/status`, { status });
    showAlert("driversAlert", "Driver status updated.", "success");
    driverStatusModal.hide();
    loadDrivers();
  } catch (error) {
    showAlert("driversAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

function openDeleteDriverModal(id) {
  driverIdPendingDelete = id;
  driverDeleteModal.show();
}

async function confirmDeleteDriver() {
  if (!driverIdPendingDelete) return;
  showSpinner();
  try {
    await api.delete(`/drivers/${driverIdPendingDelete}`);
    showAlert("driversAlert", "Driver deleted successfully.", "success");
    loadDrivers();
  } catch (error) {
    showAlert("driversAlert", getErrorMessage(error));
  } finally {
    driverIdPendingDelete = null;
    driverDeleteModal.hide();
    hideSpinner();
  }
}
