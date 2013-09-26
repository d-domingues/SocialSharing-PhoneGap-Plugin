package nl.xservices.plugins;

import android.content.Intent;
import android.net.Uri;
import org.apache.cordova.api.CallbackContext;

import org.apache.cordova.api.CordovaPlugin;
import org.apache.cordova.api.PluginResult;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.*;
import java.net.URL;

public class SocialSharing extends CordovaPlugin {

  private static final String ACTION_AVAILABLE_EVENT = "available";
  private static final String ACTION_SHARE_EVENT = "share";

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    try {
      if (ACTION_AVAILABLE_EVENT.equals(action)) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
        return true;
      } else if (ACTION_SHARE_EVENT.equals(action)) {
        final String message = args.getString(0);
        final String subject = args.getString(1);
        final String image = args.getString(2);
        doSendIntent(subject, message, image);
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
        return true;
      } else {
        callbackContext.error("socialSharing." + action + " is not a supported function. Did you mean '" + ACTION_SHARE_EVENT + "'?");
        return false;
      }
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      return false;
    }
  }

  private void doSendIntent(String subject, String message, String image) throws IOException {
    final Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND);
    final String dir = getDownloadDir();
    createOrCleanDownloadDir(dir);
    sendIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

    String localImage = image;
    if ("".equals(image) || "null".equalsIgnoreCase(image)) {
      sendIntent.setType("text/plain");
    } else {
      sendIntent.setType("image/*"); // TODO future support for any type of file!?
      if (image.startsWith("http") || image.startsWith("www/")) {
        final String filename = getFileName(image);
        localImage = "file://" + dir + "/" + filename;
        if (image.startsWith("http")) {
          downloadFromUrl(new URL(image).openConnection().getInputStream(), dir, filename);
        } else {
          downloadFromUrl(webView.getContext().getAssets().open(image), dir, filename);
        }
      } else if (!image.startsWith("file://")) {
        throw new IllegalArgumentException("URL_NOT_SUPPORTED");
      }
      sendIntent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.parse(localImage));
    }
    if (!"".equals(subject) && !"null".equalsIgnoreCase(subject)) {
      sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
    }
    if (!"".equals(message) && !"null".equalsIgnoreCase(message)) {
      sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
    }

    this.cordova.startActivityForResult(this, sendIntent, 0);
  }

  private void createOrCleanDownloadDir(final String downloadDir) throws IOException {
    File dir = new File(downloadDir);
    if (dir.exists()) {
      File[] files = dir.listFiles();
      if (null != files) {
        for (File file : files) {
          if (!file.delete()) {
            // ignore
          }
        }
      }
    } else if (!dir.mkdirs()) {
      throw new IOException("CREATE_DIRS_FAILED");
    }
  }

  private String getDownloadDir() {
    return webView.getContext().getExternalFilesDir(null) + "/socialsharing-downloads";
  }

  private String getFileName(String url) {
    final int lastIndexOfSlash = url.lastIndexOf('/');
    if (lastIndexOfSlash == -1) {
      return url;
    } else {
      return url.substring(lastIndexOfSlash + 1);
    }
  }

  // Note: this may need to be an AsyncTask
  private static void downloadFromUrl(InputStream is, String dirName, String fileName) throws IOException {
    File dir = new File(dirName);
    File file = new File(dir, fileName);
    BufferedInputStream bis = new BufferedInputStream(is);
    ByteArrayBuffer baf = new ByteArrayBuffer(5000);
    int current;
    while ((current = bis.read()) != -1) {
      baf.append((byte) current);
    }
    FileOutputStream fos = new FileOutputStream(file);
    fos.write(baf.toByteArray());
    fos.flush();
    fos.close();
  }
}