function extractClassId(url) {
  if (!url) return null;
  const str = String(url).trim();
  const match = str.match(/(?:lectures|c|lecture|id=)\/??(\d+)/i) || str.match(/^(\d+)$/);
  return match ? match[1] : null;
}

async function playClassByUrlOrId(input) {
  const classId = extractClassId(input);
  if (!classId) {
    alert("Invalid Class ID or link");
    return;
  }

  const modal = document.getElementById("video-modal");
  const player = document.getElementById("app-player");
  const title = document.getElementById("video-title");

  // Direct media stream proxy URL (bypasses API CORS issues)
  const proxyStreamUrl = `https://proxier.torahanytime.com/mp4/${classId}.mp4`;

  if (title) title.innerText = "Class ID: " + classId;
  
  if (player && modal) {
    modal.style.display = "flex";
    player.src = proxyStreamUrl;
    
    // Fallback handler if MP4 fails to load
    player.onerror = function() {
      console.warn("Direct MP4 stream failed, falling back to audio...");
      player.src = `https://proxier.torahanytime.com/mp3/${classId}.mp3`;
      player.play().catch(e => Log.e("Audio play failed", e));
    };

    player.play().catch(err => {
      console.warn("Autoplay blocked or failed:", err);
    });
  }
}

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
