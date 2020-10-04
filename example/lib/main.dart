import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:screen_recorder_flutter/screen_recorder_flutter.dart';
import 'package:video_player/video_player.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Home(),
    );
  }
}

class Home extends StatefulWidget {
  @override
  _HomeState createState() => _HomeState();
}

class _HomeState extends State<Home> {
  String _platformVersion = 'Unknown';
  File file;

  @override
  void initState() {
    super.initState();
    initPlatformState();
    ScreenRecorderFlutter.init(onRecordingStarted: (started, msg) {
      print("Recording $started $msg");
      _platformVersion = "Recording $started $msg";
      setState(() {});
    }, onRecodingCompleted: (path) {
      print("Recording completed $path");
      _platformVersion = "Recording completed $path";
      file = File(path);
      setState(() {});
    });
  }

  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await ScreenRecorderFlutter.phoneVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Container(
          padding: EdgeInsets.all(15),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('Recording Status: $_platformVersion\n'),
              FlatButton(
                  onPressed: () {
                    ScreenRecorderFlutter.startScreenRecord;
                  },
                  color: Colors.green,
                  child: Center(child: Text("Start Recording"))),
              FlatButton(
                  onPressed: () {
                    ScreenRecorderFlutter.stopScreenRecord;
                  },
                  color: Colors.red,
                  child: Center(child: Text("Stop Recording"))),
              if (null != file)
                FlatButton(
                    onPressed: () {
                      final route = MaterialPageRoute(
                          builder: (b) => PlayRecording(
                                file: file,
                              ));
                      Navigator.push(context, route);
                    },
                    color: Colors.brown,
                    child: Center(child: Text("Preview Recording"))),
            ],
          ),
        ),
      ),
    );
  }
}

class PlayRecording extends StatefulWidget {
  final File file;

  const PlayRecording({Key key, this.file}) : super(key: key);
  @override
  _PlayRecordingState createState() => _PlayRecordingState();
}

class _PlayRecordingState extends State<PlayRecording> {
  bool _isPlaying = false;
  VideoPlayerController controller;

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    controller = VideoPlayerController.file(widget.file)
      ..initialize().then((value) {
        _isPlaying = true;
        setState(() {});
      });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Play Recording"),
      ),
      backgroundColor: Colors.black,
      body: Column(
        children: [
          Expanded(
            child: null == controller
                ? Container(
                    child: Center(
                      child: CircularProgressIndicator(),
                    ),
                  )
                : AspectRatio(
                    aspectRatio: controller.value.aspectRatio,
                    child: VideoPlayer(controller),
                  ),
          ),
          FlatButton(
            child: _isPlaying
                ? Icon(
                    Icons.pause,
                    size: 80.0,
                    color: Colors.white,
                  )
                : Icon(
                    Icons.play_arrow,
                    size: 80.0,
                    color: Colors.white,
                  ),
            onPressed: () async {
              bool isPlaying = controller.value.isPlaying;
              if (isPlaying)
                controller.pause();
              else
                controller.play();
              _isPlaying = isPlaying;
              setState(() {});
            },
          )
        ],
      ),
    );
  }
}
