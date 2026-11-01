package com.chessvision.vosk;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ChessVisionVosk extends CordovaPlugin implements RecognitionListener {
  private static final String MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip";
  private final Handler main = new Handler(Looper.getMainLooper());
  private Model model;
  private Recognizer recognizer;
  private SpeechService speechService;
  private CallbackContext streamCallback;
  private volatile boolean loading = false;
  private CallbackContext permissionStart;
  private static final int AUDIO_REQUEST = 5403;

  @Override public boolean execute(String action, JSONArray args, CallbackContext cb) {
    switch (action) {
      case "initialize":
        if (model != null) { cb.success(); return true; }
        if (loading) { cb.error("Model initialization already running"); return true; }
        loading = true;
        cordova.getThreadPool().execute(() -> {
          try {
            File modelDir = installModel();
            model = new Model(modelDir.getAbsolutePath());
            cb.success();
          } catch (Exception e) { cb.error("Vosk initialization: " + e.getMessage()); }
          finally { loading = false; }
        });
        return true;
      case "start":
        if (model == null) { cb.error("Vosk model not initialized"); return true; }
        if (!cordova.hasPermission(Manifest.permission.RECORD_AUDIO)) {
          permissionStart = cb;
          cordova.requestPermission(this, AUDIO_REQUEST, Manifest.permission.RECORD_AUDIO);
          return true;
        }
        startStream(cb);
        return true;
      case "stop":
        main.post(() -> { releaseStream(); cb.success(); });
        return true;
      default: return false;
    }
  }

  @Override public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode != AUDIO_REQUEST) return;
    CallbackContext cb = permissionStart;
    permissionStart = null;
    if (cb == null) return;
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startStream(cb);
    else cb.error("Microphone permission denied");
  }

  private void startStream(CallbackContext cb) {
        main.post(() -> {
          try {
            if (speechService != null) { cb.error("Vosk stream already active"); return; }
            recognizer = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(recognizer, 16000.0f);
            streamCallback = cb;
            speechService.startListening(this);
            emit("ready", "");
          } catch (Exception e) { releaseStream(); cb.error("Vosk start: " + e.getMessage()); }
        });
  }

  private void emit(String type, String text) {
    CallbackContext cb = streamCallback;
    if (cb == null) return;
    try {
      JSONObject obj = new JSONObject();
      obj.put("type", type); obj.put("text", text); obj.put("timestampMs", System.currentTimeMillis());
      PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
      result.setKeepCallback(true);
      cb.sendPluginResult(result);
    } catch (Exception ignored) { }
  }

  private String recognizedText(String json, String field) {
    try { return new JSONObject(json).optString(field, "").trim(); }
    catch (Exception ignored) { return ""; }
  }
  @Override public void onPartialResult(String hypothesis) {
    String value = recognizedText(hypothesis, "partial");
    if (!value.isEmpty()) emit("partial", value);
  }
  @Override public void onResult(String hypothesis) {
    String value = recognizedText(hypothesis, "text");
    if (!value.isEmpty()) emit("result", value);
  }
  @Override public void onFinalResult(String hypothesis) {
    String value = recognizedText(hypothesis, "text");
    if (!value.isEmpty()) emit("result", value);
  }
  @Override public void onError(Exception e) { emit("error", e.getMessage()); }
  @Override public void onTimeout() { emit("timeout", ""); }

  private void releaseStream() {
    CallbackContext old = streamCallback;
    streamCallback = null;
    if (speechService != null) { speechService.stop(); speechService.shutdown(); speechService = null; }
    if (recognizer != null) { recognizer.close(); recognizer = null; }
    if (old != null) { PluginResult end = new PluginResult(PluginResult.Status.NO_RESULT); end.setKeepCallback(false); old.sendPluginResult(end); }
  }
  @Override public void onPause(boolean multitasking) { releaseStream(); super.onPause(multitasking); }
  @Override public void onDestroy() { releaseStream(); if (model != null) { model.close(); model = null; } super.onDestroy(); }

  private File installModel() throws Exception {
    File root = new File(cordova.getActivity().getFilesDir(), "chessvision-vosk");
    File target = new File(root, "vosk-model-small-en-us-0.15");
    if (new File(target, "am/final.mdl").exists()) return target;
    if (!root.exists() && !root.mkdirs()) throw new Exception("Cannot create model folder");
    File staging = new File(root, "unpacking");
    if (staging.exists()) deleteRecursive(staging);
    if (!staging.mkdirs()) throw new Exception("Cannot create staging folder");
    HttpURLConnection connection = (HttpURLConnection) new URL(MODEL_URL).openConnection();
    connection.setConnectTimeout(15000); connection.setReadTimeout(30000);
    connection.setInstanceFollowRedirects(true);
    if (connection.getResponseCode() != 200) throw new Exception("Model download HTTP " + connection.getResponseCode());
    long total = 0;
    try (InputStream input = connection.getInputStream(); ZipInputStream zip = new ZipInputStream(input)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        File out = new File(staging, entry.getName());
        if (!out.getCanonicalPath().startsWith(staging.getCanonicalPath() + File.separator)) throw new Exception("Unsafe model ZIP entry");
        if (entry.isDirectory()) { if (!out.exists() && !out.mkdirs()) throw new Exception("Cannot create model folder"); }
        else {
          if (!out.getParentFile().exists() && !out.getParentFile().mkdirs()) throw new Exception("Cannot create model folder");
          try (FileOutputStream output = new FileOutputStream(out)) {
            byte[] buf = new byte[16384]; int n;
            while ((n = zip.read(buf)) != -1) {
              total += n;
              if (total > 250L * 1024 * 1024) throw new Exception("Model archive too large");
              output.write(buf, 0, n);
            }
          }
        }
        zip.closeEntry();
      }
    } finally { connection.disconnect(); }
    File unpacked = new File(staging, "vosk-model-small-en-us-0.15");
    if (!new File(unpacked, "am/final.mdl").exists()) throw new Exception("Downloaded model incomplete");
    if (target.exists()) deleteRecursive(target);
    if (!unpacked.renameTo(target)) throw new Exception("Cannot install model");
    deleteRecursive(staging);
    return target;
  }
  private static void deleteRecursive(File f) {
    File[] children = f.listFiles(); if (children != null) for (File c : children) deleteRecursive(c);
    f.delete();
  }
}
