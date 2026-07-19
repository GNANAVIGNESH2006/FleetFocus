document.addEventListener("DOMContentLoaded", async () => {
  requireAuth();
  initNavbar();

  const role = localStorage.getItem("role");

  showSpinner();
  try {
    const response = await api.get("/dashboard");
    const data = response.data;

    if (role === "ADMIN") {
      renderAdminDashboard(data);
      loadRecentActivities();
    } else if (role === "DISPATCHER") {
      renderDispatcherDashboard(data);
    } else if (role === "DRIVER") {
      renderDriverDashboard(data);
    }
  } catch (error) {
    showAlert("dashboardAlert", getErrorMessage(error) || "Failed to load dashboard.");
  } finally {
    hideSpinner();
  }
});

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value ?? "-";
}

function renderAdminDashboard(data) {
  setText("totalVehicles", data.totalVehicles);
  setText("availableVehicles", data.availableVehicles);
  setText("vehiclesOnTrip", data.vehiclesOnTrip);
  setText("vehiclesUnderMaintenance", data.vehiclesUnderMaintenance);
  setText("totalDrivers", data.totalDrivers);
  setText("availableDrivers", data.availableDrivers);
  setText("totalTrips", data.totalTrips);
  setText("totalUsers", data.totalUsers);
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
    const pct = Math.round((row.value / total) * 100);
    return `
      <div class="mb-2">
        <div class="d-flex justify-content-between small mb-1">
          <span>${row.label}</span>
          <span>${row.value} (${pct}%)</span>
        </div>
        <div class="progress" style="height: 10px;">
          <div class="progress-bar" style="width: ${pct}%; background-color: ${row.color};"></div>
        </div>
      </div>`;
  }).join("");
}

async function loadRecentActivities() {
  const list = document.getElementById("recentActivities");
  if (!list) return;

  try {
    const response = await api.get("/trips");
    const trips = response.data
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
          <span>${driverName} &middot; ${plate} &middot; ${formatDate(trip.startTime)}</span>
          <span class="badge ${badge}">${trip.status}</span>
        </li>`;
    }).join("");
  } catch (error) {
    list.innerHTML = `<li class="list-group-item text-danger">${getErrorMessage(error)}</li>`;
  }
}

function renderDispatcherDashboard(data) {
  setText("todaysTrips", data.todaysTrips);
  setText("activeTrips", data.activeTrips);
  setText("dispAvailableVehicles", data.availableVehicles);
  setText("dispAvailableDrivers", data.availableDrivers);
  setText("pendingAssignments", data.pendingAssignments);
}

function renderDriverDashboard(data) {
  setText("driverWelcomeName", data.driverName || localStorage.getItem("username"));
  setText("myCompletedTrips", data.myCompletedTripsCount);

  const vehicleBlock = document.getElementById("myVehicleBlock");
  if (vehicleBlock) {
    if (data.myVehiclePlate) {
      vehicleBlock.innerHTML = `
        <div><strong>${escapeHtml(data.myVehiclePlate)}</strong> &middot; ${escapeHtml(data.myVehicleModel)}</div>
        <span class="badge ${statusBadgeClass(data.myVehicleStatus)}">${data.myVehicleStatus}</span>`;
    } else {
      vehicleBlock.textContent = "No vehicle assigned right now.";
    }
  }

  const tripBlock = document.getElementById("myTripBlock");
  if (tripBlock) {
    if (data.myCurrentTripId) {
      tripBlock.innerHTML = `
        <div>Trip #${data.myCurrentTripId}</div>
        <span class="badge ${statusBadgeClass(data.myCurrentTripStatus)}">${data.myCurrentTripStatus}</span>`;
    } else {
      tripBlock.textContent = "No active trip.";
    }
  }
}
