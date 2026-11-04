# SayChess Vosk — bundled offline English model

The Vosk model is now **packaged inside the APK**. On first launch the plugin extracts the bundled model to Android private storage and loads it. **No internet connection is needed on the phone**, including for first use. The existing Angular `ChessVisionVosk.initialize()` API is unchanged.

## Important: add the model ZIP before building

This source package **does not contain the 40 MB model binary**, because the model download server was inaccessible from the editing environment. Download the official model once on your development PC:

`https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip`

Either run `powershell -ExecutionPolicy Bypass -File scripts\download-model.ps1` from this plugin folder, or manually place the downloaded ZIP at:

`model/vosk-model-small-en-us-0.15.zip`

Do not extract or rename the ZIP. The model must contain `vosk-model-small-en-us-0.15/am/final.mdl`. Check the model license on the upstream model page before distribution.

## Cordova installation / upgrade

1. Place the model ZIP in the plugin's `model` folder **before adding the plugin**.
2. In your Cordova project directory, run:

```powershell
cordova plugin rm cordova-plugin-chessvision-vosk
cordova plugin add "C:\path\to\cordova-plugin-chessvision-vosk-main"
cordova prepare android
cordova build android
```

The `after_prepare` and `before_compile` hooks copy the model into `platforms/android/app/src/main/assets/vosk/`. Builds fail clearly if the model is missing; this avoids accidentally shipping an APK that downloads its model.

## Behavior

- Vosk loads from APK assets, extracts once into the app-private model directory, and reuses it thereafter.
- Speech recognition remains continuous; grammar and JavaScript APIs are unchanged.
- No runtime model download, no internet permission required by this plugin.
- First launch still needs a short **local extraction** step and enough storage space for the extracted model.
- APK/AAB size increases by roughly the compressed model size. On-device storage includes both the APK asset and the extracted model.
- Existing app installations with a valid extracted model reuse it.

## Verify

Check that the built APK contains `assets/vosk/vosk-model-small-en-us-0.15.zip` and test airplane-mode first launch on a clean installation. This plugin has not been compiled or tested on a device in this environment.
