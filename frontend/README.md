# FleetFocus Frontend

Multi-page static frontend for the FleetFocus Fleet Management System.

## Tech Stack
- **HTML5 & CSS3** (`css/style.css`)
- **Vanilla JavaScript** (`js/config.js`, `js/utils.js`, `js/api.js`, `js/layout.js`, `js/auth.js`, page scripts)
- **Vendored Dependencies** (`vendor/`):
  - Bootstrap `5.3.3` (`vendor/bootstrap/bootstrap.min.css`, `vendor/bootstrap/bootstrap.bundle.min.js`)
  - Bootstrap Icons `1.11.3` (`vendor/bootstrap-icons/bootstrap-icons.min.css`, `vendor/bootstrap-icons/fonts/`)
  - Axios `1.7.2` (`vendor/axios/axios.min.js`)

## Configuration
Edit `js/config.js` to configure the backend API base URL:

```javascript
window.FLEETFOCUS_CONFIG = {
  API_BASE_URL: "http://localhost:8080/api"
};
```

## Running Locally
Serve the `frontend/` directory over HTTP (for example on port `5500`):

```bash
python -m http.server 5500 --directory frontend
```

Then open `http://localhost:5500/login.html` in your browser.
