function extractClassId(url) {
  if (!url) return null;
  const str = String(url).trim();
  const match = str.match(/(?:lectures|c|lecture|id=)\/??(\d+)/i) || str.match(/^(\d+)$/);
  return match ? match[1] : null;
}

async function getDirectMediaSources(classId) {
  try {
    const res = await fetch(`https://api.torahanytime.com/lectures/${classId}`);
    if (!res.ok) throw new Error(`API status ${res.status}`);
    const data = await res.json();

    return {
      success: true,
      title: data.title || data.topic || "TorahAnytime Class",
      videoUrl: data.video_url || data.mp4_url || data.media_url || null,
      audioUrl: data.audio_url || data.mp3_url || null
    };
  } catch (err) {
    return { success: false, error: err.message };
  }
}

async function playClassByUrlOrId(input) {
  const classId = extractClassId(input);
  if (!classId) {
    alert("Could not find a valid Class ID in: " + input);
    return;
  }

  const media = await getDirectMediaSources(classId);
  const modal = document.getElementById("video-modal");
  const player = document.getElementById("app-player");
  const title = document.getElementById("video-title");

  const streamUrl = (media.success && media.videoUrl) ? media.videoUrl :
                    (media.success && media.audioUrl) ? media.audioUrl :
                    `https://www.torahanytime.com/lectures/${classId}`;

  if (title) title.innerText = media.title || "Playing Class";
  if (player && modal) {
    player.src = streamUrl;
    modal.style.display = "flex";
    player.play();
  }
}

function closeVideoModal() {
  const modal = document.getElementById("video-modal");
  const player = document.getElementById("app-player");
  if (player) {
    player.pause();
    player.src = "";
  }
  if (modal) modal.style.display = "none";
}
