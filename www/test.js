const { JSDOM } = require('jsdom');
const fs = require('fs');
const path = require('path');

let html = fs.readFileSync(path.join(__dirname, 'index.html'), 'utf8');
// Strip the <script src="script.js"> tag — jsdom's resource loader would
// otherwise auto-execute it, and we inject it manually below (once) so we
// can stub fetch/alert first. Injecting it twice caused the previous
// "Identifier 'quotes' has already been declared" error.
html = html.replace(/<script\s+src="script\.js"\s*>\s*<\/script>/, '');
// Strip the stylesheet link too — resources:'usable' tries to fetch it
// from the fake https://anchor.local/ origin, which doesn't exist, and
// prints a "Could not load link" warning. Harmless, but noisy — CSS
// doesn't matter for testing script.js's logic anyway.
html = html.replace(/<link\s+rel="stylesheet"\s+href="styles\.css"\s*>/, '');

const dom = new JSDOM(html, {
  runScripts: 'dangerously',
  resources: 'usable',
  // A real-looking URL (not file://) avoids jsdom's opaque-origin
  // restriction on localStorage.
  url: 'https://anchor.local/'
});

dom.window.fetch = () => Promise.reject(new Error('no network in test'));
dom.window.alert = () => {};

const scriptContent = fs.readFileSync(path.join(__dirname, 'script.js'), 'utf8');
const scriptEl = dom.window.document.createElement('script');
scriptEl.textContent = scriptContent;

try {
  dom.window.document.body.appendChild(scriptEl);
  console.log('✅ script.js executed cleanly against real index.html');
} catch (e) {
  console.error('❌ Runtime Error in script.js:', e);
  process.exit(1);
}
