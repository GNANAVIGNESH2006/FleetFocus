/* ==========================================================================
   utils.js
   Shared helper functions used across all pages.
   ========================================================================== */

const CURRENCY = {
  locale: "en-IN",
  code: "INR"
};

// Show a dismissible Bootstrap alert
function showAlert(containerId, message, type = "danger") {
  const container = document.getElementById(containerId);
  if (!container) return;

  container.setAttribute("aria-live", "polite");
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

// Reference-counted Spinner
let spinnerCount = 0;

function showSpinner() {
  spinnerCount++;
  const el = document.getElementById("loadingSpinner");
  if (el) el.classList.remove("d-none");
}

function hideSpinner() {
  spinnerCount = Math.max(0, spinnerCount - 1);
  if (spinnerCount === 0) {
    const el = document.getElementById("loadingSpinner");
    if (el) el.classList.add("d-none");
  }
}

async function withButtonLoading(button, asyncFn) {
  if (button) button.disabled = true;
  showSpinner();
  try {
    return await asyncFn();
  } finally {
    hideSpinner();
    if (button) button.disabled = false;
  }
}

// ===================================================================
// Improved Error Message Handler (AG-07)
// ===================================================================
function getErrorMessage(error) {
  if (!error || !error.response) {
    return "Unable to connect to the server.";
  }

  const status = error.response.status;
  const data = error.response.data;

  if (typeof data === "string" && data.trim()) {
    return escapeHtml(data.trim());
  }

  if (data && typeof data === "object") {
    if (data.errors && typeof data.errors === "object" && Object.keys(data.errors).length > 0) {
      let html = "<ul class='mb-0'>";
      Object.values(data.errors).forEach((msg) => {
        html += `<li>${escapeHtml(msg)}</li>`;
      });
      html += "</ul>";
      return html;
    }

    if (data.message && typeof data.message === "string" && data.message.trim()) {
      return escapeHtml(data.message.trim());
    }
  }

  if (status === 400) return "Invalid request";
  if (status === 401) return "Session expired. Please log in again.";
  if (status === 403) return "You don't have permission to perform this action.";
  if (status === 404) return "Not found.";
  if (status === 409) return "Conflict with existing data.";
  if (status >= 500) return "Server error. Please try again.";

  return "Something went wrong. Please try again.";
}

// Format Date (parses YYYY-MM-DD as local date rather than UTC)
function formatDate(value) {
  if (!value) return "-";

  if (typeof value === "string") {
    const trimmed = value.trim();
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(trimmed);
    if (match) {
      const year = Number(match[1]);
      const month = Number(match[2]) - 1;
      const day = Number(match[3]);
      const localDate = new Date(year, month, day);
      if (!isNaN(localDate.getTime())) {
        return localDate.toLocaleDateString();
      }
    }
  }

  const d = new Date(value);
  if (isNaN(d.getTime())) return escapeHtml(String(value));

  return d.toLocaleDateString();
}

// Format Date & Time
function formatDateTime(value) {
  if (!value) return "-";

  const d = new Date(value);
  if (isNaN(d.getTime())) return escapeHtml(String(value));

  return d.toLocaleString();
}

// Format Currency
function formatCurrency(n) {
  const num = Number(n);
  if (!Number.isFinite(num)) return "-";

  return new Intl.NumberFormat(CURRENCY.locale, {
    style: "currency",
    currency: CURRENCY.code,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(num);
}

// Badge Colors
function statusBadgeClass(status) {
  switch (status) {
    case "AVAILABLE":
      return "bg-success";
    case "ON_TRIP":
    case "IN_PROGRESS":
      return "bg-warning text-dark";
    case "UNDER_MAINTENANCE":
      return "bg-danger";
    case "ACTIVE":
      return "bg-primary";
    case "COMPLETED":
      return "bg-success";
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
    if (backdrop) backdrop.classList.add("show");
  };

  const closeSidebar = () => {
    sidebar.classList.remove("show");
    if (backdrop) backdrop.classList.remove("show");
  };

  toggleBtn.addEventListener("click", openSidebar);
  if (backdrop) backdrop.addEventListener("click", closeSidebar);
}