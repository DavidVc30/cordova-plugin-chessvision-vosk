'use strict';
var exec = require('cordova/exec');
module.exports = {
  initialize: function (ok, error) { exec(ok, error, 'ChessVisionVosk', 'initialize', []); },
  start: function (onEvent, error) { exec(onEvent, error, 'ChessVisionVosk', 'start', []); },
  stop: function (ok, error) { exec(ok, error, 'ChessVisionVosk', 'stop', []); }
};
