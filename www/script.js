// 1. Extract Class ID from any TorahAnytime URL variation
function extractClassId(url) {
  if (!url) return null;
  const str = String(url).trim();
  const match = str.match(/(?:lectures|c|lecture|id=)\/??(\d+)/i) || str.match(/^(\d+)$/);
  return match ? match[1] : null;
}

// 2. Play media directly inside in-app modal with fallbacks
async function playClassByUrlOrId(input, customTitle) {
  const classId = extractClassId(input);
  if (!classId) {
    alert("Could not find a valid Class ID in: " + input);
    return;
  }

  const modal = document.getElementById("video-modal");
  const player = document.getElementById("app-player");
  const title = document.getElementById("video-title");

  // Fallback stream chain: MP4 Video Proxy -> MP3 Audio Proxy
  const primaryUrl = `https://proxier.torahanytime.com/mp4/${classId}.mp4`;
  const fallbackAudioUrl = `https://proxier.torahanytime.com/mp3/${classId}.mp3`;

  if (title) title.innerText = customTitle || ("Class ID: " + classId);
  
  if (player && modal) {
    modal.style.display = "flex";
    player.src = primaryUrl;

    player.onerror = function() {
      console.warn("Direct MP4 stream failed, switching to audio stream fallback...");
      player.src = fallbackAudioUrl;
      player.play().catch(e => console.error("Audio playback error:", e));
    };

    player.play().catch(err => {
      console.warn("Autoplay deferred or blocked:", err);
    });
  }
}

// 3. Close Modal Player
function closeVideoModal() {
  const modal = document.getElementById("video-modal");
  const player = document.getElementById("app-player");
  if (player) {
    player.pause();
    player.removeAttribute("src");
    player.load();
  }
  if (modal) modal.style.display = "none";
}

// 4. Load JSON Feed & Render Interactive Class Buttons
async function loadSpeakerFeed(feedFile) {
  const container = document.getElementById("feed-container");
  if (!container) return;
  
  container.innerHTML = "<p style='color:#aaa;'>Loading feed...</p>";

  try {
    const res = await fetch(feedFile);
    if (!res.ok) throw new Error("Could not load " + feedFile);
    const data = await res.json();

    container.innerHTML = "";
    const list = Array.isArray(data) ? data : (data.items || data.lectures || []);

    if (list.length === 0) {
      container.innerHTML = "<p>No lectures found in this feed.</p>";
      return;
    }

    list.forEach(item => {
      const card = document.createElement("div");
      card.style.cssText = "background:#1e1e1e; padding:12px; margin-bottom:10px; border-radius:8px; text-align:left;";
      
      const itemTitle = item.title || item.topic || "Torah Lecture";
      const itemUrl = item.url || item.link || item.id || "";

      card.innerHTML = `
        <h4 style="margin:0 0 8px 0; color:#fff;">${itemTitle}</h4>
        <button onclick="playClassByUrlOrId('${itemUrl}', '${itemTitle.replace(/'/g, "\\'")}')" 
                style="padding:8px 14px; background:#1e88e5; color:#fff; border:none; border-radius:6px; font-weight:bold; cursor:pointer;">
          ▶ Play Direct Stream
        </button>
      `;
      container.appendChild(card);
    });
  } catch (err) {
    container.innerHTML = `<p style="color:#e53935;">Error: ${err.message}</p>`;
  }
}
