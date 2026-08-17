(() => {
  const cfg = window.DASHBOARD_CONFIG || {};
  const savedConfig = (() => {
    try { return JSON.parse(localStorage.getItem("dashboard_config_override") || "{}"); }
    catch (_) { return {}; }
  })();
  const base = String(savedConfig.apiBase || cfg.apiBase || "").replace(/\/+$/, "");
  const appKey = String(savedConfig.applicationKey || cfg.applicationKey || "");

  window.DashboardAPI = {
    base,
    async request(path, options = {}) {
      const headers = new Headers(options.headers || {});
      headers.set("X-Dashboard-App-Key", appKey);
      headers.set("Accept", "application/json");
      const token = sessionStorage.getItem("dashboard_token");
      if (token) headers.set("Authorization", "Bearer " + token);
      const response = await fetch(base + path, {...options, headers, cache:"no-store"});
      if (response.status === 401 && path !== "/api/auth/login") {
        sessionStorage.removeItem("dashboard_token");
        sessionStorage.removeItem("dashboard_user");
        if (!location.pathname.endsWith("login.html")) location.href = "login.html";
      }
      return response;
    },
    logout() {
      sessionStorage.removeItem("dashboard_token");
      sessionStorage.removeItem("dashboard_user");
      location.href = "login.html";
    },
    isLoggedIn() { return !!sessionStorage.getItem("dashboard_token"); }
  };

  document.addEventListener("click", e => {
    const logout = e.target.closest("[data-logout]");
    if (logout) { e.preventDefault(); DashboardAPI.logout(); }
  });

  function currentDashboardConfig() {
    return {
      apiBase: String((localStorage.getItem("dashboard_config_override") ?
        JSON.parse(localStorage.getItem("dashboard_config_override")).apiBase : cfg.apiBase) || "").replace(/\/+$/, ""),
      applicationKey: String((localStorage.getItem("dashboard_config_override") ?
        JSON.parse(localStorage.getItem("dashboard_config_override")).applicationKey : cfg.applicationKey) || "")
    };
  }

  function addSettingsUI() {
    if (document.getElementById("dashboardSettingsButton")) return;

    const button = document.createElement("button");
    button.id = "dashboardSettingsButton";
    button.className = "settings-button";
    button.type = "button";
    button.title = "Dashboard settings";
    button.setAttribute("aria-label", "Dashboard settings");
    button.innerHTML = "⚙";
    document.body.appendChild(button);

    const overlay = document.createElement("div");
    overlay.id = "dashboardSettingsOverlay";
    overlay.className = "settings-overlay";
    overlay.innerHTML = `
      <div class="settings-modal" role="dialog" aria-modal="true" aria-labelledby="settingsTitle">
        <div class="settings-header">
          <div>
            <h2 id="settingsTitle">Dashboard settings</h2>
            <p class="muted small">Change the API connection used by this app.</p>
          </div>
          <button type="button" class="settings-close" id="settingsClose" aria-label="Close">×</button>
        </div>
        <label class="field-label" for="settingsApiBase">API Base URL</label>
        <input id="settingsApiBase" type="url" autocomplete="off" spellcheck="false" placeholder="http://127.0.0.1:8765">
        <label class="field-label" for="settingsApplicationKey">Application Key</label>
        <input id="settingsApplicationKey" type="text" autocomplete="off" spellcheck="false" placeholder="Application key">
        <p class="settings-note muted small">Changes are saved on this device and remain after closing the app.</p>
        <div class="settings-actions">
          <button type="button" class="btn" id="settingsCancel">Cancel</button>
          <button type="button" class="btn settings-save" id="settingsSave">Save</button>
        </div>
      </div>`;
    document.body.appendChild(overlay);

    const apiInput = document.getElementById("settingsApiBase");
    const keyInput = document.getElementById("settingsApplicationKey");

    function openSettings() {
      const current = currentDashboardConfig();
      apiInput.value = current.apiBase;
      keyInput.value = current.applicationKey;
      overlay.classList.add("open");
      setTimeout(() => apiInput.focus(), 0);
    }
    function closeSettings() { overlay.classList.remove("open"); }

    button.addEventListener("click", openSettings);
    document.getElementById("settingsClose").addEventListener("click", closeSettings);
    document.getElementById("settingsCancel").addEventListener("click", closeSettings);
    overlay.addEventListener("click", e => { if (e.target === overlay) closeSettings(); });

    document.getElementById("settingsSave").addEventListener("click", () => {
      // Password is intentionally blank for now. Canceling the prompt aborts the save.
      const password = window.prompt("Enter the settings password:", "");
      if (password === null) return;

      // TODO: change this when you choose a settings password.
      const SETTINGS_PASSWORD = "Coco2020#~";
      if (password !== SETTINGS_PASSWORD) {
        window.alert("Incorrect settings password.");
        return;
      }

      const apiBase = apiInput.value.trim().replace(/\/+$/, "");
      const applicationKey = keyInput.value.trim();

      if (!apiBase) {
        window.alert("Please enter an API Base URL.");
        apiInput.focus();
        return;
      }
      if (!/^https?:\/\//i.test(apiBase)) {
        window.alert("API Base URL must start with http:// or https://");
        apiInput.focus();
        return;
      }
      if (!applicationKey) {
        window.alert("Please enter an Application Key.");
        keyInput.focus();
        return;
      }

      const newConfig = { apiBase, applicationKey };
      localStorage.setItem("dashboard_config_override", JSON.stringify(newConfig));
      closeSettings();
      window.alert("Settings saved. Reloading the dashboard...");
      window.location.reload();
    });

    document.addEventListener("keydown", e => {
      if (e.key === "Escape" && overlay.classList.contains("open")) closeSettings();
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", addSettingsUI);
  } else {
    addSettingsUI();
  }

  window.dashboardHealth = async function() {
    try {
      const r = await fetch(base + "/api/health", {
        cache:"no-store",
        headers:{"X-Dashboard-App-Key":appKey}
      });
      return r.ok;
    } catch (_) { return false; }
  };
})();
