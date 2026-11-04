// Copy the pre-downloaded English Vosk model into Android APK assets.
// Run on every prepare/build because Cordova may regenerate the Android project.
'use strict';
const fs = require('fs');
const path = require('path');
module.exports = function (context) {
  const root = context.opts.projectRoot;
  const pluginRoot = path.resolve(__dirname, '..');
  const modelName = 'vosk-model-small-en-us-0.15.zip';
  const source = path.join(pluginRoot, 'model', modelName);
  const androidDir = path.join(root, 'platforms', 'android');
  if (!fs.existsSync(androidDir)) return;
  if (!fs.existsSync(source) || fs.statSync(source).size < 1000000) {
    throw new Error('SayChess Vosk: bundled model missing. Put the official ' + modelName +
      ' in ' + path.join(pluginRoot, 'model') +
      ' (see README). Build aborted to prevent an APK that cannot recognize speech offline.');
  }
  const destinationDir = path.join(androidDir, 'app', 'src', 'main', 'assets', 'vosk');
  fs.mkdirSync(destinationDir, { recursive: true });
  const destination = path.join(destinationDir, modelName);
  fs.copyFileSync(source, destination);
  console.log('[SayChess Vosk] Bundled model into Android assets (' +
    (fs.statSync(destination).size / 1048576).toFixed(1) + ' MiB).');
};
