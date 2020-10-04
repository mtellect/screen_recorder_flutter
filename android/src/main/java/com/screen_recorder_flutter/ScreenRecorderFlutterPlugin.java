package com.screen_recorder_flutter;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.app.Activity.RESULT_OK;

/** ScreenRecorderFlutterPlugin */
public class ScreenRecorderFlutterPlugin implements FlutterPlugin, MethodCallHandler, PluginRegistry.ActivityResultListener, PluginRegistry.RequestPermissionsResultListener   {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;


  //Permissions
  private static final int SCREEN_RECORD_REQUEST_CODE = 777;
  private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
  private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
  private boolean hasPermissions = false;

  //private PluginRegistry.Registrar mPluginRegistrar;
  HBRecorder hbRecorder;
  private Context context;
  private Activity activity;


  public ScreenRecorderFlutterPlugin() {
    Log.v("mtellect Here.... ", "created");
  }

  public ScreenRecorderFlutterPlugin(Registrar registrar) {
    channel = new MethodChannel(registrar.messenger(), "screen_recorder_flutter");
    channel.setMethodCallHandler(this);
    context=registrar.context();
    activity=registrar.activity();

  }


  public static void registerWith(Registrar registrar) {
    if (registrar.activity() == null) return;
     ScreenRecorderFlutterPlugin plugin = new ScreenRecorderFlutterPlugin(registrar);
    registrar.addActivityResultListener(plugin);
    registrar.addRequestPermissionsResultListener(plugin);
    Log.v("mtellect Here.... ", "created");
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "screen_recorder_flutter");
    context=flutterPluginBinding.getApplicationContext();
    activity=(Activity) context.getApplicationContext();
    channel.setMethodCallHandler(this);
    Log.v("mtellect Here.... 111 ", "created");


  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

    if(call.method.equals("init")){
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        hbRecorder = new HBRecorder(context, new HBRecorderListener() {
          @Override
          public void HBRecorderOnStart() {
            Map<String, Object> argument = new HashMap<>();
            argument.put("recording",true);
            argument.put("message","Recording in progress");
            channel.invokeMethod("onRecodingStarted",argument);
          }

          @Override
          public void HBRecorderOnComplete() {
            Map<String, Object> argument = new HashMap<>();
            argument.put("path",mUri.getPath());
            channel.invokeMethod("onRecodingCompleted",argument);
          }

          @Override
          public void HBRecorderOnError(int errorCode, String reason) {
            Map<String, Object> argument = new HashMap<>();
            argument.put("recording",false);
            argument.put("message","Recording error "+reason);
            channel.invokeMethod("onRecodingStarted",argument);
          }
        });
      }
      result.success(true);
    }
    else
    if(call.method.equals("startScreenRecord")){
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
          hasPermissions = true;
        }
        if (hasPermissions) {
          //check if recording is in progress
          //and stop it if it is
          if (hbRecorder.isBusyRecording()) {
            hbRecorder.stopScreenRecording();
          }
          //else start recording
          else {
            startRecordingScreen();
          }
        } else {
          showLongToast("This library requires API 21>");
        }
      }
      result.success(true);
    } else
    if(call.method.equals("stopScreenRecord")){
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        hbRecorder.stopScreenRecording();
      }
      result.success(true);
    }
    else
    if(call.method.equals("phoneVersion")){
      result.success("Android " + Build.VERSION.RELEASE);
    }


  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void startRecordingScreen() {
    quickSettings();
    MediaProjectionManager mediaProjectionManager  = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    Intent permissionIntent   = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
    activity.startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
  }


  //Get/Set the selected settings
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void quickSettings() {
    hbRecorder.setAudioBitrate(128000);
    hbRecorder.setAudioSamplingRate(44100);
    hbRecorder.recordHDVideo(true);
    hbRecorder.isAudioEnabled(true);
    //Customise Notification
//    hbRecorder.setNotificationSmallIcon(drawable2ByteArray(R.drawable.icon));
//    hbRecorder.setNotificationTitle("Recording your screen");
//    hbRecorder.setNotificationDescription("Drag down to stop the recording");

  }


  //Create Folder
  //Only call this on Android 9 and lower (getExternalStoragePublicDirectory is deprecated)
  //This can still be used on Android 10> but you will have to add android:requestLegacyExternalStorage="true" in your Manifest
  private void createFolder() {
    File f1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "HBRecorder");
    if (!f1.exists()) {
      if (f1.mkdirs()) {
        Log.i("Folder ", "created");
      }
    }
  }


  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  //Check if permissions was granted
  private boolean checkSelfPermission(String permission, int requestCode) {
    if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
      return false;
    }
    return true;
  }


  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Override
  public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    switch (requestCode) {
      case PERMISSION_REQ_ID_RECORD_AUDIO:
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
          return true;
        } else {
          hasPermissions = false;
          showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
        }
        break;
      case PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE:
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          hasPermissions = true;
          //Permissions was provided
          //Start screen recording
          startRecordingScreen();
          return true;
        } else {
          hasPermissions = false;
          showLongToast("No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        break;
      default:
        break;
    }
    return false;
  }


  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
      if (resultCode == RESULT_OK) {
        //Start screen recording
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          setOutputPath();
          hbRecorder.startScreenRecording(data, resultCode, activity);
        }
        return  true;
      }
    }
    return false;
  }



  //For Android 10> we will pass a Uri to HBRecorder
  //This is not necessary - You can still use getExternalStoragePublicDirectory
  //But then you will have to add android:requestLegacyExternalStorage="true" in your Manifest
  //IT IS IMPORTANT TO SET THE FILE NAME THE SAME AS THE NAME YOU USE FOR TITLE AND DISPLAY_NAME
  ContentResolver resolver;
  ContentValues contentValues;
  Uri mUri;
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void setOutputPath() {




    String filename = generateFileName();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      resolver = context.getContentResolver();
      contentValues = new ContentValues();
      contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "HBRecorder");
      contentValues.put(MediaStore.Video.Media.TITLE, filename);
      contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
      contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
      mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
      //FILE NAME SHOULD BE THE SAME
      hbRecorder.setFileName(filename);
      hbRecorder.setOutputUri(mUri);
    }else{
      createFolder();
      hbRecorder.setOutputPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) +"/HBRecorder");
    }
  }


  //Show Toast
  private void showLongToast(final String msg) {
    Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
  }

  //Generate a timestamp to be used as a file name
  private String generateFileName() {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
    Date curDate = new Date(System.currentTimeMillis());
    return formatter.format(curDate).replace(" ", "");
  }

/*  @Override
  public void HBRecorderOnStart() {
     Map<String, Object> argument = new HashMap<>();
    argument.put("recording",true);
    argument.put("message","Recording in progress");
    channel.invokeMethod("onRecodingStarted",argument);
  }

  @Override
  public void HBRecorderOnComplete() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      hbRecorder.wasUriSet();
      Map<String, Object> argument = new HashMap<>();
      argument.put("path",mUri.getPath());
      channel.invokeMethod("onRecodingCompleted",argument);
    }
  }

  @Override
  public void HBRecorderOnError(int errorCode, String reason) {
    Map<String, Object> argument = new HashMap<>();
    argument.put("recording",false);
    argument.put("message","Recording error "+reason);
    channel.invokeMethod("onRecodingStarted",argument);
  }*/


}
