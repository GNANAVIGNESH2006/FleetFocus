let vehicleModal;
let deleteModal;
let statusModal;
let vehicleIdPendingDelete = null;

document.addEventListener("DOMContentLoaded", () => {
  requireAuth();
  initNavbar();

  const role = localStorage.getItem("role");

  vehicleModal = new bootstrap.Modal(document.getElementById("vehicleModal"));
  deleteModal = new bootstrap.Modal(document.getElementById("deleteModal"));
  statusModal = new bootstrap.Modal(document.getElementById("statusModal"));

  document.getElementById("addVehicleBtn").addEventListener("click", openAddModal);
  document.getElementById("vehicleForm").addEventListener("submit", saveVehicle);
  document.getElementById("confirmDeleteBtn").addEventListener("click", confirmDelete);
  document.getElementById("statusForm").addEventListener("submit", saveStatus);

  if (role === "DRIVER") {
    loadMyVehicle();
  } else {
    loadVehicles();
  }
});

async function loadVehicles() {
  showSpinner();
  clearAlert("vehiclesAlert");
  try {
    const response = await api.get("/vehicles");
    renderVehiclesTable(response.data);
  } catch (error) {
    showAlert("vehiclesAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

async function loadMyVehicle() {
  const container = document.getElementById("myVehicleCard");
  try {
    const response = await api.get("/vehicles/me");
    const v = response.data;
    container.innerHTML = `
      <div class="d-flex justify-content-between align-items-center">
        <div>
          <div class="fw-bold">${escapeHtml(v.licensePlate)} &middot; ${escapeHtml(v.model)}</div>
          <div class="text-muted small">VIN: ${escapeHtml(v.vin)}</div>
          <div class="text-muted small">Mileage: ${v.currentMileage ?? "-"}</div>
        </div>
        <span class="badge ${statusBadgeClass(v.status)}">${v.status}</span>
      </div>`;
  } catch (error) {
    container.innerHTML = `<span class="text-muted">${getErrorMessage(error) || "No vehicle assigned right now."}</span>`;
  }
}

function renderVehiclesTable(vehicles) {
  const role = localStorage.getItem("role");
  const tbody = document.getElementById("vehiclesTableBody");
  tbody.innerHTML = "";

  if (!vehicles || vehicles.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted py-4">No vehicles found.</td></tr>`;
    return;
  }

  vehicles.forEach((v) => {
    const row = document.createElement("tr");
    const adminActions = role === "ADMIN" ? `
        <button class="btn btn-sm btn-outline-primary me-1" onclick="openEditModal(${v.id})"><i class="bi bi-pencil-square"></i> Edit</button>
        <button class="btn btn-sm btn-outline-danger" onclick="openDeleteModal(${v.id})"><i class="bi bi-trash"></i> Delete</button>` : "";
    const dispatcherActions = role === "DISPATCHER" ? `
        <button class="btn btn-sm btn-outline-secondary" onclick="openStatusModal(${v.id}, '${v.status}')"><i class="bi bi-arrow-repeat"></i> Status</button>` : "";

    row.innerHTML = `
      <td>${v.id}</td>
      <td>${escapeHtml(v.vin)}</td>
      <td>${escapeHtml(v.licensePlate)}</td>
      <td>${escapeHtml(v.model)}</td>
      <td><span class="badge ${statusBadgeClass(v.status)}">${v.status}</span></td>
      <td>${v.currentMileage ?? "-"}</td>
      <td class="text-end">${adminActions}${dispatcherActions}</td>`;
    tbody.appendChild(row);
  });
}

function openAddModal() {
  document.getElementById("vehicleForm").reset();
  document.getElementById("vehicleId").value = "";
  document.getElementById("vehicleModalLabel").textContent = "Add Vehicle";
  clearAlert("vehicleFormAlert");
  vehicleModal.show();
}

async function openEditModal(id) {
  clearAlert("vehiclesAlert");
  showSpinner();
  try {
    const response = await api.get(`/vehicles/${id}`);
    const v = response.data;

    document.getElementById("vehicleId").value = v.id;
    document.getElementById("vin").value = v.vin;
    document.getElementById("licensePlate").value = v.licensePlate;
    document.getElementById("model").value = v.model;
    document.getElementById("status").value = v.status;
    document.getElementById("currentMileage").value = v.currentMileage ?? "";

    document.getElementById("vehicleModalLabel").textContent = "Edit Vehicle";
    clearAlert("vehicleFormAlert");
    vehicleModal.show();
  } catch (error) {
    showAlert("vehiclesAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

async function saveVehicle(e) {
  e.preventDefault();
  clearAlert("vehicleFormAlert");

  const id = document.getElementById("vehicleId").value;
  const payload = {
    vin: document.getElementById("vin").value.trim(),
    licensePlate: document.getElementById("licensePlate").value.trim(),
    model: document.getElementById("model").value.trim(),
    status: document.getElementById("status").value,
    currentMileage: parseFloat(document.getElementById("currentMileage").value) || 0
  };

  showSpinner();
  try {
    if (id) {
      await api.put(`/vehicles/${id}`, payload);
      showAlert("vehiclesAlert", "Vehicle updated successfully.", "success");
    } else {
      await api.post("/vehicles", payload);
      showAlert("vehiclesAlert", "Vehicle created successfully.", "success");
    }
    vehicleModal.hide();
    loadVehicles();
  } catch (error) {
    showAlert("vehicleFormAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

function openStatusModal(id, currentStatus) {
  document.getElementById("statusVehicleId").value = id;
  document.getElementById("newStatus").value = currentStatus;
  statusModal.show();
}

async function saveStatus(e) {
  e.preventDefault();
  const id = document.getElementById("statusVehicleId").value;
  const status = document.getElementById("newStatus").value;

  showSpinner();
  try {
    await api.patch(`/vehicles/${id}/status`, { status });
    showAlert("vehiclesAlert", "Vehicle status updated.", "success");
    statusModal.hide();
    loadVehicles();
  } catch (error) {
    showAlert("vehiclesAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

function openDeleteModal(id) {
  vehicleIdPendingDelete = id;
  deleteModal.show();
}

async function confirmDelete() {
  if (!vehicleIdPendingDelete) return;
  showSpinner();
  try {
    await api.delete(`/vehicles/${vehicleIdPendingDelete}`);
    showAlert("vehiclesAlert", "Vehicle deleted successfully.", "success");
    loadVehicles();
  } catch (error) {
    showAlert("vehiclesAlert", getErrorMessage(error));
  } finally {
    vehicleIdPendingDelete = null;
    deleteModal.hide();
    hideSpinner();
  }
}
