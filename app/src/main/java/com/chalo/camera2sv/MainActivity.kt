package com.chalo.camera2sv

//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle

//class MainActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//    }
//}

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.icu.text.SimpleDateFormat
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.Date


class MainActivity : AppCompatActivity() {

    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var changeCameraButton: Button
    private lateinit var recordVid: ImageView
    private lateinit var chronometer: Chronometer
    private var isRecording=false
    private lateinit var currentVidFilePath:String
    private lateinit var characteristics: CameraCharacteristics
    //private var outputFilePath: String = createVidFile().absolutePath


    private val mediaRecorder by lazy{
        MediaRecorder()
    }

    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
            finish()
        }
    }

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            // Surface is created, open camera and start preview
            openCamera()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            // Surface changed, handle accordingly (e.g., update camera parameters)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            // Surface is destroyed, release camera resources
            closeCamera()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        surfaceView = findViewById(R.id.surfaceView)
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(surfaceCallback)
        changeCameraButton=findViewById(R.id.changeCameraButton)
        recordVid=findViewById(R.id.recordVid)
        chronometer=findViewById(R.id.idCMmeter)




//        cameraManager.cam
//        changeCameraButton.setOnClickListener(View.OnClickListener {
//            if (mCameraSource.getCameraFacing() === CameraSource.CAMERA_FACING_FRONT) {
//                changeCameraButton.setText("FRONT CAMERA")
//                if (mCameraSource != null) {
//                    mCameraSource.release()
//                }
//                createCameraSource(CameraSource.CAMERA_FACING_BACK)
//            } else {
//                changeCameraButton.setText("BACK CAMERA")
//                if (mCameraSource != null) {
//                    mCameraSource.release()
//                }
//                createCameraSource(CameraSource.CAMERA_FACING_FRONT)
//            }
//            openCamera()
//        })

        try {
            // Assuming you want to use the rear camera
            cameraId = cameraManager.cameraIdList[1]
            characteristics = cameraManager.getCameraCharacteristics(cameraId)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        changeCameraButton.setOnClickListener(View.OnClickListener {
            closeCamera()
            if(cameraId==cameraManager.cameraIdList[1])
            {
                cameraId=cameraManager.cameraIdList[2]
            }
            else{
                cameraId=cameraManager.cameraIdList[1]
            }
            openCamera()
        })

        recordVid.setOnClickListener {
            Toast.makeText(this, "Button Pressed", Toast.LENGTH_SHORT).show()

            if(isRecording)
            {
                isRecording=false
                stopRecordSession()
            }
            else{
                isRecording=true
                startRecordSession()
            }
        }

        val hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
        Log.d("HardwareLevel","hardwareLevel="+hardwareLevel)


    }

    private fun openCamera() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }

//        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CAMERA_PERMISSION)
//        }
    }

    private fun recordSession(){
        setupMediaRecorder()

        val textureSurface = surfaceHolder.surface
        val recordSurface=mediaRecorder.surface
        try {
            val captureRequestBuilder =
                cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                    ?: throw IllegalStateException("CameraDevice is null")
            captureRequestBuilder.addTarget(textureSurface)
            captureRequestBuilder.addTarget(recordSurface)
            val surfaces= ArrayList<Surface>().apply{
                add(textureSurface)
                add(recordSurface)
            }
            //apply {
//                surfaces.add(textureSurface)
//                surfaces.add(recordSurface)
            //}

            cameraDevice?.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return

                        val captureRequest = captureRequestBuilder.build()
                        session.setRepeatingRequest(captureRequest, null, backgroundHandler)
                        isRecording=true
                        mediaRecorder.start()

                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(this@MainActivity, "Creating Record Session Failed", Toast.LENGTH_SHORT).show()
                    }
                },
                backgroundHandler
            )

        }
        catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun createCameraPreviewSession() {
        // Set up camera preview here using the Surface
        val surface = surfaceHolder.surface

        try {
            val captureRequestBuilder =
                cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    ?: throw IllegalStateException("CameraDevice is null")

            captureRequestBuilder.addTarget(surface)

            cameraDevice?.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return

                        val captureRequest = captureRequestBuilder.build()
                        session.setRepeatingRequest(captureRequest, null, backgroundHandler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(this@MainActivity, "Failed", Toast.LENGTH_SHORT).show()
                    }
                },
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    private fun setupMediaRecorder(){
        val rotation=this.windowManager.defaultDisplay.rotation
        //val sensorOrientation=characteristics     //************CALL this after CameraCgharacteristics have been assigned in onCreate

        characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val sensorOrienatation: Int? = characteristics.get<Int>(CameraCharacteristics.SENSOR_ORIENTATION)
        //val so=cameraManager.getCameraCharacteristics(cameraId())

        Log.d("ORIENTATION","sensorOrienatation="+sensorOrienatation)
        Log.d("ORIENTATION","rotation="+rotation)
        //mediaRecorder.setOrientationHint(90)
        when(sensorOrienatation){
            SENSOR_DEFAULT_ORIENTATION_DEGREES->{
                mediaRecorder.setOrientationHint(DEFAULT_ORIENTATION.get(rotation!!))
                Log.d("ORIENTATION","setOrientationHintD="+DEFAULT_ORIENTATION.get(rotation!!))
            }
            SENSOR_INVERSE_ORIENTATION_DEGREES->{
                mediaRecorder.setOrientationHint(INVERSE_ORIENTATION.get(rotation!!))
                Log.d("ORIENTATION","setOrientationHintI="+INVERSE_ORIENTATION.get(rotation!!))
            }
        }

//        var orientation = 0
//        orientation = if (context is Activity) {
//            (context as Activity).windowManager.defaultDisplay.rotation
//        } else {
//            (context.getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
//        }

        val display = (this.getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        val orientation = display.rotation

        Log.d("ORIENTATION","display.rotation="+orientation)
        val outputPath:String=createVidFile().absolutePath
        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        //val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appDirectory = File(downloadsDirectory, "YourAppDirectoryName")

//        if (!appDirectory.exists()) {
//            val directoryCreated = appDirectory.mkdir()
//            if (!directoryCreated) {
//                // Failed to create the directory
//                Log.d("File_Dir","File directory not created")
//            }
//        }
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        mediaRecorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE) //without this illegal state exception, on .camera failed to get a surface
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) //illegal state, can save .3gpp files, change here and below for file name also
            setOutputFile(outputPath)
            setVideoEncodingBitRate(10000000) //storage changing,,,,,IllegalArg Exception only when bitrate =0 or less, i.e. should be positive,
            setVideoFrameRate(25)   //RuntimeException: setVideoFrameRate failed for 2000, more the frameRate, more blurred the video getting
            setVideoSize(1920,1080) //Changing this results in onConfigureFailed for Record Session
            setVideoEncoder(MediaRecorder.VideoEncoder.H264) //H264 for encoding high-quality video at lower bit rates
            prepare()
        }
    }

    private fun stopMediaRecorder(){
        mediaRecorder.apply{
            //Put try-catch block around stop() as using stop before start/not capturing stream perfectly or pressing stop instantly after start can give IllegalStateException so to catch that
            try {
                stop()
                reset()
            }
            catch(e:java.lang.IllegalStateException) {
                Log.e("Error",e.toString())
            }
        }
    }

    private fun closeCamera() {
        cameraDevice?.close()
        cameraDevice = null
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = backgroundThread?.looper?.let { Handler(it) }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun startRecordSession(){
        startChronometer()
        recordSession()
    }

    private fun stopRecordSession(){
        stopChronometer()
        stopMediaRecorder()
        createCameraPreviewSession()
    }

    private fun startChronometer(){
        chronometer.base=SystemClock.elapsedRealtime()
        chronometer.setTextColor(resources.getColor(android.R.color.holo_red_dark,null))
        chronometer.start()
    }

    private fun stopChronometer(){
        chronometer.setTextColor(resources.getColor(android.R.color.black,null))
        chronometer.stop()
    }

    private fun createVidFileName():String{
        val timeStamp=SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return "VIDEO_${timeStamp}.3gpp"
    }

    private fun createVidFile(): File {
        //val mediaStorageDir = File(this.getExternalFilesDir(Environment.DIRECTORY_DCIM), "YourAppVideosFolder2")
        val mediaStorageDir=File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"YourAppDirectoryName")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            return File("") // Return an empty file if directory creation fails
        }
        return File(
            mediaStorageDir.path + File.separator +
                    "VID_" + System.currentTimeMillis() + ".mp4"
        )
//        val vidfile=File(filesDir,createVidFileName())
//        currentVidFilePath=vidfile.absolutePath
//        return vidfile
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1
        private val SENSOR_DEFAULT_ORIENTATION_DEGREES=90
        private val SENSOR_INVERSE_ORIENTATION_DEGREES=270
        private val DEFAULT_ORIENTATION=SparseIntArray().apply {
            append(Surface.ROTATION_0,90)
            append(Surface.ROTATION_90,0)
            append(Surface.ROTATION_180,270)
            append(Surface.ROTATION_270,180)
        }
        private val INVERSE_ORIENTATION=SparseIntArray().apply {
            append(Surface.ROTATION_0,270)
            append(Surface.ROTATION_90,180)
            append(Surface.ROTATION_180,90)
            append(Surface.ROTATION_270,0)
        }
    }
}