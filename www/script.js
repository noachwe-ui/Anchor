let quotes = [];
let urls = [];

const REMOTE_QUOTES = "https://raw.githubusercontent.com/noachwe-ui/Anchor/main/www/quotes.json";
const REMOTE_URLS   = "https://raw.githubusercontent.com/noachwe-ui/Anchor/main/www/urls.json";

async function loadData() {
  // 1) Try remote (GitHub)
  try {
    const [qRes, uRes] = await Promise.all([
      fetch(REMOTE_QUOTES, { cache: "no-store" }),
      fetch(REMOTE_URLS,   { cache: "no-store" })
    ]);
    if (qRes.ok && uRes.ok) {
      quotes = await qRes.json();
      urls   = await uRes.json();
      localStorage.setItem("anchor-quotes-cache", JSON.stringify(quotes));
      localStorage.setItem("anchor-urls-cache",   JSON.stringify(urls));
      showRandomQuote();
      return;
    }
  } catch (e) {
    // offline or remote failed — fall through
  }

  // 2) Try local cache
  try {
    const cq = localStorage.getItem("anchor-quotes-cache");
    const cu = localStorage.getItem("anchor-urls-cache");
    if (cq && cu) {
      quotes = JSON.parse(cq);
      urls   = JSON.parse(cu);
      showRandomQuote();
      return;
    }
  } catch (e) {}

  // 3) Fallback to bundled files inside the APK
  try {
    const [qRes, uRes] = await Promise.all([
      fetch("quotes.json"),
      fetch("urls.json")
    ]);
    quotes = await qRes.json();
    urls   = await uRes.json();
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

// Personal note
const noteEl = document.getElementById("personal-note");
noteEl.value = localStorage.getItem("anchor-note") || "";
document.getElementById("save-note-btn").addEventListener("click", () => {
  localStorage.setItem("anchor-note", noteEl.value);
  alert("Note saved on this device.");
});

// Bubble toggle
const bubbleToggle = document.getElementById("bubble-toggle");
bubbleToggle.checked = localStorage.getItem("anchor-bubble") === "true";
bubbleToggle.addEventListener("change", () => {
  localStorage.setItem("anchor-bubble", bubbleToggle.checked);
  alert(bubbleToggle.checked
    ? "Bubble will appear next time you open the app"
    : "Bubble disabled");
});

// Multiple Chizuk links
function getChizukLinks() {
  try {
    return JSON.parse(localStorage.getItem("anchor-chizuk-links") || "[]");
  } catch {
    return [];
  }
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
    row.style.cssText = "display:flex;align-items:center;gap:8px;margin-bottom:10px;";

    const linkBtn = document.createElement("button");
    linkBtn.textContent = link.length > 38 ? link.substring(0, 35) + "..." : link;
    linkBtn.style.cssText = `
      flex: 1;
      text-align: left;
      background: #fdfbf8;
      border: 2px solid #f0ebe3;
      border-radius: 14px;
      padding: 10px 14px;
      font-family: 'Nunito', sans-serif;
      font-size: 0.9rem;
      color: var(--text);
      cursor: pointer;
    `;
    linkBtn.onclick = () => window.open(link, "_blank");

    const delBtn = document.createElement("button");
    delBtn.textContent = "✕";
    delBtn.style.cssText = `
      background: #ff6b6b;
      color: white;
      border: none;
      border-radius: 50%;
      width: 28px;
      height: 28px;
      font-size: 13px;
      cursor: pointer;
      flex-shrink: 0;
    `;
    delBtn.onclick = (e) => {
      e.stopPropagation();
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
  const links = getChizukLinks();
  btn.style.display = links.length > 0 ? "block" : "none";
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
  if (links.length === 0) return;
  const link = links[Math.floor(Math.random() * links.length)];
  window.open(link, "_blank");
});

renderChizukList();
updateChizukButton();
loadData();
