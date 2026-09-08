let quotes = [];
let urls = [];

const REMOTE_QUOTES = "https://raw.githubusercontent.com/noachwe-ui/Anchor/main/www/quotes.json";
const REMOTE_URLS   = "https://raw.githubusercontent.com/noachwe-ui/Anchor/main/www/urls.json";
const MODE_KEY = "anchor-bubble-mode";

async function loadData() {
  try {
    const [qRes, uRes] = await Promise.all([
      fetch(REMOTE_QUOTES, { cache: "no-store" }),
      fetch(REMOTE_URLS, { cache: "no-store" })
    ]);
    if (qRes.ok && uRes.ok) {
      quotes = await qRes.json();
      urls = await uRes.json();
      localStorage.setItem("anchor-quotes-cache", JSON.stringify(quotes));
      localStorage.setItem("anchor-urls-cache", JSON.stringify(urls));
      showRandomQuote();
      return;
    }
  } catch (e) {}

  try {
    const cq = localStorage.getItem("anchor-quotes-cache");
    const cu = localStorage.getItem("anchor-urls-cache");
    if (cq && cu) {
      quotes = JSON.parse(cq);
      urls = JSON.parse(cu);
      showRandomQuote();
      return;
    }
  } catch (e) {}

  try {
    const [qRes, uRes] = await Promise.all([fetch("quotes.json"), fetch("urls.json")]);
    quotes = await qRes.json();
    urls = await uRes.json();
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

document.getElementById("new-quote-btn").addEventListener("click", showRandomQuote);

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
loadData();
