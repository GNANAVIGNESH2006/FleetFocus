let startTripModal;
let endTripModal;
let myDriverId = null;
let tripCurrentPage = 0;
const TRIP_PAGE_SIZE = 20;

document.addEventListener("DOMContentLoaded", async () => {
  if (!requireAuth()) return;
  if (!(await syncSession())) return;
  initNavbar();

  startTripModal = new bootstrap.Modal(document.getElementById("startTripModal"));
  endTripModal = new bootstrap.Modal(document.getElementById("endTripModal"));

  document.getElementById("startTripBtn").addEventListener("click", openStartTripModal);
  document.getElementById("startTripForm").addEventListener("submit", startTrip);
  document.getElementById("endTripForm").addEventListener("submit", endTrip);

  const searchInput = document.getElementById("tripSearch");
  const statusFilter = document.getElementById("tripStatusFilter");
  const sortSelect = document.getElementById("tripSort");

  if (searchInput) {
    searchInput.addEventListener("input", () => {
      tripCurrentPage = 0;
      loadTrips();
    });
  }
  if (statusFilter) {
    statusFilter.addEventListener("change", () => {
      tripCurrentPage = 0;
      loadTrips();
    });
  }
  if (sortSelect) {
    sortSelect.addEventListener("change", () => {
      tripCurrentPage = 0;
      loadTrips();
    });
  }

  loadTrips();
});

async function loadTrips() {
  showSpinner();
  clearAlert("tripsAlert");
  const role = getRole();
  const endpoint = role === "DRIVER" ? "/trips/me" : "/trips";

  const q = (document.getElementById("tripSearch")?.value || "").trim();
  const status = document.getElementById("tripStatusFilter")?.value || "";
  const sort = document.getElementById("tripSort")?.value || "id,desc";

  const params = { page: tripCurrentPage, size: TRIP_PAGE_SIZE, sort };
  if (q) params.q = q;
  if (status) params.status = status;

  try {
    const response = await api.get(endpoint, { params });
    const total = Number(response.headers["x-total-count"] ?? response.data.length);
    renderTripsTable(response.data);
    if (typeof renderPaginationBar === "function") {
      renderPaginationBar("tripsPagination", tripCurrentPage, TRIP_PAGE_SIZE, total, (newPage) => {
        tripCurrentPage = newPage;
        loadTrips();
      });
    }
  } catch (error) {
    showAlert("tripsAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}

function renderTripsTable(trips) {
  const tbody = document.getElementById("tripsTableBody");
  tbody.innerHTML = "";

  if (!trips || trips.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted py-4">No trips found.</td></tr>`;
    return;
  }

  trips.forEach((t) => {
    const vehicleLabel = t.vehicle ? `${escapeHtml(t.vehicle.licensePlate)} (${escapeHtml(t.vehicle.model)})` : "-";
    const driverLabel = t.driver ? escapeHtml(t.driver.name) : "-";

    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${t.id}</td>
      <td>${vehicleLabel}</td>
      <td>${driverLabel}</td>
      <td>${formatDateTime(t.startTime)}</td>
      <td>${t.endTime ? formatDateTime(t.endTime) : "-"}</td>
      <td>${t.distanceCovered ?? "-"}</td>
      <td><span class="badge ${statusBadgeClass(t.status)}">${escapeHtml(t.status)}</span></td>
      <td class="text-end">
        ${
          t.status === "ACTIVE"
            ? `<button class="btn btn-sm btn-outline-success" onclick="openEndTripModal(${t.id})"><i class="bi bi-flag-fill"></i> End Trip</button>`
            : `<span class="text-muted small">&mdash;</span>`
        }
      </td>`;
    tbody.appendChild(row);
  });
}

async function openStartTripModal() {
  document.getElementById("startTripForm").reset();
  clearAlert("startTripFormAlert");

  const role = getRole();
  const vehicleSelect = document.getElementById("tripVehicleId");
  const driverSelect = document.getElementById("tripDriverId");
  const submitBtn = document.getElementById("startTripSubmitBtn");

  driverSelect.required = role !== "DRIVER";
  if (submitBtn) submitBtn.disabled = false;
  myDriverId = null;

  vehicleSelect.innerHTML = `<option value="">Loading vehicles...</option>`;
  startTripModal.show();

  try {
    const vehiclesRes = await api.get("/vehicles/available");
    const vehicles = vehiclesRes.data || [];
    vehicleSelect.innerHTML = vehicles.length
      ? vehicles.map((v) => `<option value="${v.id}">${escapeHtml(v.licensePlate)} - ${escapeHtml(v.model)}</option>`).join("")
      : `<option value="">No available vehicles</option>`;

    if (vehicles.length === 0 && submitBtn) {
      submitBtn.disabled = true;
    }

    if (role !== "DRIVER") {
      driverSelect.innerHTML = `<option value="">Loading drivers...</option>`;
      const driversRes = await api.get("/drivers/available");
      const drivers = driversRes.data || [];
      driverSelect.innerHTML = drivers.length
        ? drivers.map((d) => `<option value="${d.id}">${escapeHtml(d.name)} (${escapeHtml(d.licenseNumber)})</option>`).join("")
        : `<option value="">No available drivers</option>`;
      if (drivers.length === 0 && submitBtn) {
        submitBtn.disabled = true;
      }
    } else {
      try {
        const meRes = await api.get("/drivers/me");
        myDriverId = meRes.data.id;
      } catch (meError) {
        myDriverId = null;
        if (submitBtn) submitBtn.disabled = true;
        showAlert("startTripFormAlert", "No driver profile is linked to your account");
      }
    }
  } catch (error) {
    vehicleSelect.innerHTML = `<option value="">Failed to load vehicles</option>`;
    if (submitBtn) submitBtn.disabled = true;
    showAlert("startTripFormAlert", getErrorMessage(error));
  }
}

async function startTrip(e) {
  e.preventDefault();
  clearAlert("startTripFormAlert");

  const role = getRole();
  const vehicleId = document.getElementById("tripVehicleId").value;
  const driverId = role === "DRIVER" ? myDriverId : document.getElementById("tripDriverId").value;

  if (!vehicleId) {
    showAlert("startTripFormAlert", "Please select a vehicle.");
    return;
  }
  if (!driverId) {
    showAlert(
      "startTripFormAlert",
      role === "DRIVER" ? "No driver profile is linked to your account" : "Please select a driver."
    );
    return;
  }

  const submitBtn = document.getElementById("startTripSubmitBtn");
  await withButtonLoading(submitBtn, async () => {
    try {
      await api.post("/trips/start", { vehicleId: Number(vehicleId), driverId: Number(driverId) });
      startTripModal.hide();
      showAlert("tripsAlert", "Trip started successfully.", "success");
      loadTrips();
    } catch (error) {
      showAlert("startTripFormAlert", getErrorMessage(error));
    }
  });
}

function openEndTripModal(id) {
  document.getElementById("endTripForm").reset();
  document.getElementById("endTripId").value = id;
  clearAlert("endTripFormAlert");
  endTripModal.show();
}

async function endTrip(e) {
  e.preventDefault();
  clearAlert("endTripFormAlert");

  const id = document.getElementById("endTripId").value;
  const rawDistance = document.getElementById("endTripDistance").value.trim();

  if (rawDistance === "") {
    showAlert("endTripFormAlert", "Distance covered is required.");
    return;
  }
  const distance = Number(rawDistance);
  if (!Number.isFinite(distance) || distance < 0 || distance > 20000) {
    showAlert("endTripFormAlert", "Distance must be a valid number between 0 and 20,000.");
    return;
  }

  const submitBtn = document.querySelector('#endTripForm button[type="submit"]');
  await withButtonLoading(submitBtn, async () => {
    try {
      await api.put(`/trips/${id}/end`, { distance });
      endTripModal.hide();
      showAlert("tripsAlert", "Trip ended successfully.", "success");
      loadTrips();
    } catch (error) {
      showAlert("endTripFormAlert", getErrorMessage(error));
    }
  });
}
