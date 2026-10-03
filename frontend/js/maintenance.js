let maintenanceModal;
let driverVehicleId = null;
let maintenanceCurrentPage = 0;
const MAINTENANCE_PAGE_SIZE = 20;

document.addEventListener("DOMContentLoaded", async () => {
  if (!requireAuth()) return;
  if (!(await syncSession())) return;
  initNavbar();

  const role = getRole();
  maintenanceModal = new bootstrap.Modal(document.getElementById("maintenanceModal"));
  document.getElementById("maintenanceForm").addEventListener("submit", saveMaintenance);

  const searchInput = document.getElementById("maintenanceSearch");
  const statusFilter = document.getElementById("maintenanceStatusFilter");
  const sortSelect = document.getElementById("maintenanceSort");

  if (searchInput) {
    searchInput.addEventListener("input", () => {
      maintenanceCurrentPage = 0;
      loadMaintenanceLogs();
    });
  }
  if (statusFilter) {
    statusFilter.addEventListener("change", () => {
      maintenanceCurrentPage = 0;
      loadMaintenanceLogs();
    });
  }
  if (sortSelect) {
    sortSelect.addEventListener("change", () => {
      maintenanceCurrentPage = 0;
      loadMaintenanceLogs();
    });
  }

  if (role === "DRIVER") {
    document.getElementById("driverReportBtn").addEventListener("click", openDriverReportModal);
  } else {
    document.getElementById("addMaintenanceBtn").addEventListener("click", openAddMaintenanceModal);
    loadMaintenanceLogs();
  }
});

function getTodayIsoString() {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(now.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

async function loadMaintenanceLogs() {
  showSpinner();
  clearAlert("maintenanceAlert");

  const q = (document.getElementById("maintenanceSearch")?.value || "").trim();
  const status = document.getElementById("maintenanceStatusFilter")?.value || "";
  const sort = document.getElementById("maintenanceSort")?.value || "id,desc";

  const params = { page: maintenanceCurrentPage, size: MAINTENANCE_PAGE_SIZE, sort };
  if (q) params.q = q;
  if (status) params.status = status;

  try {
    const response = await api.get("/maintenance", { params });
    const total = Number(response.headers["x-total-count"] ?? response.data.length);
    renderMaintenanceTable(response.data);
    if (typeof renderPaginationBar === "function") {
      renderPaginationBar("maintenancePagination", maintenanceCurrentPage, MAINTENANCE_PAGE_SIZE, total, (newPage) => {
        maintenanceCurrentPage = newPage;
        loadMaintenanceLogs();
      });
    }
  } catch (error) {
    showAlert("maintenanceAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

function renderMaintenanceTable(logs) {
  const role = getRole();
  const tbody = document.getElementById("maintenanceTableBody");
  tbody.innerHTML = "";

  if (!logs || logs.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted py-4">No maintenance logs found.</td></tr>`;
    return;
  }

  logs.forEach((log) => {
    const row = document.createElement("tr");
    const vehicleLabel = log.vehicle ? `${escapeHtml(log.vehicle.licensePlate)} (${escapeHtml(log.vehicle.model)})` : "-";
    const status = log.status || "IN_PROGRESS";
    const statusBadge = `<span class="badge ${statusBadgeClass(status)}">${escapeHtml(status)}</span>`;
    const completedDateStr = log.completedDate ? formatDate(log.completedDate) : "&mdash;";

    const actions = [];
    if (status === "IN_PROGRESS" && (role === "ADMIN" || role === "DISPATCHER")) {
      actions.push(
        `<button class="btn btn-outline-success btn-sm me-1" onclick="completeMaintenance(${log.id})"><i class="bi bi-check2-circle"></i> Complete</button>`
      );
    }
    if (role === "ADMIN") {
      actions.push(
        `<button class="btn btn-outline-danger btn-sm" onclick="deleteMaintenance(${log.id})"><i class="bi bi-trash"></i> Delete</button>`
      );
    }
    const actionHtml = actions.length > 0 ? actions.join("") : `<span class="text-muted small">&mdash;</span>`;

    row.innerHTML = `
      <td>${log.id}</td>
      <td>${vehicleLabel}</td>
      <td>${formatDate(log.serviceDate)}</td>
      <td>${completedDateStr}</td>
      <td>${escapeHtml(log.description)}</td>
      <td>${formatCurrency(log.cost)}</td>
      <td>${statusBadge}</td>
      <td class="text-end">${actionHtml}</td>`;
    tbody.appendChild(row);
  });
}

async function openAddMaintenanceModal() {
  document.getElementById("maintenanceForm").reset();
  clearAlert("maintenanceFormAlert");

  const role = getRole();
  const select = document.getElementById("maintenanceVehicleId");
  const serviceDateEl = document.getElementById("serviceDate");
  const todayIso = getTodayIsoString();

  select.required = role !== "DRIVER";
  if (serviceDateEl) {
    serviceDateEl.max = todayIso;
    serviceDateEl.value = todayIso;
  }

  select.innerHTML = `<option value="">Loading vehicles...</option>`;
  maintenanceModal.show();

  try {
    const response = await api.get("/vehicles");
    const vehicles = response.data || [];
    select.innerHTML = vehicles.length
      ? vehicles.map((v) => `<option value="${v.id}">${escapeHtml(v.licensePlate)} - ${escapeHtml(v.model)}</option>`).join("")
      : `<option value="">No vehicles available</option>`;
  } catch (error) {
    select.innerHTML = `<option value="">Failed to load vehicles</option>`;
    showAlert("maintenanceFormAlert", getErrorMessage(error));
  }
}

async function openDriverReportModal() {
  document.getElementById("maintenanceForm").reset();
  clearAlert("maintenanceFormAlert");
  clearAlert("maintenanceAlert");

  const select = document.getElementById("maintenanceVehicleId");
  const assignedDisplay = document.getElementById("driverAssignedVehicleDisplay");
  const serviceDateEl = document.getElementById("serviceDate");
  const todayIso = getTodayIsoString();

  if (select) select.required = false;
  if (serviceDateEl) {
    serviceDateEl.max = todayIso;
    serviceDateEl.value = todayIso;
  }

  try {
    const meVehicle = await api.get("/vehicles/me");
    const v = meVehicle.data;
    driverVehicleId = v.id;
    if (assignedDisplay) {
      assignedDisplay.value = `${v.licensePlate} - ${v.model}`;
    }
    maintenanceModal.show();
  } catch (error) {
    driverVehicleId = null;
    showAlert("maintenanceAlert", getErrorMessage(error) || "No vehicle assigned right now.");
  }
}

async function saveMaintenance(e) {
  e.preventDefault();
  clearAlert("maintenanceFormAlert");

  const role = getRole();
  const vehicleId = role === "DRIVER" ? driverVehicleId : document.getElementById("maintenanceVehicleId").value;

  if (!vehicleId) {
    showAlert("maintenanceFormAlert", "Please select a vehicle.");
    return;
  }

  const serviceDate = document.getElementById("serviceDate").value;
  const description = document.getElementById("description").value.trim();
  const rawCost = document.getElementById("cost").value.trim();

  if (!serviceDate) {
    showAlert("maintenanceFormAlert", "Service date is required.");
    return;
  }
  if (!description) {
    showAlert("maintenanceFormAlert", "Maintenance description is required.");
    return;
  }
  if (rawCost === "") {
    showAlert("maintenanceFormAlert", "Maintenance cost is required.");
    return;
  }

  const cost = Number(rawCost);
  if (!Number.isFinite(cost) || cost < 0 || cost > 9999999999.99) {
    showAlert("maintenanceFormAlert", "Cost must be a valid non-negative number.");
    return;
  }

  const payload = {
    serviceDate,
    description,
    cost
  };

  const submitBtn = document.getElementById("maintenanceSubmitBtn");
  await withButtonLoading(submitBtn, async () => {
    try {
      await api.post(`/maintenance/log/${vehicleId}`, payload);
      maintenanceModal.hide();
      showAlert("maintenanceAlert", "Maintenance log added successfully.", "success");
      if (role !== "DRIVER") loadMaintenanceLogs();
    } catch (error) {
      showAlert("maintenanceFormAlert", getErrorMessage(error));
    }
  });
}

async function completeMaintenance(id) {
  showSpinner();
  clearAlert("maintenanceAlert");
  try {
    await api.put(`/maintenance/${id}/complete`);
    showAlert("maintenanceAlert", "Maintenance marked as completed.", "success");
    loadMaintenanceLogs();
  } catch (error) {
    showAlert("maintenanceAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

async function deleteMaintenance(id) {
  const confirmed = typeof confirmActionModal === "function"
    ? await confirmActionModal({
        title: "Confirm Delete",
        message: "Are you sure you want to delete this maintenance log? This action cannot be undone.",
        confirmText: "Delete",
        confirmClass: "btn-danger"
      })
    : false;
  if (!confirmed) return;

  showSpinner();
  clearAlert("maintenanceAlert");
  try {
    await api.delete(`/maintenance/${id}`);
    showAlert("maintenanceAlert", "Maintenance log deleted successfully.", "success");
    loadMaintenanceLogs();
  } catch (error) {
    showAlert("maintenanceAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}
