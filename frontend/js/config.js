/* ==========================================================================
   config.js
   Runtime configuration for FleetFocus frontend.
   Override API_BASE_URL for staging or production deployments.
   ========================================================================== */

window.FLEETFOCUS_CONFIG = Object.assign(
  {
    API_BASE_URL: "https://fleetfocus-production.up.railway.app/api"
  },
  window.FLEETFOCUS_CONFIG || {}
);
