'use strict';
var exec = require('cordova/exec');
module.exports = {
  initialize: function (ok, error) { exec(ok, error, 'ChessVisionVosk', 'initialize', []); },
  start: function (onEvent, error) { exec(onEvent, error, 'ChessVisionVosk', 'start', []); },
  setGrammar: function (mode, ok, error) { exec(ok, error, 'ChessVisionVosk', 'setGrammar', [mode]); },
  stop: function (ok, error) { exec(ok, error, 'ChessVisionVosk', 'stop', []); }
};
