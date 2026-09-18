let urls = [];
let dailyDose = [];
let hillelLinks = [];
let davidAshearLinks = [];
let joeyHaberLinks = [];
let yehudaMandelLinks = [];

const MODE_KEY = "anchor-bubble-mode";

async function loadData() {
  try {
    const [uRes, dRes, hRes, daRes, jhRes, ymRes] = await Promise.all([
      fetch("urls.json"),
      fetch("daily_dose.json"),
      fetch("hillel_eisenberg.json"),
      fetch("rdavidashear.json"),
      fetch("rjoeyhaber.json"),
      fetch("ryehudamandel.json")
    ]);
    if (uRes.ok) urls = await uRes.json();
    if (dRes.ok) dailyDose = await dRes.json();
    if (hRes.ok) hillelLinks = await hRes.json();
    if (daRes.ok) davidAshearLinks = await daRes.json();
    if (jhRes.ok) joeyHaberLinks = await jhRes.json();
    if (ymRes.ok) yehudaMandelLinks = await ymRes.json();
  } catch (err) {}
}

const dailyBtn = document.getElementById("daily-dose-btn");
if (dailyBtn) dailyBtn.addEventListener("click", () => {
  if (!dailyDose.length) {
    alert("No Daily Dose links yet.");
    return;
  }
  window.open(dailyDose[Math.floor(Math.random() * dailyDose.length)], "_blank");
});

const hillelBtn = document.getElementById("hillel-btn");
if (hillelBtn) hillelBtn.addEventListener("click", () => {
  if (!hillelLinks.length) {
    alert("No Rabbi Hillel Eisenberg links yet.");
    return;
  }
  window.open(hillelLinks[Math.floor(Math.random() * hillelLinks.length)], "_blank");
});

const daBtn = document.getElementById("david-ashear-btn");
if (daBtn) daBtn.addEventListener("click", () => {
  if (!davidAshearLinks.length) {
    alert("No Rabbi David Ashear links yet.");
    return;
  }
  window.open(davidAshearLinks[Math.floor(Math.random() * davidAshearLinks.length)], "_blank");
});

const jhBtn = document.getElementById("joey-haber-btn");
if (jhBtn) jhBtn.addEventListener("click", () => {
  if (!joeyHaberLinks.length) {
    alert("No Rabbi Joey Haber links yet.");
    return;
  }
  window.open(joeyHaberLinks[Math.floor(Math.random() * joeyHaberLinks.length)], "_blank");
});

const ymBtn = document.getElementById("yehuda-mandel-btn");
if (ymBtn) ymBtn.addEventListener("click", () => {
  if (!yehudaMandelLinks.length) {
    alert("No Rabbi Yehuda Mandel links yet.");
    return;
  }
  window.open(yehudaMandelLinks[Math.floor(Math.random() * yehudaMandelLinks.length)], "_blank");
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

const noteEl = document.getElementById("personal-note");
if (noteEl) noteEl.value = localStorage.getItem("anchor-note") || "";
const saveNoteBtn = document.getElementById("save-note-btn");
if (saveNoteBtn) saveNoteBtn.addEventListener("click", () => {
  if (noteEl) localStorage.setItem("anchor-note", noteEl.value);
  alert("Note saved on this device.");
});

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
  if (window.AnchorNative && window.AnchorNative.setBubbleMode) {
    window.AnchorNative.setBubbleMode(selected.value);
  }
  const status = document.getElementById("mode-status");
  if (status) status.textContent = "Saved: " + selected.value;
  alert("Bubble mode saved: " + selected.value);
});

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
  });
}

document.querySelectorAll(".bubble-corner-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    const corner = btn.getAttribute("data-corner");
    localStorage.setItem("anchor-bubble-corner", corner);
    if (window.AnchorNative && window.AnchorNative.setBubbleCorner) {
      window.AnchorNative.setBubbleCorner(corner);
    }
  });
});

document.querySelectorAll(".bubble-action-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    const action = btn.getAttribute("data-action");
    localStorage.setItem("anchor-bubble-action", action);
    if (window.AnchorNative && window.AnchorNative.setBubbleAction) {
      window.AnchorNative.setBubbleAction(action);
    }
  });
});

loadModeUI();
renderChizukList();
updateChizukButton();

const widgetColorPicker = document.getElementById("widget-color-picker");
const widgetColorHex = document.getElementById("widget-color-hex");
if (widgetColorPicker) {
  const savedColor = localStorage.getItem("anchor-widget-color");
  if (savedColor) widgetColorPicker.value = savedColor;
  if (widgetColorHex) widgetColorHex.textContent = widgetColorPicker.value.toUpperCase();
  widgetColorPicker.addEventListener("input", () => {
    if (widgetColorHex) widgetColorHex.textContent = widgetColorPicker.value.toUpperCase();
  });
  widgetColorPicker.addEventListener("change", () => {
    const color = widgetColorPicker.value;
    localStorage.setItem("anchor-widget-color", color);
    if (window.AnchorNative && window.AnchorNative.setWidgetColor) {
      window.AnchorNative.setWidgetColor(color);
    }
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
  widgetOpacityInput.addEventListener("change", () => {
    const pct = parseInt(widgetOpacityInput.value, 10);
    localStorage.setItem("anchor-widget-opacity", String(pct));
    if (window.AnchorNative && window.AnchorNative.setWidgetAlpha) {
      window.AnchorNative.setWidgetAlpha(Math.round(pct * 255 / 100));
    }
  });
}

const HOME_IMAGE_KEY = "anchor-home-image";

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
    if (status) status.textContent = "No backup found yet.";
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
  if (status) status.textContent = "Backup restored.";
});

loadData();
