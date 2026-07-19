let startTripModal;
let endTripModal;
let myDriverId = null;

document.addEventListener("DOMContentLoaded", () => {
  requireAuth();
  initNavbar();

  startTripModal = new bootstrap.Modal(document.getElementById("startTripModal"));
  endTripModal = new bootstrap.Modal(document.getElementById("endTripModal"));

  document.getElementById("startTripBtn").addEventListener("click", openStartTripModal);
  document.getElementById("startTripForm").addEventListener("submit", startTrip);
  document.getElementById("endTripForm").addEventListener("submit", endTrip);

  loadTrips();
});

async function loadTrips() {
  showSpinner();
  clearAlert("tripsAlert");
  const role = localStorage.getItem("role");
  const endpoint = role === "DRIVER" ? "/trips/me" : "/trips";
  try {
    const response = await api.get(endpoint);
    renderTripsTable(response.data);
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

  const sorted = [...trips].sort((a, b) => b.id - a.id);

  sorted.forEach((t) => {
    const vehicleLabel = t.vehicle ? `${escapeHtml(t.vehicle.licensePlate)} (${escapeHtml(t.vehicle.model)})` : "-";
    const driverLabel = t.driver ? escapeHtml(t.driver.name) : "-";

    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${t.id}</td>
      <td>${vehicleLabel}</td>
      <td>${driverLabel}</td>
      <td>${formatDate(t.startTime)}</td>
      <td>${t.endTime ? formatDate(t.endTime) : "-"}</td>
      <td>${t.distanceCovered ?? "-"}</td>
      <td><span class="badge ${statusBadgeClass(t.status)}">${t.status}</span></td>
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

  const role = localStorage.getItem("role");
  const vehicleSelect = document.getElementById("tripVehicleId");
  const driverSelect = document.getElementById("tripDriverId");
  vehicleSelect.innerHTML = `<option value="">Loading vehicles...</option>`;

  startTripModal.show();

  try {
    const vehiclesRes = await api.get("/vehicles/available");
    const vehicles = vehiclesRes.data;
    vehicleSelect.innerHTML = vehicles.length
      ? vehicles.map((v) => `<option value="${v.id}">${escapeHtml(v.licensePlate)} - ${escapeHtml(v.model)}</option>`).join("")
      : `<option value="">No available vehicles</option>`;

    if (role !== "DRIVER") {
      driverSelect.innerHTML = `<option value="">Loading drivers...</option>`;
      const driversRes = await api.get("/drivers/available");
      const drivers = driversRes.data;
      driverSelect.innerHTML = drivers.length
        ? drivers.map((d) => `<option value="${d.id}">${escapeHtml(d.name)} (${escapeHtml(d.licenseNumber)})</option>`).join("")
        : `<option value="">No available drivers</option>`;
    } else {
      const meRes = await api.get("/drivers/me");
      myDriverId = meRes.data.id;
    }
  } catch (error) {
    vehicleSelect.innerHTML = `<option value="">Failed to load vehicles</option>`;
    showAlert("startTripFormAlert", getErrorMessage(error));
  }
}

async function startTrip(e) {
  e.preventDefault();
  clearAlert("startTripFormAlert");

  const role = localStorage.getItem("role");
  const vehicleId = document.getElementById("tripVehicleId").value;
  const driverId = role === "DRIVER" ? myDriverId : document.getElementById("tripDriverId").value;

  if (!vehicleId || !driverId) {
    showAlert("startTripFormAlert", "Please select a vehicle.");
    return;
  }

  showSpinner();
  try {
    await api.post("/trips/start", { vehicleId: Number(vehicleId), driverId: Number(driverId) });
    startTripModal.hide();
    showAlert("tripsAlert", "Trip started successfully.", "success");
    loadTrips();
  } catch (error) {
    showAlert("startTripFormAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
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
  const distance = parseFloat(document.getElementById("endTripDistance").value);

  showSpinner();
  try {
    await api.put(`/trips/${id}/end`, { distance });
    endTripModal.hide();
    showAlert("tripsAlert", "Trip ended successfully.", "success");
    loadTrips();
  } catch (error) {
    showAlert("endTripFormAlert", getErrorMessage(error));
  } finally {
    hideSpinner();
  }
}
