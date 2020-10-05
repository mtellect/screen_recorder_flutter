# screen_recorder_flutter


A new Flutter plugin for screen recording. This plug-in requires Android SDK 19+ and iOS 10+


## Getting Started

This plugin can be used for record the screen on Android and iOS devices.

1) Initialize the Plugin

```
bool hasStarted=false;
String message="";
File videoRecordedFile;


ScreenRecorderFlutter.init(onRecordingStarted: (started, msg) {
      hasStarted=started;
      message=msg;
      setState(() {});
    }, onRecodingCompleted: (path) {
      videoRecordedFile = File(path);
      setState(() {});
    });
```


2) For start the recording

```dart
 ScreenRecorderFlutter.startScreenRecord;
```


3) For stop the recording

```dart
 ScreenRecorderFlutter.stopScreenRecord;
```

## Android

Flutter_Screen_Recorder do not request permissions necessary. You can use [Permission_handler](https://pub.dev/packages/permission_handler), a permissions plugin for Flutter.
Require and add the following permissions in your manifest:

```java
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_INTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

## iOS

You only need add the permission message on the Info.plist

	<key>NSPhotoLibraryUsageDescription</key>
	<string>Save video in gallery</string>
	<key>NSMicrophoneUsageDescription</key>
	<string>Save audio in video</string>