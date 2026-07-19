# FleetFocus Frontend

Plain HTML5 / CSS3 / Bootstrap 5 / Vanilla JS (ES6) / Axios frontend for the FleetFocus Spring Boot backend. No build step required.

## Run it

Open `frontend/login.html` with VS Code's "Live Server" extension (or any static file server). No npm install needed — Bootstrap, Bootstrap Icons, and Axios load from CDN.

## Backend URL

Set in `js/api.js`:
```js
const API_BASE_URL = "http://localhost:8080/api";
```
Change this if your backend runs elsewhere.

## Login

Log in with a username/password created via the backend's `POST /api/auth/register` endpoint (no register page is included here, since it wasn't in the requested page list).

## Known backend limitations reflected in this UI

These aren't frontend bugs — they mirror what the backend actually exposes, since the brief asked not to invent endpoints:

- **Drivers**: only list (`GET /api/drivers`, `GET /api/drivers/available`) and create (`POST /api/drivers`) exist. No update/delete endpoint, so the Drivers page has no Edit/Delete actions.
- **Maintenance**: only list (`GET /api/maintenance`) and create (`POST /api/maintenance/log/{vehicleId}`) exist. Same reasoning — no Edit/Delete actions.
- **Vehicles**: full CRUD is supported by the backend (`GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`), so the Vehicles page has full Add/Edit/Delete.
- **Login route**: in the backend's `SecurityConfig`, `permitAll()` is applied to the exact path `/api/auth` rather than `/api/auth/**`, so `/api/auth/login` and `/api/auth/register` may currently require authentication to reach. If login calls fail with 401/403 immediately, that's a backend config fix, not something this frontend can work around.

## Role-based UI

Elements with the `admin-only` class (Add/Edit/Delete buttons) are hidden client-side for non-ADMIN users, matching the backend's `@PreAuthorize` rules. This is a UI convenience only — the backend still enforces authorization server-side.
