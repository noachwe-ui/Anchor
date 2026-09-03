let quotes = [];
let urls = [];

async function loadData() {
  try {
    const [qRes, uRes] = await Promise.all([
      fetch('quotes.json'),
      fetch('urls.json')
    ]);
    quotes = await qRes.json();
    urls = await uRes.json();
    showRandomQuote();
  } catch (err) {
    document.getElementById('quote-text').textContent = "Take a slow breath. You are here now.";
  }
}

function showRandomQuote() {
  if (!quotes.length) return;
  const item = quotes[Math.floor(Math.random() * quotes.length)];
  document.getElementById('quote-text').textContent = item.quote;
  document.getElementById('quote-source').textContent = item.source ? `— ${item.source}` : '';
}

document.getElementById('new-quote-btn').addEventListener('click', showRandomQuote);

document.getElementById('clip-btn').addEventListener('click', () => {
  if (!urls.length) {
    alert('No clips added yet.');
    return;
  }
  const link = urls[Math.floor(Math.random() * urls.length)];
  window.open(link, '_blank');
});

const noteEl = document.getElementById('personal-note');
noteEl.value = localStorage.getItem('anchor-note') || '';

document.getElementById('save-note-btn').addEventListener('click', () => {
  localStorage.setItem('anchor-note', noteEl.value);
  alert('Note saved on this device.');
});

loadData();
