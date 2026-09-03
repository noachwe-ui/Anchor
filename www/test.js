const { JSDOM } = require('jsdom');
const fs = require('fs');

const dom = new JSDOM('<!DOCTYPE html><html><body><button id="new-quote-btn"></button></body></html>', {
  runScripts: 'dangerously',
  resources: 'usable',
  url: 'file://' + __dirname + '/'
});

try {
  const scriptContent = fs.readFileSync('script.js', 'utf8');
  dom.window.eval(scriptContent);
  console.log('✅ script.js executed cleanly with no reference errors!');
} catch (e) {
  console.error('❌ Runtime Error in script.js:', e);
}
