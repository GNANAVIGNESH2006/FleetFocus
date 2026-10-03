/* ==========================================================================
   config.js
   Runtime configuration for FleetFocus frontend.
   Override API_BASE_URL for staging or production deployments.
   ========================================================================== */

window.FLEETFOCUS_CONFIG = Object.assign(
  {
    API_BASE_URL: "http://localhost:8080/api"
  },
  window.FLEETFOCUS_CONFIG || {}
);
