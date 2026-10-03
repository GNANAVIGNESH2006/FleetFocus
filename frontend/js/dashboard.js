document.addEventListener("DOMContentLoaded", async () => {
  if (!requireAuth()) return;
  if (!(await syncSession())) return;
  initNavbar();

  await loadDashboard();
});

async function loadDashboard() {
  clearAlert("dashboardAlert");
  showSpinner();
  try {
    const response = await api.get("/dashboard");
    const data = response.data;
    const role = data.role || getRole();
    if (data.role) {
      localStorage.setItem("role", data.role);
      applyRoleVisibility(data.role);
    }

    if (role === "ADMIN") {
      renderAdminDashboard(data);
      await loadRecentActivities();
    } else if (role === "DISPATCHER") {
      renderDispatcherDashboard(data);
    } else if (role === "DRIVER") {
      renderDriverDashboard(data);
    }
  } catch (error) {
    const msg = getErrorMessage(error) || "Failed to load dashboard.";
    showAlert(
      "dashboardAlert",
      `${msg} <button type="button" class="btn btn-sm btn-outline-danger ms-2" onclick="loadDashboard()"><i class="bi bi-arrow-clockwise me-1"></i>Retry</button>`
    );
  } finally {
    hideSpinner();
  }
}

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value ?? "-";
}

function renderAdminDashboard(data) {
  setText("totalVehicles", data.totalVehicles ?? 0);
  setText("availableVehicles", data.availableVehicles ?? 0);
  setText("vehiclesOnTrip", data.vehiclesOnTrip ?? 0);
  setText("vehiclesUnderMaintenance", data.vehiclesUnderMaintenance ?? 0);
  setText("totalDrivers", data.totalDrivers ?? 0);
  setText("availableDrivers", data.availableDrivers ?? 0);
  setText("totalTrips", data.totalTrips ?? 0);
  setText("totalUsers", data.totalUsers ?? 0);
  setText("adminActiveTrips", data.activeTrips ?? 0);
  setText("adminCompletedTrips", data.completedTrips ?? 0);
  setText("adminOverdueTrips", data.overdueTrips ?? data.pendingAssignments ?? 0);
  renderFleetChart(data);
}

function renderFleetChart(data) {
  const container = document.getElementById("fleetChart");
  if (!container) return;

  const total = Math.max(data.totalVehicles || 0, 1);
  const rows = [
    { label: "Available", value: data.availableVehicles || 0, color: "#198754" },
    { label: "On Trip", value: data.vehiclesOnTrip || 0, color: "#ffc107" },
    { label: "Under Maintenance", value: data.vehiclesUnderMaintenance || 0, color: "#dc3545" }
  ];

  container.innerHTML = rows.map((row) => {
    const pct = data.totalVehicles ? Math.round((row.value / total) * 100) : 0;
    return `
      <div class="mb-2">
        <div class="d-flex justify-content-between small mb-1">
          <span>${row.label}</span>
          <span>${row.value} (${pct}%)</span>
        </div>
        <div class="progress" style="height: 10px;">
          <div class="progress-bar" role="progressbar" aria-valuenow="${pct}" aria-valuemin="0" aria-valuemax="100" style="width: ${pct}%; background-color: ${row.color};"></div>
        </div>
      </div>`;
  }).join("");
}

async function loadRecentActivities() {
  const list = document.getElementById("recentActivities");
  if (!list) return;

  try {
    const response = await api.get("/trips", {
      params: { page: 0, size: 5, sort: "startTime,desc" }
    });
    const trips = (response.data || [])
      .slice()
      .sort((a, b) => new Date(b.startTime) - new Date(a.startTime))
      .slice(0, 5);

    if (trips.length === 0) {
      list.innerHTML = `<li class="list-group-item text-muted">No recent activity.</li>`;
      return;
    }

    list.innerHTML = trips.map((trip) => {
      const badge = statusBadgeClass(trip.status);
      const driverName = trip.driver ? escapeHtml(trip.driver.name) : "-";
      const plate = trip.vehicle ? escapeHtml(trip.vehicle.licensePlate) : "-";
      return `
        <li class="list-group-item d-flex justify-content-between align-items-center">
          <span>${driverName} &middot; ${plate} &middot; ${formatDateTime(trip.startTime)}</span>
          <span class="badge ${badge}">${escapeHtml(trip.status)}</span>
        </li>`;
    }).join("");
  } catch (error) {
    list.innerHTML = `<li class="list-group-item text-danger">${getErrorMessage(error)}</li>`;
  }
}

function renderDispatcherDashboard(data) {
  setText("todaysTrips", data.todaysTrips ?? 0);
  setText("activeTrips", data.activeTrips ?? 0);
  setText("dispAvailableVehicles", data.availableVehicles ?? 0);
  setText("dispAvailableDrivers", data.availableDrivers ?? 0);
  setText("overdueTrips", data.overdueTrips ?? data.pendingAssignments ?? 0);
  setText("dispCompletedTrips", data.completedTrips ?? 0);
}

function renderDriverDashboard(data) {
  setText("driverWelcomeName", data.driverName || getUsername());
  setText("myCompletedTrips", data.myCompletedTripsCount ?? 0);

  const vehicleBlock = document.getElementById("myVehicleBlock");
  if (vehicleBlock) {
    if (data.myVehiclePlate) {
      vehicleBlock.innerHTML = `
        <div><strong>${escapeHtml(data.myVehiclePlate)}</strong> &middot; ${escapeHtml(data.myVehicleModel)}</div>
        <span class="badge ${statusBadgeClass(data.myVehicleStatus)}">${escapeHtml(data.myVehicleStatus)}</span>`;
    } else {
      vehicleBlock.textContent = "No vehicle assigned right now.";
    }
  }

  const tripBlock = document.getElementById("myTripBlock");
  if (tripBlock) {
    if (data.myCurrentTripId) {
      tripBlock.innerHTML = `
        <div>Trip #${escapeHtml(data.myCurrentTripId)}</div>
        <span class="badge ${statusBadgeClass(data.myCurrentTripStatus)}">${escapeHtml(data.myCurrentTripStatus)}</span>`;
    } else {
      tripBlock.textContent = "No active trip.";
    }
  }
}
