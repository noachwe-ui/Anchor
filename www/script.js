let quotes = [];
let urls = [];
let dailyDose = [];
let hillelLinks = [];

const MODE_KEY = "anchor-bubble-mode";

async function loadData() {
  // Local-only: read the JSON files bundled inside the app. No network calls.
  try {
    const [qRes, uRes, dRes, hRes] = await Promise.all([
      fetch("quotes.json"),
      fetch("urls.json"),
      fetch("daily_dose.json"),
      fetch("hillel_eisenberg.json")
    ]);
    if (qRes.ok) quotes = await qRes.json();
    if (uRes.ok) urls = await uRes.json();
    if (dRes.ok) dailyDose = await dRes.json();
    if (hRes.ok) hillelLinks = await hRes.json();
    showRandomQuote();
  } catch (err) {
    const qt = document.getElementById("quote-text");
    if (qt) qt.textContent = "Take a slow breath. You are here now.";
  }
}

function showRandomQuote() {
  if (!quotes.length) return;
  const item = quotes[Math.floor(Math.random() * quotes.length)];
  const qt = document.getElementById("quote-text");
  const qs = document.getElementById("quote-source");
  if (qt) qt.textContent = item.quote;
  if (qs) qs.textContent = item.source ? `— ${item.source}` : "";
}

const dailyBtn = document.getElementById("daily-dose-btn");
if (dailyBtn) dailyBtn.addEventListener("click", () => {
  if (!dailyDose.length) {
    alert("No Daily Dose links yet. Add them in daily_dose.json");
    return;
  }
  window.open(dailyDose[Math.floor(Math.random() * dailyDose.length)], "_blank");
});

const hillelBtn = document.getElementById("hillel-btn");
if (hillelBtn) hillelBtn.addEventListener("click", () => {
  if (!hillelLinks.length) {
    alert("No Rabbi Hillel Eisenberg links yet. Add them in hillel_eisenberg.json");
    return;
  }
  window.open(hillelLinks[Math.floor(Math.random() * hillelLinks.length)], "_blank");
});

const clipBtn = document.getElementById("clip-btn");
if (clipBtn) clipBtn.addEventListener("click", () => {
  if (!urls.length) {
    alert("No clips added yet.");
    return;
  }
  const link = urls[Math.floor(Math.random() * urls.length)];
  window.open(link, "_blank");
});

// Notes
const noteEl = document.getElementById("personal-note");
if (noteEl) noteEl.value = localStorage.getItem("anchor-note") || "";
const saveNoteBtn = document.getElementById("save-note-btn");
if (saveNoteBtn) saveNoteBtn.addEventListener("click", () => {
  if (noteEl) localStorage.setItem("anchor-note", noteEl.value);
  alert("Note saved on this device.");
});

// Screen navigation
const mainScreen = document.getElementById("screen-main");
const settingsScreen = document.getElementById("screen-settings");

const openSettingsBtn = document.getElementById("open-settings-btn");
if (openSettingsBtn) openSettingsBtn.addEventListener("click", () => {
  if (mainScreen) mainScreen.hidden = true;
  if (settingsScreen) settingsScreen.hidden = false;
});

const backMainBtn = document.getElementById("back-main-btn");
if (backMainBtn) backMainBtn.addEventListener("click", () => {
  if (settingsScreen) settingsScreen.hidden = true;
  if (mainScreen) mainScreen.hidden = false;
});

// Bubble mode
function getMode() {
  return localStorage.getItem(MODE_KEY) || "always";
}

function loadModeUI() {
  const mode = getMode();
  const input = document.querySelector(`input[name="bubble-mode"][value="${mode}"]`);
  if (input) input.checked = true;
  const status = document.getElementById("mode-status");
  if (status) status.textContent = "Current mode: " + mode;
}

const saveModeBtn = document.getElementById("save-mode-btn");
if (saveModeBtn) saveModeBtn.addEventListener("click", () => {
  const selected = document.querySelector('input[name="bubble-mode"]:checked');
  if (!selected) return;
  localStorage.setItem(MODE_KEY, selected.value);
  // Push to native immediately instead of waiting for it to next poll
  // localStorage — that delay was making this menu look like it did nothing.
  if (window.AnchorNative && window.AnchorNative.setBubbleMode) {
    window.AnchorNative.setBubbleMode(selected.value);
  }
  const status = document.getElementById("mode-status");
  if (status) status.textContent = "Saved: " + selected.value;
  alert("Bubble mode saved: " + selected.value);
});

// Chizuk links
function getChizukLinks() {
  try { return JSON.parse(localStorage.getItem("anchor-chizuk-links") || "[]"); }
  catch { return []; }
}

function saveChizukLinks(links) {
  localStorage.setItem("anchor-chizuk-links", JSON.stringify(links));
}

function renderChizukList() {
  const list = document.getElementById("chizuk-list");
  if (!list) return;
  const links = getChizukLinks();
  list.innerHTML = "";
  links.forEach((link, index) => {
    const row = document.createElement("div");
    row.style.cssText = "display:flex;align-items:center;gap:8px;margin:8px 0;";

    const linkBtn = document.createElement("button");
    linkBtn.className = "btn secondary";
    linkBtn.style.cssText = "flex:1;text-align:left;font-size:0.9rem;padding:10px 12px;";
    linkBtn.textContent = link.length > 40 ? link.slice(0, 37) + "..." : link;
    linkBtn.onclick = () => window.open(link, "_blank");

    const delBtn = document.createElement("button");
    delBtn.textContent = "✕";
    delBtn.style.cssText = "background:#ff6b6b;color:#fff;border:none;border-radius:50%;width:28px;height:28px;";
    delBtn.onclick = () => {
      links.splice(index, 1);
      saveChizukLinks(links);
      renderChizukList();
      updateChizukButton();
    };

    row.appendChild(linkBtn);
    row.appendChild(delBtn);
    list.appendChild(row);
  });
}

function updateChizukButton() {
  const btn = document.getElementById("chizuk-btn");
  if (btn) btn.style.display = getChizukLinks().length ? "block" : "none";
}

const addChizukBtn = document.getElementById("add-chizuk-btn");
if (addChizukBtn) addChizukBtn.addEventListener("click", () => {
  const input = document.getElementById("chizuk-link");
  if (!input) return;
  const link = input.value.trim();
  if (!link) return;
  const links = getChizukLinks();
  if (!links.includes(link)) {
    links.push(link);
    saveChizukLinks(links);
  }
  input.value = "";
  renderChizukList();
  updateChizukButton();
  alert("Link added!");
});

const chizukBtn = document.getElementById("chizuk-btn");
if (chizukBtn) chizukBtn.addEventListener("click", () => {
  const links = getChizukLinks();
  if (!links.length) return;
  window.open(links[Math.floor(Math.random() * links.length)], "_blank");
});

loadModeUI();
renderChizukList();
updateChizukButton();

// TEMPORARY DEBUG — for diagnosing the bubble persistence bug. Remove this
// block and the matching Settings section once it's fixed.
const refreshLogBtn = document.getElementById("refresh-log-btn");
const debugLogOutput = document.getElementById("debug-log-output");
if (refreshLogBtn) refreshLogBtn.addEventListener("click", () => {
  if (window.AnchorNative && window.AnchorNative.getDebugLog) {
    debugLogOutput.value = window.AnchorNative.getDebugLog();
    debugLogOutput.scrollTop = debugLogOutput.scrollHeight;
  }
});
const clearLogBtn = document.getElementById("clear-log-btn");
if (clearLogBtn) clearLogBtn.addEventListener("click", () => {
  if (window.AnchorNative && window.AnchorNative.clearDebugLog) {
    window.AnchorNative.clearDebugLog();
    if (debugLogOutput) debugLogOutput.value = "";
  }
});

// Widget appearance (color + opacity) — applies to all Anchor widgets.
const widgetColorPicker = document.getElementById("widget-color-picker");
if (widgetColorPicker) {
  const savedColor = localStorage.getItem("anchor-widget-color");
  if (savedColor) widgetColorPicker.value = savedColor;
  // "change" fires once the picker closes, not on every drag frame inside it.
  widgetColorPicker.addEventListener("change", () => {
    const color = widgetColorPicker.value;
    localStorage.setItem("anchor-widget-color", color);
    if (window.AnchorNative && window.AnchorNative.setWidgetColor) {
      window.AnchorNative.setWidgetColor(color);
    }
    const status = document.getElementById("widget-appearance-status");
    if (status) status.textContent = "Widget color updated";
  });
}

const widgetTextColorPicker = document.getElementById("widget-text-color-picker");
if (widgetTextColorPicker) {
  const savedTextColor = localStorage.getItem("anchor-widget-text-color");
  if (savedTextColor) widgetTextColorPicker.value = savedTextColor;
  widgetTextColorPicker.addEventListener("change", () => {
    const color = widgetTextColorPicker.value;
    localStorage.setItem("anchor-widget-text-color", color);
    if (window.AnchorNative && window.AnchorNative.setWidgetTextColor) {
      window.AnchorNative.setWidgetTextColor(color);
    }
    const status = document.getElementById("widget-appearance-status");
    if (status) status.textContent = "Text color updated";
  });
}

const widgetOpacityInput = document.getElementById("widget-opacity");
if (widgetOpacityInput) {
  const savedOpacity = localStorage.getItem("anchor-widget-opacity");
  if (savedOpacity) widgetOpacityInput.value = savedOpacity;
  // "change" (fires on release), not "input" (fires continuously while
  // dragging) — avoids flooding the native bridge and widget redraws.
  widgetOpacityInput.addEventListener("change", () => {
    const pct = parseInt(widgetOpacityInput.value, 10);
    localStorage.setItem("anchor-widget-opacity", String(pct));
    if (window.AnchorNative && window.AnchorNative.setWidgetAlpha) {
      // Convert 0-100% to a 0-255 alpha byte for the native side.
      window.AnchorNative.setWidgetAlpha(Math.round(pct * 255 / 100));
    }
    const status = document.getElementById("widget-appearance-status");
    if (status) status.textContent = "Opacity: " + pct + "%";
  });
}

// Home screen mode: quote | image (local file)
const HOME_MODE_KEY = "anchor-home-mode";
const HOME_IMAGE_KEY = "anchor-home-image"; // data URL stored locally

function applyHomeMode() {
  const mode = localStorage.getItem(HOME_MODE_KEY) || "quote";
  const quoteCard = document.getElementById("quote-card");
  const imageCard = document.getElementById("image-card");
  const img = document.getElementById("home-image");
  const dataUrl = localStorage.getItem(HOME_IMAGE_KEY) || "";

  if (mode === "image" && dataUrl) {
    if (quoteCard) quoteCard.hidden = true;
    if (imageCard) imageCard.hidden = false;
    if (img) img.src = dataUrl;
  } else {
    if (quoteCard) quoteCard.hidden = false;
    if (imageCard) imageCard.hidden = true;
  }
}

function loadHomeUI() {
  const mode = localStorage.getItem(HOME_MODE_KEY) || "quote";
  const input = document.querySelector(`input[name="home-mode"][value="${mode}"]`);
  if (input) input.checked = true;
  const status = document.getElementById("home-status");
  if (status) {
    const hasImg = !!localStorage.getItem(HOME_IMAGE_KEY);
    status.textContent = "Current: " + mode + (hasImg ? " (image saved)" : " (no image yet)");
  }
  applyHomeMode();
}

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

// Resize large photos so localStorage can hold them
function resizeImageDataUrl(dataUrl, maxW = 1200, quality = 0.7) {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = () => {
      let w = img.width, h = img.height;
      if (w > maxW) {
        h = Math.round(h * (maxW / w));
        w = maxW;
      }
      const canvas = document.createElement("canvas");
      canvas.width = w;
      canvas.height = h;
      const ctx = canvas.getContext("2d");
      ctx.drawImage(img, 0, 0, w, h);
      resolve(canvas.toDataURL("image/jpeg", quality));
    };
    img.onerror = () => resolve(dataUrl);
    img.src = dataUrl;
  });
}

const saveHomeBtn = document.getElementById("save-home-btn");
if (saveHomeBtn) {
  saveHomeBtn.addEventListener("click", async () => {
    const selected = document.querySelector('input[name="home-mode"]:checked');
    const mode = selected ? selected.value : "quote";
    localStorage.setItem(HOME_MODE_KEY, mode);

    const fileInput = document.getElementById("home-image-file");
    if (mode === "image" && fileInput && fileInput.files && fileInput.files[0]) {
      try {
        const raw = await fileToDataUrl(fileInput.files[0]);
        const resized = await resizeImageDataUrl(raw);
        localStorage.setItem(HOME_IMAGE_KEY, resized);
      } catch (e) {
        alert("Could not save image");
        return;
      }
    }

    if (mode === "image" && !localStorage.getItem(HOME_IMAGE_KEY)) {
      alert("Choose an image file first");
      return;
    }

    loadHomeUI();
    alert("Home screen saved");
  });
}

loadHomeUI();

loadData();

