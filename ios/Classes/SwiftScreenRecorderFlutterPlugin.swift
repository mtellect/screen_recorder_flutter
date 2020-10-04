import Flutter
import UIKit

public class SwiftScreenRecorderFlutterPlugin: NSObject, FlutterPlugin {
    
    let screenRecord = ScreenRecordCoordinator()
    static var methodChannel = FlutterMethodChannel()
    private var isRecording = false
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        methodChannel = FlutterMethodChannel(name: "screen_recorder_flutter", binaryMessenger: registrar.messenger())
        let instance = SwiftScreenRecorderFlutterPlugin()
        registrar.addMethodCallDelegate(instance, channel: methodChannel)
    }
    
    
    
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
        if(call.method == "init"){
            screenRecord.screenRecorder.onRecordingError = {
                ReplayFileUtil.deleteFile()
                self.isRecording = false
            }
            screenRecord.screenRecorder.recordingQua = .high
            result(true)
        }else
            if(call.method == "startScreenRecord"){
                self.startTheRecording()
                result(true)
            }else if(call.method == "stopScreenRecord"){
                self.stopTheRecording()
                result(true)
            }else if(call.method == "phoneVersion"){
                result("iOS " + UIDevice.current.systemVersion)
        }
        
    }
    
    @objc  func startTheRecording(){
        screenRecord.startRecording(recordingHandler: { (error) in
            print("Recording in progress")
        }) { (error) in
            print("Recording Complete ee ")
        }
        self.isRecording = true
        var dict: [String: Any] = [String:Any]()
        dict["recording"] = true
        dict["message"] = "Recording in progress"
        SwiftScreenRecorderFlutterPlugin.methodChannel.invokeMethod("onRecodingStarted", arguments: dict)
    }
    
    @objc  func stopTheRecording(){
        screenRecord.stopRecording()
        self.isRecording = false
        if ReplayFileUtil.isRecordingAvailible(){
            let path = ReplayFileUtil.filePath()
            var dict: [String: String] = [String:String]()
            dict["path"] = path.absoluteString
            SwiftScreenRecorderFlutterPlugin.methodChannel.invokeMethod("onRecodingCompleted", arguments: dict)
             print("Recording Done/Stooped \(path.absoluteString)")
        }
        
    }
}
