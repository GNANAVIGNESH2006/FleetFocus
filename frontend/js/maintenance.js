let maintenanceModal;
let driverVehicleId = null;

document.addEventListener("DOMContentLoaded", () => {
  requireAuth();
  initNavbar();

  const role = localStorage.getItem("role");
  maintenanceModal = new bootstrap.Modal(document.getElementById("maintenanceModal"));
  document.getElementById("maintenanceForm").addEventListener("submit", saveMaintenance);

  if (role === "DRIVER") {
    document.getElementById("driverReportBtn").addEventListener("click", openDriverReportModal);
  } else {
    document.getElementById("addMaintenanceBtn").addEventListener("click", openAddMaintenanceModal);
    loadMaintenanceLogs();
  }
});

async function loadMaintenanceLogs() {
  showSpinner();
  clearAlert("maintenanceAlert");
  try {
    const response = await api.get("/maintenance");
    renderMaintenanceTable(response.data);
  } catch (error) {
    showAlert("maintenanceAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

function renderMaintenanceTable(logs) {
  const role = localStorage.getItem("role");
  const tbody = document.getElementById("maintenanceTableBody");
  tbody.innerHTML = "";

  if (!logs || logs.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted py-4">No maintenance logs found.</td></tr>`;
    return;
  }

  logs.forEach((log) => {
    const row = document.createElement("tr");
    const vehicleLabel = log.vehicle ? `${escapeHtml(log.vehicle.licensePlate)} (${escapeHtml(log.vehicle.model)})` : "-";
    const deleteAction = role === "ADMIN"
      ? `<button class="btn btn-danger btn-sm" onclick="deleteMaintenance(${log.id})"><i class="bi bi-trash"></i> Delete</button>`
      : `<span class="text-muted small">&mdash;</span>`;

    row.innerHTML = `
      <td>${log.id}</td>
      <td>${vehicleLabel}</td>
      <td>${formatDate(log.serviceDate)}</td>
      <td>${escapeHtml(log.description)}</td>
      <td>$${Number(log.cost).toFixed(2)}</td>
      <td class="text-end">${deleteAction}</td>`;
    tbody.appendChild(row);
  });
}

async function openAddMaintenanceModal() {
  document.getElementById("maintenanceForm").reset();
  clearAlert("maintenanceFormAlert");

  const select = document.getElementById("maintenanceVehicleId");
  select.innerHTML = `<option value="">Loading vehicles...</option>`;

  maintenanceModal.show();

  try {
    const response = await api.get("/vehicles");
    const vehicles = response.data;
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

  try {
    const meVehicle = await api.get("/vehicles/me");
    driverVehicleId = meVehicle.data.id;
    maintenanceModal.show();
  } catch (error) {
    showAlert("maintenanceAlert", getErrorMessage(error) || "No vehicle assigned right now.");
  }
}

async function saveMaintenance(e) {
  e.preventDefault();
  clearAlert("maintenanceFormAlert");

  const role = localStorage.getItem("role");
  const vehicleId = role === "DRIVER" ? driverVehicleId : document.getElementById("maintenanceVehicleId").value;

  if (!vehicleId) {
    showAlert("maintenanceFormAlert", "Please select a vehicle.");
    return;
  }

  const payload = {
    serviceDate: document.getElementById("serviceDate").value,
    description: document.getElementById("description").value.trim(),
    cost: parseFloat(document.getElementById("cost").value)
  };

  showSpinner();
  try {
    await api.post(`/maintenance/log/${vehicleId}`, payload);
    maintenanceModal.hide();
    showAlert("maintenanceAlert", "Maintenance log added successfully.", "success");
    if (role !== "DRIVER") loadMaintenanceLogs();
  } catch (error) {
    showAlert("maintenanceFormAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

async function deleteMaintenance(id) {
  const confirmed = confirm("Are you sure you want to delete this maintenance log?");
  if (!confirmed) return;

  showSpinner();
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
