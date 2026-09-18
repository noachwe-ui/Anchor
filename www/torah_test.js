// Extract Class ID from any TorahAnytime URL variation
function extractClassId(url) {
  if (!url) return null;
  const str = String(url).trim();
  const match = str.match(/(?:lectures|c|lecture|id=)\/??(\d+)/i) || str.match(/^(\d+)$/);
  return match ? match[1] : null;
}

// Fetch direct stream details from TorahAnytime API
async function getDirectMediaSources(classId) {
  try {
    const res = await fetch(`https://api.torahanytime.com/lectures/${classId}`);
    if (!res.ok) throw new Error(`API status ${res.status}`);
    const data = await res.json();

    return {
      success: true,
      title: data.title || data.topic || "TorahAnytime Class",
      videoUrl: data.video_url || data.mp4_url || data.media_url || null,
      audioUrl: data.audio_url || data.mp3_url || null,
      youtubeId: data.youtube_id || null
    };
  } catch (err) {
    return {
      success: false,
      error: err.message
    };
  }
}

// Multi-tier playback engine with fallbacks
async function playClassByUrlOrId(input) {
  const classId = extractClassId(input);
  if (!classId) {
    alert("Could not find a valid Class ID in: " + input);
    return;
  }

  console.log("Processing Class ID:", classId);
  const media = await getDirectMediaSources(classId);

  // Tier 1: Direct Video MP4
  if (media.success && media.videoUrl) {
    console.log("Playing primary video:", media.videoUrl);
    window.open(media.videoUrl, "_blank");
    return;
  }

  // Tier 2: YouTube Mirror
  if (media.success && media.youtubeId) {
    const ytUrl = `https://www.youtube.com/watch?v=${media.youtubeId}`;
    console.log("Video MP4 unavailable. Playing YouTube mirror:", ytUrl);
    window.open(ytUrl, "_blank");
    return;
  }

  // Tier 3: Audio Stream MP3
  if (media.success && media.audioUrl) {
    console.log("Video unavailable. Playing audio stream:", media.audioUrl);
    window.open(media.audioUrl, "_blank");
    return;
  }

  // Tier 4: Emergency Fallback to standard web URL
  const fallbackWebUrl = `https://www.torahanytime.com/lectures/${classId}`;
  console.warn("API/Streams failed. Opening original web link:", fallbackWebUrl);
  window.open(fallbackWebUrl, "_blank");
}

// Test call (uncomment or run directly in console to test with Class ID 100000)
// playClassByUrlOrId("https://www.torahanytime.com/lectures/100000");
