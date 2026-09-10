#!/data/data/com.termux/files/usr/bin/bash
set -e
cd /data/data/com.termux/files/home/Anchor

echo "Files to update:"
ls -l www/*.json 2>/dev/null || true

git add www/quotes.json www/urls.json www/daily_dose.json www/hillel_eisenberg.json 2>/dev/null || true
git add www/*.json 2>/dev/null || true

if git diff --cached --quiet; then
  echo "No JSON changes to push."
  exit 0
fi

MSG="${1:-Update content JSON files}"
git commit -m "$MSG"
git push origin main

echo "Done. Open Anchor online once to sync."
