/* ==========================================================================
   utils.js
   Shared helper functions used across all pages.
   ========================================================================== */

// Show a dismissible Bootstrap alert
function showAlert(containerId, message, type = "danger") {
  const container = document.getElementById(containerId);
  if (!container) return;

  container.innerHTML = `
    <div class="alert alert-${type} alert-dismissible fade show" role="alert">
      ${message}
      <button type="button" class="btn-close"
              data-bs-dismiss="alert"
              aria-label="Close"></button>
    </div>`;
}

// Clear alert
function clearAlert(containerId) {
  const container = document.getElementById(containerId);
  if (container) container.innerHTML = "";
}

// Escape HTML
function escapeHtml(text) {
  if (text === null || text === undefined) return "";

  const div = document.createElement("div");
  div.textContent = String(text);
  return div.innerHTML;
}

// Spinner
function showSpinner() {
  const el = document.getElementById("loadingSpinner");
  if (el) el.classList.remove("d-none");
}

function hideSpinner() {
  const el = document.getElementById("loadingSpinner");
  if (el) el.classList.add("d-none");
}

// ===================================================================
// Improved Error Message Handler
// ===================================================================
function getErrorMessage(error) {

  // Network error
  if (!error.response) {
    return "Unable to connect to the server.";
  }

  const data = error.response.data;

  // Plain String Response
  if (typeof data === "string") {
    return escapeHtml(data);
  }

  // Spring Validation Errors
  if (data.errors) {

    let html = "<ul class='mb-0'>";

    Object.values(data.errors).forEach(msg => {
      html += `<li>${escapeHtml(msg)}</li>`;
    });

    html += "</ul>";

    return html;
  }

  // Normal Backend Message
  if (data.message) {
    return escapeHtml(data.message);
  }

  return "Something went wrong. Please try again.";
}

// Format Date
function formatDate(value) {

  if (!value) return "-";

  const d = new Date(value);

  if (isNaN(d.getTime())) return value;

  return d.toLocaleDateString();
}

// Badge Colors
function statusBadgeClass(status) {

  switch (status) {

    case "AVAILABLE":
      return "bg-success";

    case "ON_TRIP":
      return "bg-warning text-dark";

    case "UNDER_MAINTENANCE":
      return "bg-danger";

    case "ACTIVE":
      return "bg-primary";

    case "COMPLETED":
      return "bg-secondary";

    default:
      return "bg-secondary";
  }
}

// Sidebar
function initSidebarToggle() {

  const sidebar = document.getElementById("sidebar");
  const backdrop = document.getElementById("sidebarBackdrop");
  const toggleBtn = document.getElementById("sidebarToggleBtn");

  if (!sidebar || !toggleBtn) return;

  const openSidebar = () => {

    sidebar.classList.add("show");

    if (backdrop)
      backdrop.classList.add("show");
  };

  const closeSidebar = () => {

    sidebar.classList.remove("show");

    if (backdrop)
      backdrop.classList.remove("show");
  };

  toggleBtn.addEventListener("click", openSidebar);

  if (backdrop)
    backdrop.addEventListener("click", closeSidebar);
}