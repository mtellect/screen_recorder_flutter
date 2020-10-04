import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

typedef void OnRecordingStarted(bool recording, String message);
typedef void OnRecodingCompleted(String path);

class ScreenRecorderFlutter {
  static const MethodChannel _channel =
      const MethodChannel('screen_recorder_flutter');

  static init(
      {@required OnRecordingStarted onRecordingStarted,
      @required OnRecodingCompleted onRecodingCompleted}) async {
    final bool ready = await _channel.invokeMethod('init');
    print("Recorder ready $ready");
    _channel.setMethodCallHandler((call) async {
      if (call.method == "onRecodingStarted") {
        bool recording = call.arguments['recording'] as bool;
        String message = call.arguments['message'] as String;
        onRecordingStarted(recording, message);
      } else if (call.method == "onRecodingCompleted") {
        String path = call.arguments['path'] as String;
        onRecodingCompleted(path);
      }
      // return null;
    });
  }

  static Future<bool> get startScreenRecord async {
    final bool version = await _channel.invokeMethod('startScreenRecord');
    return version;
  }

  static Future<bool> get stopScreenRecord async {
    final bool version = await _channel.invokeMethod('stopScreenRecord');
    return version;
  }

  static Future<String> get phoneVersion async {
    final String version = await _channel.invokeMethod('phoneVersion');
    return version;
  }
}
