let urls = [];
let dailyDose = [];
let hillelLinks = [];

const MODE_KEY = "anchor-bubble-mode";

function openLinkOrStream(url) {
  if (!url) return;
  
  // Use _system target to force Capacitor to delegate navigation to the native Android browser engine
  window.open(url, "_system");
}

async function loadData() {
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
  if (!dailyDose.length) { alert("No Daily Dose links yet."); return; }
  openLinkOrStream(dailyDose[Math.floor(Math.random() * dailyDose.length)]);
});

const hillelBtn = document.getElementById("hillel-btn");
if (hillelBtn) hillelBtn.addEventListener("click", () => {
  if (!hillelLinks.length) { alert("No Rabbi Hillel Eisenberg links yet."); return; }
  openLinkOrStream(hillelLinks[Math.floor(Math.random() * hillelLinks.length)]);
});

const clipBtn = document.getElementById("clip-btn");
if (clipBtn) clipBtn.addEventListener("click", () => {
  if (!urls.length) { alert("No clips added yet."); return; }
  openLinkOrStream(urls[Math.floor(Math.random() * urls.length)]);
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

function getMode() { return localStorage.getItem(MODE_KEY) || "always"; }

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
    linkBtn.onclick = () => openLinkOrStream(link);

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
  openLinkOrStream(links[Math.floor(Math.random() * links.length)]);
});

loadModeUI();
renderChizukList();
updateChizukButton();
loadData();
