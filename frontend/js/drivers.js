let driverModal;
let driverStatusModal;
let driverDeleteModal;
let editingDriverId = null;
let driverIdPendingDelete = null;
let currentDrivers = [];
let driverCurrentPage = 0;
const DRIVER_PAGE_SIZE = 20;

document.addEventListener("DOMContentLoaded", async () => {
  if (!requireAuth()) return;
  if (!(await syncSession())) return;
  initNavbar();

  const role = getRole();

  if (role === "DRIVER") {
    loadMyProfile();
    return;
  }

  driverModal = new bootstrap.Modal(document.getElementById("driverModal"));
  driverStatusModal = new bootstrap.Modal(document.getElementById("driverStatusModal"));
  driverDeleteModal = new bootstrap.Modal(document.getElementById("driverDeleteModal"));

  document.getElementById("addDriverBtn").addEventListener("click", openAddDriverModal);
  document.getElementById("driverForm").addEventListener("submit", saveDriver);
  document.getElementById("availableOnlyToggle").addEventListener("change", () => {
    const toggle = document.getElementById("availableOnlyToggle");
    const statusFilter = document.getElementById("driverStatusFilter");
    if (statusFilter) {
      statusFilter.value = toggle.checked ? "AVAILABLE" : "";
    }
    driverCurrentPage = 0;
    loadDrivers();
  });
  document.getElementById("driverStatusForm").addEventListener("submit", saveDriverStatus);
  document.getElementById("confirmDriverDeleteBtn").addEventListener("click", confirmDeleteDriver);

  const searchInput = document.getElementById("driverSearch");
  const statusFilter = document.getElementById("driverStatusFilter");
  const sortSelect = document.getElementById("driverSort");

  if (searchInput) {
    searchInput.addEventListener("input", () => {
      driverCurrentPage = 0;
      loadDrivers();
    });
  }
  if (statusFilter) {
    statusFilter.addEventListener("change", () => {
      const toggle = document.getElementById("availableOnlyToggle");
      if (toggle) toggle.checked = statusFilter.value === "AVAILABLE";
      driverCurrentPage = 0;
      loadDrivers();
    });
  }
  if (sortSelect) {
    sortSelect.addEventListener("change", () => {
      driverCurrentPage = 0;
      loadDrivers();
    });
  }

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
        <span class="badge ${statusBadgeClass(d.status)}">${escapeHtml(d.status)}</span>
      </div>`;
  } catch (error) {
    container.innerHTML = `<span class="text-danger">${getErrorMessage(error)}</span>`;
  }
}

async function loadDrivers() {
  showSpinner();
  clearAlert("driversAlert");

  const availableOnly = document.getElementById("availableOnlyToggle")?.checked;
  const q = (document.getElementById("driverSearch")?.value || "").trim();
  const statusVal = document.getElementById("driverStatusFilter")?.value || (availableOnly ? "AVAILABLE" : "");
  const sort = document.getElementById("driverSort")?.value || "id,asc";

  const params = { page: driverCurrentPage, size: DRIVER_PAGE_SIZE, sort };
  if (q) params.q = q;
  if (statusVal) params.status = statusVal;

  try {
    const response = await api.get("/drivers", { params });
    currentDrivers = response.data;
    const total = Number(response.headers["x-total-count"] ?? currentDrivers.length);
    renderDriversTable(currentDrivers);
    if (typeof renderPaginationBar === "function") {
      renderPaginationBar("driversPagination", driverCurrentPage, DRIVER_PAGE_SIZE, total, (newPage) => {
        driverCurrentPage = newPage;
        loadDrivers();
      });
    }
  } catch (error) {
    showAlert("driversAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

function renderDriversTable(drivers) {
  const role = getRole();
  const tbody = document.getElementById("driversTableBody");
  tbody.innerHTML = "";

  if (!drivers || drivers.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-4">No drivers found.</td></tr>`;
    return;
  }

  drivers.forEach((d) => {
    const row = document.createElement("tr");
    const isOnTrip = d.status === "ON_TRIP";
    const adminActions = role === "ADMIN" ? `
        <button class="btn btn-sm btn-outline-primary me-1" onclick="editDriver(${d.id})"><i class="bi bi-pencil-square"></i> Edit</button>
        <button class="btn btn-sm btn-outline-danger" onclick="openDeleteDriverModal(${d.id})" ${isOnTrip ? "disabled title='Cannot delete driver on an active trip'" : ""}><i class="bi bi-trash"></i> Delete</button>` : "";
    const dispatcherActions = role === "DISPATCHER" ? (
      isOnTrip
        ? `<span class="text-muted small">On Trip</span>`
        : `<button class="btn btn-sm btn-outline-secondary" onclick="openDriverStatusModal(${d.id}, '${escapeHtml(d.status)}')"><i class="bi bi-arrow-repeat"></i> Status</button>`
    ) : "";

    const usernameSub = d.username ? `<div class="text-muted small"><i class="bi bi-person me-1"></i>${escapeHtml(d.username)}</div>` : "";

    row.innerHTML = `
      <td>${d.id}</td>
      <td>${escapeHtml(d.name)}${usernameSub}</td>
      <td>${escapeHtml(d.licenseNumber)}</td>
      <td><span class="badge ${statusBadgeClass(d.status)}">${escapeHtml(d.status)}</span></td>
      <td class="text-end">${adminActions}${dispatcherActions}</td>`;
    tbody.appendChild(row);
  });
}

function setDriverStatusOptions(selectEl, currentStatus) {
  if (currentStatus === "ON_TRIP") {
    selectEl.innerHTML = `<option value="ON_TRIP" selected>ON_TRIP</option>`;
    selectEl.disabled = true;
    document.getElementById("driverStatusHelp")?.classList.remove("d-none");
  } else {
    selectEl.innerHTML = `<option value="AVAILABLE" selected>AVAILABLE</option>`;
    selectEl.disabled = false;
    document.getElementById("driverStatusHelp")?.classList.add("d-none");
  }
}

function openAddDriverModal() {
  editingDriverId = null;
  document.getElementById("driverForm").reset();
  document.getElementById("driverId").value = "";
  document.getElementById("driverModalTitle").textContent = "Add Driver";
  setDriverStatusOptions(document.getElementById("driverStatus"), "AVAILABLE");
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
  setDriverStatusOptions(document.getElementById("driverStatus"), driver.status);

  clearAlert("driverFormAlert");
  driverModal.show();
}

async function saveDriver(e) {
  e.preventDefault();
  clearAlert("driverFormAlert");

  const payload = {
    name: document.getElementById("driverName").value.trim(),
    licenseNumber: document.getElementById("licenseNumber").value.trim().toUpperCase(),
    username: document.getElementById("driverUsername").value.trim() || null,
    status: document.getElementById("driverStatus").value
  };

  const submitBtn = document.querySelector('#driverForm button[type="submit"]');
  await withButtonLoading(submitBtn, async () => {
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
    }
  });
}

function openDriverStatusModal(id, currentStatus) {
  clearAlert("driverStatusFormAlert");
  document.getElementById("statusDriverId").value = id;
  document.getElementById("newDriverStatus").value = "AVAILABLE";
  driverStatusModal.show();
}

async function saveDriverStatus(e) {
  e.preventDefault();
  clearAlert("driverStatusFormAlert");
  const id = document.getElementById("statusDriverId").value;
  const status = document.getElementById("newDriverStatus").value;

  const submitBtn = document.querySelector('#driverStatusForm button[type="submit"]');
  await withButtonLoading(submitBtn, async () => {
    try {
      await api.patch(`/drivers/${id}/status`, { status });
      showAlert("driversAlert", "Driver status updated.", "success");
      driverStatusModal.hide();
      loadDrivers();
    } catch (error) {
      showAlert("driverStatusFormAlert", getErrorMessage(error));
    }
  });
}

function openDeleteDriverModal(id) {
  driverIdPendingDelete = id;
  driverDeleteModal.show();
}

async function confirmDeleteDriver() {
  if (!driverIdPendingDelete) return;
  const btn = document.getElementById("confirmDriverDeleteBtn");
  await withButtonLoading(btn, async () => {
    try {
      await api.delete(`/drivers/${driverIdPendingDelete}`);
      showAlert("driversAlert", "Driver deleted successfully.", "success");
      loadDrivers();
    } catch (error) {
      showAlert("driversAlert", getErrorMessage(error));
    } finally {
      driverIdPendingDelete = null;
      driverDeleteModal.hide();
    }
  });
}
