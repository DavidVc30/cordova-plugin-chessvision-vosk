# Chess Vision native Vosk Android (experimental)

## Installation

1. Extract this plugin folder outside the Cordova project.
2. In `C:\Prive\Daddy30\Apps\Cordova`, run:
   `cordova plugin add "C:\path\to\chessvision-vosk-plugin"`
3. Keep the existing `cordova-plugin-speechrecognition` installed for the selectable Android fallback.
4. Replace the Angular project's `src` folder with the accompanying Chess Vision Vosk source ZIP.
5. Run your usual Angular build / Cordova prepare / Cordova build script.

## First use

The first Vosk session downloads `vosk-model-small-en-us-0.15.zip` (~40 MB) from
https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
and unpacks it into Android app-private storage. An internet connection is required
**once**. Afterwards recognition runs offline. Initial extraction needs additional
free storage and can take time. This plugin does not bundle the model.

## Behavior

- The microphone stays active across consecutive `listen()` requests in Voice
  Training / Move Trainer, rather than reopening every nine seconds.
- Each `listen()` has a nine-second visual waiting window; if it expires without
  speech, only the JS request ends. The microphone stream is not restarted.
- A nonempty Vosk partial hypothesis changes the button to Processing.
- A nonempty Vosk final utterance result is passed to the existing chess parser.
- On navigation, pause, disabling auto-listening, or match stop, the microphone
  is released; it will reopen when the next session starts.
- Settings can switch between experimental native Vosk and legacy Android recognition.
- The three Android silence thresholds do not apply to Vosk.

## Limitations / test notes

- Experimental Android-first implementation, **not compiled or device-tested**.
- The Android library version and Maven/JNA dependency resolution must be
  verified in your Cordova build environment.
- English small model is general-purpose; chess vocabulary has NOT yet been
  constrained with Vosk grammar. Accuracy must be measured.
- The Processing state uses the first nonempty partial recognition, not a true
  microphone-level VAD. If partial text is delayed, the UI will lag speech onset.
- A 12-second processing watchdog releases the waiting JS request but keeps the
  audio stream running. Late results while idle are discarded.
- An utterance that starts just before the nine-second deadline may need more
  time to produce a partial result. The first partial suspends the countdown.
- Microphone permission is requested on Android.
- Android background pause closes the stream.
- For production, consider shipping a prebundled model or explicit download
  progress, network retry, checksums and model version management.

Vosk: https://alphacephei.com/vosk/  (Apache-2.0 software; check model license separately).
