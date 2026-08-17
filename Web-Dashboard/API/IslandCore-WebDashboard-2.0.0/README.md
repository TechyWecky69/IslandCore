# IslandCore Web Dashboard API 2.0.0

This release separates the dashboard UI from the Minecraft plugin.

## Architecture

Minecraft server/plugin -> HTTPS JSON API -> separate dashboard website/app

The plugin no longer hosts the dashboard pages and no longer captures or exposes the Minecraft console log.

### API endpoints

- `GET /api/health` — public connectivity check (requires the application key).
- `POST /api/auth/login` — username/password login.
- `GET /api/status` — complete current server status snapshot plus retained chat/trade counts.
- `GET /api/metrics` — detailed live server metrics.
- `GET /api/chat` — retained player chat log.
- `GET /api/trades` — IslandCore trade history.

All protected endpoints require:

- `X-Dashboard-App-Key: <application key>`
- `Authorization: Bearer <session token>`

Sessions expire after 8 hours.

## Security

1. TLS is enabled by default.
2. Login passwords are converted to salted PBKDF2-HMAC-SHA256 hashes on first startup if an old plaintext `password:` field is present.
3. The API has a separate application key.
4. Login attempts are rate-limited after repeated failures from an IP.
5. Security headers and no-store caching are enabled.
6. The server's TLS private key stays inside the PKCS12 keystore and is never sent to the website.

### Generate the TLS certificate/keystore

From the Minecraft server directory with Java 21 installed:

```text
keytool -genkeypair -alias islandcore-dashboard -keyalg RSA -keysize 3072 -validity 825 -storetype PKCS12 -keystore plugins/IslandCoreWebDashboard/dashboard.p12 -storepass CHANGE_THIS_PASSWORD -keypass CHANGE_THIS_PASSWORD -dname "CN=IslandCore Dashboard"
```

Then put the same password in `config.yml` under `tls.keystore-password`.

For a public website, replace the self-signed certificate with a certificate trusted by the client, or put the API behind a trusted HTTPS reverse proxy.

### Application authorization

On first start the plugin generates `api.application-key` if it is empty. Copy that value into the separate website/app's `config.js`.

Do not commit that value to a public repository.

For the future native app, keep the application key in the platform's secure credential/keychain storage rather than plain source code.

## First start

1. Copy the plugin JAR into `plugins/`.
2. Start the server once.
3. Create the TLS PKCS12 keystore before enabling TLS, or temporarily set `tls.enabled: false` if you are placing the API behind a trusted HTTPS proxy.
4. Change the example dashboard password.
5. Copy the generated application key from `plugins/IslandCoreWebDashboard/config.yml`.
6. Put the API URL and key into the separate website's `config.js`.

The old `password:` entry is automatically replaced with `passwordSalt` and `passwordHash`.

## Build

Requirements:
- Java 21
- Maven 3.9+
- Spigot 1.21.1

```text
mvn clean package
```

The plugin JAR is written to `target/islandcore-webdashboard-2.0.0.jar`.

## Important

The API intentionally does not expose command execution or the Minecraft console. This removes the highest-risk dashboard capability from the browser-facing API.

Trade files are read from the configured IslandCore trade-log directory and cached by filename/last-modified time.
