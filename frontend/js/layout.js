/* ==========================================================================
   layout.js
   Shared layout, navigation, mobile sidebar, confirmation modal, and
   pagination helpers across all authenticated FleetFocus pages (AG-21, AG-22, AG-20).
   ========================================================================== */

const FLEETFOCUS_NAV_ITEMS = [
  { href: "dashboard.html", icon: "bi-speedometer2", label: "Dashboard", roles: null },
  { href: "vehicles.html", icon: "bi-car-front", label: "Vehicles", roles: "ADMIN,DISPATCHER" },
  { href: "drivers.html", icon: "bi-person-badge", label: "Drivers", roles: "ADMIN,DISPATCHER" },
  { href: "trips.html", icon: "bi-sign-turn-right", label: "Trips", roles: "ADMIN,DISPATCHER" },
  { href: "trips.html", icon: "bi-sign-turn-right", label: "My Trips", roles: "DRIVER" },
  { href: "vehicles.html", icon: "bi-car-front", label: "My Vehicle", roles: "DRIVER" },
  { href: "maintenance.html", icon: "bi-tools", label: "Maintenance", roles: "ADMIN,DISPATCHER" },
  { href: "maintenance.html", icon: "bi-tools", label: "Report Issue", roles: "DRIVER" },
  { href: "users.html", icon: "bi-people", label: "Users", roles: "ADMIN" }
];

function initLayout() {
  const sidebar = document.getElementById("sidebar");
  if (sidebar) {
    const currentFile = (window.location.pathname.split("/").pop() || "dashboard.html").toLowerCase();
    const navHtml = FLEETFOCUS_NAV_ITEMS.map((item) => {
      const isActive = item.href.toLowerCase() === currentFile;
      const roleAttr = item.roles ? ` data-roles="${item.roles}"` : "";
      const activeClass = isActive ? " active" : "";
      const ariaCurrent = isActive ? ` aria-current="page"` : "";
      return `<li class="nav-item"${roleAttr}><a class="nav-link${activeClass}" href="${item.href}"${ariaCurrent}><i class="bi ${item.icon}"></i>${escapeHtml(item.label)}</a></li>`;
    }).join("");

    sidebar.innerHTML = `
      <div class="brand d-flex justify-content-between align-items-center">
        <span><i class="bi bi-truck me-2"></i>FleetFocus</span>
        <button type="button" id="sidebarCloseBtn" class="btn btn-sm text-light d-lg-none p-0 border-0" aria-label="Close navigation">
          <i class="bi bi-x-lg"></i>
        </button>
      </div>
      <ul class="nav flex-column py-2">${navHtml}</ul>
    `;
  }

  const toggleBtn = document.getElementById("sidebarToggleBtn");
  if (toggleBtn && !toggleBtn.getAttribute("aria-label")) {
    toggleBtn.setAttribute("aria-label", "Toggle navigation sidebar");
  }

  if (typeof applyRoleVisibility === "function") {
    applyRoleVisibility(typeof getRole === "function" ? getRole() : "");
  }

  initSidebarBehavior();
}

function initSidebarBehavior() {
  const sidebar = document.getElementById("sidebar");
  const backdrop = document.getElementById("sidebarBackdrop");
  const toggleBtn = document.getElementById("sidebarToggleBtn");
  const closeBtn = document.getElementById("sidebarCloseBtn");

  if (!sidebar) return;

  const openSidebar = () => {
    sidebar.classList.add("show");
    if (backdrop) backdrop.classList.add("show");
  };

  const closeSidebar = () => {
    sidebar.classList.remove("show");
    if (backdrop) backdrop.classList.remove("show");
  };

  if (toggleBtn && !toggleBtn.dataset.layoutBound) {
    toggleBtn.dataset.layoutBound = "true";
    toggleBtn.addEventListener("click", openSidebar);
  }
  if (backdrop && !backdrop.dataset.layoutBound) {
    backdrop.dataset.layoutBound = "true";
    backdrop.addEventListener("click", closeSidebar);
  }
  if (closeBtn) {
    closeBtn.addEventListener("click", closeSidebar);
  }

  sidebar.querySelectorAll(".nav-link").forEach((link) => {
    link.addEventListener("click", closeSidebar);
  });

  if (!document.body.dataset.escSidebarBound) {
    document.body.dataset.escSidebarBound = "true";
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && sidebar.classList.contains("show")) {
        closeSidebar();
      }
    });
  }
}

// Shared Bootstrap confirmation modal (replaces native confirm())
function confirmActionModal({
  title = "Confirm Action",
  message = "Are you sure you want to proceed?",
  confirmText = "Confirm",
  confirmClass = "btn-danger"
} = {}) {
  return new Promise((resolve) => {
    let modalEl = document.getElementById("sharedConfirmModal");
    if (!modalEl) {
      modalEl = document.createElement("div");
      modalEl.id = "sharedConfirmModal";
      modalEl.className = "modal fade";
      modalEl.tabIndex = -1;
      modalEl.setAttribute("aria-hidden", "true");
      modalEl.innerHTML = `
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title" id="sharedConfirmModalTitle"></h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body" id="sharedConfirmModalBody"></div>
            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
              <button type="button" id="sharedConfirmModalBtn" class="btn">Confirm</button>
            </div>
          </div>
        </div>`;
      document.body.appendChild(modalEl);
    }

    document.getElementById("sharedConfirmModalTitle").textContent = title;
    document.getElementById("sharedConfirmModalBody").textContent = message;

    const confirmBtn = document.getElementById("sharedConfirmModalBtn");
    confirmBtn.textContent = confirmText;
    confirmBtn.className = `btn ${confirmClass}`;

    const bsModal = bootstrap.Modal.getOrCreateInstance(modalEl);
    let settled = false;

    const onConfirm = () => {
      settled = true;
      bsModal.hide();
      resolve(true);
    };

    const onHidden = () => {
      confirmBtn.removeEventListener("click", onConfirm);
      modalEl.removeEventListener("hidden.bs.modal", onHidden);
      if (!settled) {
        settled = true;
        resolve(false);
      }
    };

    confirmBtn.addEventListener("click", onConfirm);
    modalEl.addEventListener("hidden.bs.modal", onHidden);
    bsModal.show();
  });
}

// Shared pagination bar renderer (AG-20)
function renderPaginationBar(containerId, page, size, totalElements, onPageChange) {
  const container = document.getElementById(containerId);
  if (!container) return;

  const total = Number(totalElements) || 0;
  const totalPages = Math.max(1, Math.ceil(total / size));

  if (total === 0) {
    container.innerHTML = "";
    return;
  }

  const startItem = page * size + 1;
  const endItem = Math.min(total, (page + 1) * size);

  container.innerHTML = `
    <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mt-3 pt-2 border-top">
      <span class="text-muted small">Showing ${startItem}&ndash;${endItem} of ${total}</span>
      <div class="btn-group btn-group-sm" role="group" aria-label="Pagination">
        <button type="button" class="btn btn-outline-secondary" id="${containerId}_prev" ${page <= 0 ? "disabled" : ""}>
          <i class="bi bi-chevron-left"></i> Prev
        </button>
        <span class="btn btn-outline-secondary disabled text-dark">Page ${page + 1} of ${totalPages}</span>
        <button type="button" class="btn btn-outline-secondary" id="${containerId}_next" ${page + 1 >= totalPages ? "disabled" : ""}>
          Next <i class="bi bi-chevron-right"></i>
        </button>
      </div>
    </div>`;

  const prevBtn = document.getElementById(`${containerId}_prev`);
  const nextBtn = document.getElementById(`${containerId}_next`);
  if (prevBtn && page > 0) {
    prevBtn.addEventListener("click", () => onPageChange(page - 1));
  }
  if (nextBtn && page + 1 < totalPages) {
    nextBtn.addEventListener("click", () => onPageChange(page + 1));
  }
}
