let quotes = [];
let urls = [];
let dailyDose = [];
let hillelLinks = [];

const REMOTE_QUOTES = "https://raw.githubusercontent.com/noachwe-ui/Anchor/main/www/quotes.json";
const REMOTE_URLS   = "https://raw.githubusercontent.com/noachwe-ui/Anchor/main/www/urls.json";
const REMOTE_DAILY  = "https://raw.githubusercontent.com/noachwe-ui/Anchor/main/www/daily_dose.json";
const REMOTE_HILLEL = "https://raw.githubusercontent.com/noachwe-ui/Anchor/main/www/hillel_eisenberg.json";
const MODE_KEY = "anchor-bubble-mode";

async function loadData() {
  // Try remote first
  try {
    const [qRes, uRes, dRes, hRes] = await Promise.all([
      fetch(REMOTE_QUOTES, { cache: "no-store" }),
      fetch(REMOTE_URLS, { cache: "no-store" }),
      fetch(REMOTE_DAILY, { cache: "no-store" }),
      fetch(REMOTE_HILLEL, { cache: "no-store" })
    ]);
    if (qRes.ok) {
      quotes = await qRes.json();
      localStorage.setItem("anchor-quotes-cache", JSON.stringify(quotes));
    }
    if (uRes.ok) {
      urls = await uRes.json();
      localStorage.setItem("anchor-urls-cache", JSON.stringify(urls));
    }
    if (dRes.ok) {
      dailyDose = await dRes.json();
      localStorage.setItem("anchor-daily-cache", JSON.stringify(dailyDose));
    }
    if (hRes.ok) {
      hillelLinks = await hRes.json();
      localStorage.setItem("anchor-hillel-cache", JSON.stringify(hillelLinks));
    }
    if (quotes.length) showRandomQuote();
    if (quotes.length || urls.length || dailyDose.length || hillelLinks.length) return;
  } catch (e) {}

  // Local cache
  try {
    const cq = localStorage.getItem("anchor-quotes-cache");
    const cu = localStorage.getItem("anchor-urls-cache");
    const cd = localStorage.getItem("anchor-daily-cache");
    const ch = localStorage.getItem("anchor-hillel-cache");
    if (cq) quotes = JSON.parse(cq);
    if (cu) urls = JSON.parse(cu);
    if (cd) dailyDose = JSON.parse(cd);
    if (ch) hillelLinks = JSON.parse(ch);
    if (quotes.length) showRandomQuote();
    if (quotes.length || urls.length || dailyDose.length || hillelLinks.length) return;
  } catch (e) {}

  // Bundled files
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
    document.getElementById("quote-text").textContent =
      "Take a slow breath. You are here now.";
  }
}

function showRandomQuote() {
  if (!quotes.length) return;
  const item = quotes[Math.floor(Math.random() * quotes.length)];
  document.getElementById("quote-text").textContent = item.quote;
  document.getElementById("quote-source").textContent =
    item.source ? `— ${item.source}` : "";
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

document.getElementById("clip-btn").addEventListener("click", () => {
  if (!urls.length) {
    alert("No clips added yet.");
    return;
  }
  const link = urls[Math.floor(Math.random() * urls.length)];
  window.open(link, "_blank");
});

// Notes
const noteEl = document.getElementById("personal-note");
noteEl.value = localStorage.getItem("anchor-note") || "";
document.getElementById("save-note-btn").addEventListener("click", () => {
  localStorage.setItem("anchor-note", noteEl.value);
  alert("Note saved on this device.");
});

// Screen navigation
const mainScreen = document.getElementById("screen-main");
const settingsScreen = document.getElementById("screen-settings");

document.getElementById("open-settings-btn").addEventListener("click", () => {
  mainScreen.hidden = true;
  settingsScreen.hidden = false;
});

document.getElementById("back-main-btn").addEventListener("click", () => {
  settingsScreen.hidden = true;
  mainScreen.hidden = false;
});

// Bubble mode
function getMode() {
  return localStorage.getItem(MODE_KEY) || "always";
}

function loadModeUI() {
  const mode = getMode();
  const input = document.querySelector(`input[name="bubble-mode"][value="${mode}"]`);
  if (input) input.checked = true;
  document.getElementById("mode-status").textContent = "Current mode: " + mode;
}

document.getElementById("save-mode-btn").addEventListener("click", () => {
  const selected = document.querySelector('input[name="bubble-mode"]:checked');
  if (!selected) return;
  localStorage.setItem(MODE_KEY, selected.value);
  document.getElementById("mode-status").textContent =
    "Saved: " + selected.value + ". Reopen Anchor once to apply.";
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
  btn.style.display = getChizukLinks().length ? "block" : "none";
}

document.getElementById("add-chizuk-btn").addEventListener("click", () => {
  const input = document.getElementById("chizuk-link");
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

document.getElementById("chizuk-btn").addEventListener("click", () => {
  const links = getChizukLinks();
  if (!links.length) return;
  window.open(links[Math.floor(Math.random() * links.length)], "_blank");
});

loadModeUI();
renderChizukList();
updateChizukButton();


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
