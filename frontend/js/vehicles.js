let vehicleModal;
let deleteModal;
let statusModal;
let vehicleIdPendingDelete = null;
let vehicleCurrentPage = 0;
const VEHICLE_PAGE_SIZE = 20;

document.addEventListener("DOMContentLoaded", async () => {
  if (!requireAuth()) return;
  if (!(await syncSession())) return;
  initNavbar();

  const role = getRole();

  vehicleModal = new bootstrap.Modal(document.getElementById("vehicleModal"));
  deleteModal = new bootstrap.Modal(document.getElementById("deleteModal"));
  statusModal = new bootstrap.Modal(document.getElementById("statusModal"));

  document.getElementById("addVehicleBtn").addEventListener("click", openAddModal);
  document.getElementById("vehicleForm").addEventListener("submit", saveVehicle);
  document.getElementById("confirmDeleteBtn").addEventListener("click", confirmDelete);
  document.getElementById("statusForm").addEventListener("submit", saveStatus);

  const searchInput = document.getElementById("vehicleSearch");
  const statusFilter = document.getElementById("vehicleStatusFilter");
  const sortSelect = document.getElementById("vehicleSort");

  if (searchInput) {
    searchInput.addEventListener("input", () => {
      vehicleCurrentPage = 0;
      loadVehicles();
    });
  }
  if (statusFilter) {
    statusFilter.addEventListener("change", () => {
      vehicleCurrentPage = 0;
      loadVehicles();
    });
  }
  if (sortSelect) {
    sortSelect.addEventListener("change", () => {
      vehicleCurrentPage = 0;
      loadVehicles();
    });
  }

  if (role === "DRIVER") {
    loadMyVehicle();
  } else {
    loadVehicles();
  }
});

async function loadVehicles() {
  showSpinner();
  clearAlert("vehiclesAlert");

  const q = (document.getElementById("vehicleSearch")?.value || "").trim();
  const status = document.getElementById("vehicleStatusFilter")?.value || "";
  const sort = document.getElementById("vehicleSort")?.value || "id,asc";

  const params = { page: vehicleCurrentPage, size: VEHICLE_PAGE_SIZE, sort };
  if (q) params.q = q;
  if (status) params.status = status;

  try {
    const response = await api.get("/vehicles", { params });
    const total = Number(response.headers["x-total-count"] ?? response.data.length);
    renderVehiclesTable(response.data);
    if (typeof renderPaginationBar === "function") {
      renderPaginationBar("vehiclesPagination", vehicleCurrentPage, VEHICLE_PAGE_SIZE, total, (newPage) => {
        vehicleCurrentPage = newPage;
        loadVehicles();
      });
    }
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
        <span class="badge ${statusBadgeClass(v.status)}">${escapeHtml(v.status)}</span>
      </div>`;
  } catch (error) {
    container.innerHTML = `<span class="text-muted">${getErrorMessage(error) || "No vehicle assigned right now."}</span>`;
  }
}

function renderVehiclesTable(vehicles) {
  const role = getRole();
  const tbody = document.getElementById("vehiclesTableBody");
  tbody.innerHTML = "";

  if (!vehicles || vehicles.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted py-4">No vehicles found.</td></tr>`;
    return;
  }

  vehicles.forEach((v) => {
    const row = document.createElement("tr");
    const isOnTrip = v.status === "ON_TRIP";
    const adminActions = role === "ADMIN" ? `
        <button class="btn btn-sm btn-outline-primary me-1" onclick="openEditModal(${v.id})"><i class="bi bi-pencil-square"></i> Edit</button>
        <button class="btn btn-sm btn-outline-danger" onclick="openDeleteModal(${v.id})" ${isOnTrip ? "disabled title='Cannot delete vehicle on an active trip'" : ""}><i class="bi bi-trash"></i> Delete</button>` : "";
    const dispatcherActions = role === "DISPATCHER" ? (
      isOnTrip
        ? `<span class="text-muted small">On Trip</span>`
        : `<button class="btn btn-sm btn-outline-secondary" onclick="openStatusModal(${v.id}, '${escapeHtml(v.status)}')"><i class="bi bi-arrow-repeat"></i> Status</button>`
    ) : "";

    row.innerHTML = `
      <td>${v.id}</td>
      <td>${escapeHtml(v.vin)}</td>
      <td>${escapeHtml(v.licensePlate)}</td>
      <td>${escapeHtml(v.model)}</td>
      <td><span class="badge ${statusBadgeClass(v.status)}">${escapeHtml(v.status)}</span></td>
      <td>${v.currentMileage ?? "-"}</td>
      <td class="text-end">${adminActions}${dispatcherActions}</td>`;
    tbody.appendChild(row);
  });
}

function setStatusSelectOptions(selectEl, currentStatus) {
  if (currentStatus === "ON_TRIP") {
    selectEl.innerHTML = `<option value="ON_TRIP" selected>ON_TRIP</option>`;
    selectEl.disabled = true;
    document.getElementById("vehicleStatusHelp")?.classList.remove("d-none");
  } else {
    selectEl.innerHTML = `
      <option value="AVAILABLE">AVAILABLE</option>
      <option value="UNDER_MAINTENANCE">UNDER_MAINTENANCE</option>`;
    selectEl.disabled = false;
    selectEl.value = currentStatus || "AVAILABLE";
    document.getElementById("vehicleStatusHelp")?.classList.add("d-none");
  }
}

function openAddModal() {
  document.getElementById("vehicleForm").reset();
  document.getElementById("vehicleId").value = "";
  document.getElementById("vehicleModalLabel").textContent = "Add Vehicle";
  setStatusSelectOptions(document.getElementById("status"), "AVAILABLE");
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
    setStatusSelectOptions(document.getElementById("status"), v.status);
    document.getElementById("currentMileage").value = v.currentMileage ?? 0;

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
  const vin = document.getElementById("vin").value.trim().toUpperCase();
  const licensePlate = document.getElementById("licensePlate").value.trim().toUpperCase();
  const model = document.getElementById("model").value.trim();
  const status = document.getElementById("status").value;
  const rawMileage = document.getElementById("currentMileage").value.trim();

  if (rawMileage === "") {
    showAlert("vehicleFormAlert", "Current mileage is required.");
    return;
  }
  const currentMileage = Number(rawMileage);
  if (!Number.isFinite(currentMileage) || currentMileage < 0 || currentMileage > 2000000) {
    showAlert("vehicleFormAlert", "Mileage must be a valid number between 0 and 2,000,000.");
    return;
  }

  const payload = {
    vin,
    licensePlate,
    model,
    status,
    currentMileage
  };

  const submitBtn = document.querySelector('#vehicleForm button[type="submit"]');
  await withButtonLoading(submitBtn, async () => {
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
    }
  });
}

function openStatusModal(id, currentStatus) {
  clearAlert("statusFormAlert");
  document.getElementById("statusVehicleId").value = id;
  document.getElementById("newStatus").value = currentStatus === "UNDER_MAINTENANCE" ? "UNDER_MAINTENANCE" : "AVAILABLE";
  statusModal.show();
}

async function saveStatus(e) {
  e.preventDefault();
  clearAlert("statusFormAlert");
  const id = document.getElementById("statusVehicleId").value;
  const status = document.getElementById("newStatus").value;

  const submitBtn = document.querySelector('#statusForm button[type="submit"]');
  await withButtonLoading(submitBtn, async () => {
    try {
      await api.patch(`/vehicles/${id}/status`, { status });
      showAlert("vehiclesAlert", "Vehicle status updated.", "success");
      statusModal.hide();
      loadVehicles();
    } catch (error) {
      showAlert("statusFormAlert", getErrorMessage(error));
    }
  });
}

function openDeleteModal(id) {
  vehicleIdPendingDelete = id;
  deleteModal.show();
}

async function confirmDelete() {
  if (!vehicleIdPendingDelete) return;
  const btn = document.getElementById("confirmDeleteBtn");
  await withButtonLoading(btn, async () => {
    try {
      await api.delete(`/vehicles/${vehicleIdPendingDelete}`);
      showAlert("vehiclesAlert", "Vehicle deleted successfully.", "success");
      loadVehicles();
    } catch (error) {
      showAlert("vehiclesAlert", getErrorMessage(error));
    } finally {
      vehicleIdPendingDelete = null;
      deleteModal.hide();
    }
  });
}
