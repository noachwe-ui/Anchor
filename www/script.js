let urls = [];
let dailyDose = [];
let hillelLinks = [];

const MODE_KEY = "anchor-bubble-mode";

async function loadData() {
  // Local-only: read the JSON files bundled inside the app. No network calls.
  try {
    const [uRes, dRes, hRes] = await Promise.all([
      fetch("urls.json"),
      fetch("daily_dose.json"),
      fetch("hillel_eisenberg.json")
    ]);
    if (uRes.ok) urls = await uRes.json();
    if (dRes.ok) dailyDose = await dRes.json();
    if (hRes.ok) hillelLinks = await hRes.json();
  } catch (err) {}
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
  if (links.length) list.className = "link-list";
  links.forEach((link, index) => {
    const row = document.createElement("div");
    row.className = "link-row";

    const linkBtn = document.createElement("button");
    linkBtn.className = "link-label";
    linkBtn.textContent = link;
    linkBtn.title = link;
    linkBtn.onclick = () => window.open(link, "_blank");

    const delBtn = document.createElement("button");
    delBtn.textContent = "🗑";
    delBtn.className = "icon-btn-sm";
    delBtn.setAttribute("aria-label", "Remove link");
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
  if (!btn) return;
  btn.classList.toggle("is-hidden", getChizukLinks().length === 0);
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

// Bubble appearance (color + opacity + reappear corner).
const bubbleColorPicker = document.getElementById("bubble-color-picker");
const bubbleColorHex = document.getElementById("bubble-color-hex");
if (bubbleColorPicker) {
  const savedColor = localStorage.getItem("anchor-bubble-color");
  if (savedColor) bubbleColorPicker.value = savedColor;
  if (bubbleColorHex) bubbleColorHex.textContent = bubbleColorPicker.value.toUpperCase();
  bubbleColorPicker.addEventListener("input", () => {
    if (bubbleColorHex) bubbleColorHex.textContent = bubbleColorPicker.value.toUpperCase();
  });
  bubbleColorPicker.addEventListener("change", () => {
    const color = bubbleColorPicker.value;
    localStorage.setItem("anchor-bubble-color", color);
    if (window.AnchorNative && window.AnchorNative.setBubbleColor) {
      window.AnchorNative.setBubbleColor(color);
    }
    const status = document.getElementById("bubble-appearance-status");
    if (status) status.textContent = "Bubble color updated";
  });
}

const bubbleOpacityInput = document.getElementById("bubble-opacity");
const bubbleOpacityValue = document.getElementById("bubble-opacity-value");
if (bubbleOpacityInput) {
  const savedBubbleOpacity = localStorage.getItem("anchor-bubble-opacity");
  if (savedBubbleOpacity) bubbleOpacityInput.value = savedBubbleOpacity;
  if (bubbleOpacityValue) bubbleOpacityValue.textContent = bubbleOpacityInput.value + "%";
  bubbleOpacityInput.addEventListener("input", () => {
    if (bubbleOpacityValue) bubbleOpacityValue.textContent = bubbleOpacityInput.value + "%";
  });
  bubbleOpacityInput.addEventListener("change", () => {
    const pct = parseInt(bubbleOpacityInput.value, 10);
    localStorage.setItem("anchor-bubble-opacity", String(pct));
    if (window.AnchorNative && window.AnchorNative.setBubbleAlpha) {
      window.AnchorNative.setBubbleAlpha(Math.round(pct * 255 / 100));
    }
    const status = document.getElementById("bubble-appearance-status");
    if (status) status.textContent = "Opacity: " + pct + "%";
  });
}

const bubbleSizeInput = document.getElementById("bubble-size");
const bubbleSizeValue = document.getElementById("bubble-size-value");
if (bubbleSizeInput) {
  const savedSize = localStorage.getItem("anchor-bubble-size");
  if (savedSize) bubbleSizeInput.value = savedSize;
  if (bubbleSizeValue) bubbleSizeValue.textContent = bubbleSizeInput.value + "dp";
  bubbleSizeInput.addEventListener("input", () => {
    if (bubbleSizeValue) bubbleSizeValue.textContent = bubbleSizeInput.value + "dp";
  });
  bubbleSizeInput.addEventListener("change", () => {
    const dp = parseInt(bubbleSizeInput.value, 10);
    localStorage.setItem("anchor-bubble-size", String(dp));
    if (window.AnchorNative && window.AnchorNative.setBubbleSize) {
      window.AnchorNative.setBubbleSize(dp);
    }
    const status = document.getElementById("bubble-appearance-status");
    if (status) status.textContent = "Bubble size: " + dp + "dp";
  });
}

document.querySelectorAll(".bubble-corner-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    const corner = btn.getAttribute("data-corner");
    localStorage.setItem("anchor-bubble-corner", corner);
    if (window.AnchorNative && window.AnchorNative.setBubbleCorner) {
      window.AnchorNative.setBubbleCorner(corner);
    }
    const status = document.getElementById("bubble-appearance-status");
    if (status) status.textContent = "Will reappear: " + corner.replace("_", " ");
  });
});

document.querySelectorAll(".bubble-action-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    const action = btn.getAttribute("data-action");
    localStorage.setItem("anchor-bubble-action", action);
    if (window.AnchorNative && window.AnchorNative.setBubbleAction) {
      window.AnchorNative.setBubbleAction(action);
    }
    const status = document.getElementById("bubble-appearance-status");
    if (status) status.textContent = "Bubble now opens: " + btn.textContent;
  });
});

loadModeUI();
renderChizukList();
updateChizukButton();

// Widget appearance (color + opacity) — applies to all Anchor widgets.
const widgetColorPicker = document.getElementById("widget-color-picker");
const widgetColorHex = document.getElementById("widget-color-hex");
if (widgetColorPicker) {
  const savedColor = localStorage.getItem("anchor-widget-color");
  if (savedColor) widgetColorPicker.value = savedColor;
  if (widgetColorHex) widgetColorHex.textContent = widgetColorPicker.value.toUpperCase();
  widgetColorPicker.addEventListener("input", () => {
    if (widgetColorHex) widgetColorHex.textContent = widgetColorPicker.value.toUpperCase();
  });
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
const widgetTextColorHex = document.getElementById("widget-text-color-hex");
if (widgetTextColorPicker) {
  const savedTextColor = localStorage.getItem("anchor-widget-text-color");
  if (savedTextColor) widgetTextColorPicker.value = savedTextColor;
  if (widgetTextColorHex) widgetTextColorHex.textContent = widgetTextColorPicker.value.toUpperCase();
  widgetTextColorPicker.addEventListener("input", () => {
    if (widgetTextColorHex) widgetTextColorHex.textContent = widgetTextColorPicker.value.toUpperCase();
  });
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
const widgetOpacityValue = document.getElementById("widget-opacity-value");
if (widgetOpacityInput) {
  const savedOpacity = localStorage.getItem("anchor-widget-opacity");
  if (savedOpacity) widgetOpacityInput.value = savedOpacity;
  if (widgetOpacityValue) widgetOpacityValue.textContent = widgetOpacityInput.value + "%";
  widgetOpacityInput.addEventListener("input", () => {
    if (widgetOpacityValue) widgetOpacityValue.textContent = widgetOpacityInput.value + "%";
  });
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

// Home screen image (local file)
const HOME_IMAGE_KEY = "anchor-home-image"; // data URL stored locally

function applyHomeMode() {
  const imageCard = document.getElementById("image-card");
  const img = document.getElementById("home-image");
  const dataUrl = localStorage.getItem(HOME_IMAGE_KEY) || "";

  if (dataUrl) {
    if (imageCard) imageCard.hidden = false;
    if (img) img.src = dataUrl;
  } else if (imageCard) {
    imageCard.hidden = true;
  }
}

function loadHomeUI() {
  const status = document.getElementById("home-status");
  if (status) {
    const hasImg = !!localStorage.getItem(HOME_IMAGE_KEY);
    status.textContent = hasImg ? "Image saved" : "No image saved yet";
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
    const fileInput = document.getElementById("home-image-file");
    if (!fileInput || !fileInput.files || !fileInput.files[0]) {
      alert("Choose an image file first");
      return;
    }
    try {
      const raw = await fileToDataUrl(fileInput.files[0]);
      const resized = await resizeImageDataUrl(raw);
      localStorage.setItem(HOME_IMAGE_KEY, resized);
    } catch (e) {
      alert("Could not save image");
      return;
    }

    loadHomeUI();
    alert("Home screen saved");
  });
}

loadHomeUI();

// Backup & Restore — fully local: reads/writes the same localStorage keys
// every other control here already uses, and re-applies each one through
// the exact same functions/bridge calls as when the user changes a
// setting manually. Nothing is written to disk or sent anywhere.
const BACKUP_KEYS = [
  "anchor-note", "anchor-chizuk-links", "anchor-bubble-mode",
  "anchor-bubble-color", "anchor-bubble-opacity", "anchor-bubble-size",
  "anchor-bubble-corner", "anchor-bubble-action",
  "anchor-widget-color", "anchor-widget-text-color", "anchor-widget-opacity",
  "anchor-home-image"
];

const exportBackupBtn = document.getElementById("export-backup-btn");
if (exportBackupBtn) exportBackupBtn.addEventListener("click", () => {
  const data = {};
  BACKUP_KEYS.forEach((k) => {
    const v = localStorage.getItem(k);
    if (v !== null) data[k] = v;
  });
  const status = document.getElementById("backup-status");
  if (!(window.AnchorNative && window.AnchorNative.saveBackupFile)) {
    if (status) status.textContent = "Backup isn't available on this build.";
    return;
  }
  const result = window.AnchorNative.saveBackupFile(JSON.stringify(data));
  if (status) status.textContent = result === "ok" ? "Backup saved." : "Could not save backup.";
});

const importBackupBtn = document.getElementById("import-backup-btn");
if (importBackupBtn) importBackupBtn.addEventListener("click", () => {
  const status = document.getElementById("backup-status");
  if (!(window.AnchorNative && window.AnchorNative.loadBackupFile)) {
    if (status) status.textContent = "Restore isn't available on this build.";
    return;
  }
  const raw = window.AnchorNative.loadBackupFile();
  if (!raw) {
    if (status) status.textContent = "No backup found yet — tap Backup first.";
    return;
  }
  let data;
  try {
    data = JSON.parse(raw);
  } catch (e) {
    if (status) status.textContent = "Backup file is corrupted.";
    return;
  }
  Object.keys(data).forEach((k) => localStorage.setItem(k, data[k]));

  if (data["anchor-note"] !== undefined && noteEl) noteEl.value = data["anchor-note"];
  if (data["anchor-chizuk-links"] !== undefined) { renderChizukList(); updateChizukButton(); }
  if (data["anchor-bubble-mode"] !== undefined && window.AnchorNative && window.AnchorNative.setBubbleMode) {
    window.AnchorNative.setBubbleMode(data["anchor-bubble-mode"]);
  }
  if (data["anchor-bubble-color"] !== undefined) {
    if (bubbleColorPicker) bubbleColorPicker.value = data["anchor-bubble-color"];
    if (bubbleColorHex) bubbleColorHex.textContent = data["anchor-bubble-color"].toUpperCase();
    if (window.AnchorNative && window.AnchorNative.setBubbleColor) window.AnchorNative.setBubbleColor(data["anchor-bubble-color"]);
  }
  if (data["anchor-bubble-opacity"] !== undefined) {
    if (bubbleOpacityInput) bubbleOpacityInput.value = data["anchor-bubble-opacity"];
    if (bubbleOpacityValue) bubbleOpacityValue.textContent = data["anchor-bubble-opacity"] + "%";
    if (window.AnchorNative && window.AnchorNative.setBubbleAlpha) {
      window.AnchorNative.setBubbleAlpha(Math.round(parseInt(data["anchor-bubble-opacity"], 10) * 255 / 100));
    }
  }
  if (data["anchor-bubble-size"] !== undefined) {
    if (bubbleSizeInput) bubbleSizeInput.value = data["anchor-bubble-size"];
    if (bubbleSizeValue) bubbleSizeValue.textContent = data["anchor-bubble-size"] + "dp";
    if (window.AnchorNative && window.AnchorNative.setBubbleSize) {
      window.AnchorNative.setBubbleSize(parseInt(data["anchor-bubble-size"], 10));
    }
  }
  if (data["anchor-bubble-corner"] !== undefined && window.AnchorNative && window.AnchorNative.setBubbleCorner) {
    window.AnchorNative.setBubbleCorner(data["anchor-bubble-corner"]);
  }
  if (data["anchor-bubble-action"] !== undefined && window.AnchorNative && window.AnchorNative.setBubbleAction) {
    window.AnchorNative.setBubbleAction(data["anchor-bubble-action"]);
  }
  if (data["anchor-widget-color"] !== undefined) {
    if (widgetColorPicker) widgetColorPicker.value = data["anchor-widget-color"];
    if (widgetColorHex) widgetColorHex.textContent = data["anchor-widget-color"].toUpperCase();
    if (window.AnchorNative && window.AnchorNative.setWidgetColor) window.AnchorNative.setWidgetColor(data["anchor-widget-color"]);
  }
  if (data["anchor-widget-text-color"] !== undefined) {
    if (widgetTextColorPicker) widgetTextColorPicker.value = data["anchor-widget-text-color"];
    if (widgetTextColorHex) widgetTextColorHex.textContent = data["anchor-widget-text-color"].toUpperCase();
    if (window.AnchorNative && window.AnchorNative.setWidgetTextColor) window.AnchorNative.setWidgetTextColor(data["anchor-widget-text-color"]);
  }
  if (data["anchor-widget-opacity"] !== undefined) {
    if (widgetOpacityInput) widgetOpacityInput.value = data["anchor-widget-opacity"];
    if (widgetOpacityValue) widgetOpacityValue.textContent = data["anchor-widget-opacity"] + "%";
    if (window.AnchorNative && window.AnchorNative.setWidgetAlpha) {
      window.AnchorNative.setWidgetAlpha(Math.round(parseInt(data["anchor-widget-opacity"], 10) * 255 / 100));
    }
  }
  if (data["anchor-home-image"] !== undefined) {
    loadHomeUI();
  }
  loadModeUI();

  if (status) status.textContent = "Backup restored.";
});

loadData();
