/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.gesturerecognizer.fragment

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mediapipe.examples.gesturerecognizer.GestureRecognizerHelper
import com.google.mediapipe.examples.gesturerecognizer.MainViewModel
import com.google.mediapipe.examples.gesturerecognizer.databinding.FragmentGalleryBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler
import androidx.core.content.ContextCompat.getSystemService
import com.google.mediapipe.examples.gesturerecognizer.MainActivity

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class GalleryFragment : Fragment() {

    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_FINE_LOCATION = 2
    private val SCAN_PERIOD: Long = 10000 // 10 seconds
    private var scanning = false
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val specificDeviceAddress = "00:11:22:AA:BB:CC" // Example device address
    private var _fragmentGalleryBinding: FragmentGalleryBinding? = null
    private var _binding: GalleryFragment? = null
    private val binding get() = _binding!!
    private val fragmentGalleryBinding
        get() = _fragmentGalleryBinding!!
    //private lateinit var gestureRecognizerHelper: GestureRecognizerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var defaultNumResults = 1
//    private val gestureRecognizerResultsAdapter by lazy {
//        GestureRecognizerResultsAdapter().apply {
//            updateAdapterSize(defaultNumResults)
//        }
//    }

    // GATT Client Callback
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            // Handle connection and disconnection
        }
        // Implement other callback methods like onServicesDiscovered, etc.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bluetoothManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        checkBluetoothSupport()
    }

//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        return inflater.inflate(R.layout.fragment_ble_connection, container, false).apply {
//            findViewById<Button>(R.id.connectButton).setOnClickListener {
//                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOCATION)
//                } else {
//                    scanLeDevice(true)
//                }
//            }
//        }
//    }

// new start
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentGallery.inflate(inflater, container, false)
//        return binding.root
//        _fragmentGalleryBinding =
//            FragmentGalleryBinding.inflate(inflater, container, false)
//
//        return fragmentGalleryBinding.root
//    }



//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        binding.connectButton.setOnClickListener {
//            scanLeDevice(true)
//        }
//    }

    // new end

    private fun checkBluetoothSupport() {
        if (!requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            // Device does not support BLE
        }
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
    }
    private fun scanLeDevice(enable: Boolean) {
        bluetoothAdapter?.bluetoothLeScanner?.let { scanner ->
            when {
                enable && !scanning -> {
                    // Stops scanning after a predefined scan period.
                    Handler().postDelayed({
                        scanning = false
                        scanner.stopScan(leScanCallback)
                    }, SCAN_PERIOD)
                    scanning = true
                    scanner.startScan(leScanCallback)
                }
                else -> {
                    scanning = false
                    scanner.stopScan(leScanCallback)
                }
            }
        }
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                val device = it.device
                if (device.address.equals(specificDeviceAddress, ignoreCase = true)) {
                    connectToDevice(device)
                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(this)
                }
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        device.connectGatt(context, false, gattCallback)
        // Optionally, save this BluetoothGatt to manage the connection
    }
}




//
//class GalleryFragment : Fragment() ,
//    GestureRecognizerHelper.GestureRecognizerListener {
//
//    enum class MediaType {
//        IMAGE,
//        VIDEO,
//        UNKNOWN
//    }
//
//    private var _fragmentGalleryBinding: FragmentGalleryBinding? = null
//    private val fragmentGalleryBinding
//        get() = _fragmentGalleryBinding!!
//    private lateinit var gestureRecognizerHelper: GestureRecognizerHelper
//    private val viewModel: MainViewModel by activityViewModels()
//    private var defaultNumResults = 1
//    private val gestureRecognizerResultsAdapter by lazy {
//        GestureRecognizerResultsAdapter().apply {
//            updateAdapterSize(defaultNumResults)
//        }
//    }
//
//    //ble start
////    private val bluetoothAdapter = MainActivity.BTA
////    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
////    private var scanning = false
////    private val handler = Handler()
////
////    // Stops scanning after 10 seconds.
////    private val SCAN_PERIOD: Long = 10000
////
////    private fun scanLeDevice() {
////        if (!scanning) { // Stops scanning after a pre-defined scan period.
////            handler.postDelayed({
////                scanning = false
////                bluetoothLeScanner.stopScan(leScanCallback)
////            }, SCAN_PERIOD)
////            scanning = true
////            bluetoothLeScanner.startScan(leScanCallback)
////        } else {
////            scanning = false
////            bluetoothLeScanner.stopScan(leScanCallback)
////        }
////    }
//    //ble end
//    /** Blocking ML operations are performed using this executor */
//    private lateinit var backgroundExecutor: ScheduledExecutorService
//
//    private val getContent =
//        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
//            // Handle the returned Uri
//            uri?.let { mediaUri ->
//                when (val mediaType = loadMediaType(mediaUri)) {
//                    MediaType.IMAGE -> runGestureRecognitionOnImage(mediaUri)
//                    MediaType.VIDEO -> runGestureRecognitionOnVideo(mediaUri)
//                    MediaType.UNKNOWN -> {
//                        updateDisplayView(mediaType)
//                        Toast.makeText(
//                            requireContext(),
//                            "Unsupported data type.",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            }
//        }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _fragmentGalleryBinding =
//            FragmentGalleryBinding.inflate(inflater, container, false)
//
//        return fragmentGalleryBinding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        fragmentGalleryBinding.fabGetContent.setOnClickListener {
//            getContent.launch(arrayOf("image/*", "video/*"))
//        }
//
//        with(fragmentGalleryBinding.recyclerviewResults) {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = gestureRecognizerResultsAdapter
//        }
//        initBottomSheetControls()
//    }
//
//    override fun onPause() {
//        fragmentGalleryBinding.overlay.clear()
//        if (fragmentGalleryBinding.videoView.isPlaying) {
//            fragmentGalleryBinding.videoView.stopPlayback()
//        }
//        fragmentGalleryBinding.videoView.visibility = View.GONE
//        super.onPause()
//    }
//
//    private fun initBottomSheetControls() {
//        // init bottom sheet settings
//        updateControlsUi()
//
//        // When clicked, lower detection score threshold floor
//        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdMinus.setOnClickListener {
//            if (viewModel.currentMinHandDetectionConfidence >= 0.2) {
//                viewModel.setMinHandDetectionConfidence(viewModel.currentMinHandDetectionConfidence - 0.1f)
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, raise detection score threshold floor
//        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdPlus.setOnClickListener {
//            if (viewModel.currentMinHandDetectionConfidence <= 0.8) {
//                viewModel.setMinHandDetectionConfidence(viewModel.currentMinHandDetectionConfidence + 0.1f)
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, lower hand tracking score threshold floor
//        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdMinus.setOnClickListener {
//            if (viewModel.currentMinHandTrackingConfidence >= 0.2) {
//                viewModel.setMinHandTrackingConfidence(
//                    viewModel.currentMinHandTrackingConfidence - 0.1f
//                )
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, raise hand tracking score threshold floor
//        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdPlus.setOnClickListener {
//            if (viewModel.currentMinHandTrackingConfidence <= 0.8) {
//                viewModel.setMinHandTrackingConfidence(
//                    viewModel.currentMinHandTrackingConfidence + 0.1f
//                )
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, lower hand presence score threshold floor
//        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdMinus.setOnClickListener {
//            if (viewModel.currentMinHandPresenceConfidence >= 0.2) {
//                viewModel.setMinHandPresenceConfidence(
//                    viewModel.currentMinHandPresenceConfidence - 0.1f
//                )
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, raise hand presence score threshold floor
//        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdPlus.setOnClickListener {
//            if (viewModel.currentMinHandPresenceConfidence <= 0.8) {
//                viewModel.setMinHandPresenceConfidence(
//                    viewModel.currentMinHandPresenceConfidence + 0.1f
//                )
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, change the underlying hardware used for inference. Current options are CPU
//        // GPU, and NNAPI
//        fragmentGalleryBinding.bottomSheetLayout.spinnerDelegate.setSelection(
//            viewModel.currentDelegate,
//            false
//        )
//        fragmentGalleryBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(
//                    p0: AdapterView<*>?,
//                    p1: View?,
//                    p2: Int,
//                    p3: Long
//                ) {
//
//                    viewModel.setDelegate(p2)
//                    updateControlsUi()
//                }
//
//                override fun onNothingSelected(p0: AdapterView<*>?) {
//                    /* no op */
//                }
//            }
//    }
//
//    // Update the values displayed in the bottom sheet. Reset detector.
//    @SuppressLint("NotifyDataSetChanged")
//    private fun updateControlsUi() {
//        if (fragmentGalleryBinding.videoView.isPlaying) {
//            fragmentGalleryBinding.videoView.stopPlayback()
//        }
//        fragmentGalleryBinding.videoView.visibility = View.GONE
//        fragmentGalleryBinding.imageResult.visibility = View.GONE
//        fragmentGalleryBinding.overlay.clear()
//        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdValue.text =
//            String.format(
//                Locale.US, "%.2f", viewModel.currentMinHandDetectionConfidence
//            )
//        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdValue.text =
//            String.format(
//                Locale.US, "%.2f", viewModel.currentMinHandTrackingConfidence
//            )
//        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdValue.text =
//            String.format(
//                Locale.US, "%.2f", viewModel.currentMinHandPresenceConfidence
//            )
//
//        fragmentGalleryBinding.overlay.clear()
//        fragmentGalleryBinding.tvPlaceholder.visibility = View.VISIBLE
//        gestureRecognizerResultsAdapter.updateResults(null)
//        gestureRecognizerResultsAdapter.notifyDataSetChanged()
//    }
//
//    // Load and display the image.
//    private fun runGestureRecognitionOnImage(uri: Uri) {
//        setUiEnabled(false)
//        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
//        updateDisplayView(MediaType.IMAGE)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            val source = ImageDecoder.createSource(
//                requireActivity().contentResolver,
//                uri
//            )
//            ImageDecoder.decodeBitmap(source)
//        } else {
//            MediaStore.Images.Media.getBitmap(
//                requireActivity().contentResolver,
//                uri
//            )
//        }
//            .copy(Bitmap.Config.ARGB_8888, true)
//            ?.let { bitmap ->
//                fragmentGalleryBinding.imageResult.setImageBitmap(bitmap)
//
//                // Run gesture recognizer on the input image
//                backgroundExecutor.execute {
//
//                    gestureRecognizerHelper =
//                        GestureRecognizerHelper(
//                            context = requireContext(),
//                            runningMode = RunningMode.IMAGE,
//                            minHandDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
//                            minHandTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
//                            minHandPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
//                            currentDelegate = viewModel.currentDelegate
//                        )
//
//                    gestureRecognizerHelper.recognizeImage(bitmap)
//                        ?.let { resultBundle ->
//                            activity?.runOnUiThread {
//
//                                    fragmentGalleryBinding.overlay.setResults(
//                                        resultBundle.results[0],
//                                        bitmap.height,
//                                        bitmap.width,
//                                        RunningMode.IMAGE
//                                    )
//
//                                    // This will return an empty list if there are no gestures detected
//                                    if(!resultBundle.results.first().gestures().isEmpty()) {
//                                        gestureRecognizerResultsAdapter.updateResults(
//                                            resultBundle.results.first()
//                                                .gestures().first()
//                                        )
//                                    } else {
//                                        Toast.makeText(
//                                            context,
//                                            "Hands not detected",
//                                            Toast.LENGTH_SHORT).show()
//                                    }
//
//                                setUiEnabled(true)
//                                fragmentGalleryBinding.bottomSheetLayout.inferenceTimeVal.text =
//                                    String.format(
//                                        "%d ms",
//                                        resultBundle.inferenceTime
//                                    )
//                            }
//                        } ?: run {
//                        Log.e(
//                            TAG, "Error running gesture recognizer."
//                        )
//                    }
//
//                    gestureRecognizerHelper.clearGestureRecognizer()
//                }
//            }
//    }
//
//    // Load and display the video.
//    private fun runGestureRecognitionOnVideo(uri: Uri) {
//        setUiEnabled(false)
//        updateDisplayView(MediaType.VIDEO)
//        gestureRecognizerResultsAdapter.updateResults(null)
//        gestureRecognizerResultsAdapter.notifyDataSetChanged()
//
//        with(fragmentGalleryBinding.videoView) {
//            setVideoURI(uri)
//            // mute the audio
//            setOnPreparedListener { it.setVolume(0f, 0f) }
//            requestFocus()
//        }
//
//        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
//        backgroundExecutor.execute {
//
//            gestureRecognizerHelper =
//                GestureRecognizerHelper(
//                    context = requireContext(),
//                    runningMode = RunningMode.VIDEO,
//                    minHandDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
//                    minHandTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
//                    minHandPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
//                    currentDelegate = viewModel.currentDelegate
//                )
//
//            activity?.runOnUiThread {
//                fragmentGalleryBinding.videoView.visibility = View.GONE
//                fragmentGalleryBinding.progress.visibility = View.VISIBLE
//            }
//
//            gestureRecognizerHelper.recognizeVideoFile(uri, VIDEO_INTERVAL_MS)
//                ?.let { resultBundle ->
//                    activity?.runOnUiThread { displayVideoResult(resultBundle) }
//                }
//                ?: run {
//                    activity?.runOnUiThread {
//                        fragmentGalleryBinding.progress.visibility =
//                            View.GONE
//                    }
//                    Log.e(TAG, "Error running gesture recognizer.")
//                }
//
//            gestureRecognizerHelper.clearGestureRecognizer()
//        }
//    }
//
//    // Setup and display the video.
//    private fun displayVideoResult(result: GestureRecognizerHelper.ResultBundle) {
//
//        fragmentGalleryBinding.videoView.visibility = View.VISIBLE
//        fragmentGalleryBinding.progress.visibility = View.GONE
//
//        fragmentGalleryBinding.videoView.start()
//        val videoStartTimeMs = SystemClock.uptimeMillis()
//
//        backgroundExecutor.scheduleAtFixedRate(
//            {
//                activity?.runOnUiThread {
//                    val videoElapsedTimeMs =
//                        SystemClock.uptimeMillis() - videoStartTimeMs
//                    val resultIndex =
//                        videoElapsedTimeMs.div(VIDEO_INTERVAL_MS).toInt()
//
//                    if (resultIndex >= result.results.size || fragmentGalleryBinding.videoView.visibility == View.GONE) {
//                        // The video playback has finished so we stop drawing bounding boxes
//                        setUiEnabled(true)
//                        backgroundExecutor.shutdown()
//                    } else {
//                        fragmentGalleryBinding.overlay.setResults(
//                            result.results[resultIndex],
//                            result.inputImageHeight,
//                            result.inputImageWidth,
//                            RunningMode.VIDEO
//                        )
//                        val categories = result.results[resultIndex].gestures()
//                        if (categories.isNotEmpty()) {
//                            gestureRecognizerResultsAdapter.updateResults(
//                                categories.first()
//                            )
//                        }
//
//                        setUiEnabled(false)
//
//                        fragmentGalleryBinding.bottomSheetLayout.inferenceTimeVal.text =
//                            String.format("%d ms", result.inferenceTime)
//                    }
//                }
//            },
//            0,
//            VIDEO_INTERVAL_MS,
//            TimeUnit.MILLISECONDS
//        )
//    }
//
//    private fun updateDisplayView(mediaType: MediaType) {
//        fragmentGalleryBinding.imageResult.visibility =
//            if (mediaType == MediaType.IMAGE) View.VISIBLE else View.GONE
//        fragmentGalleryBinding.videoView.visibility =
//            if (mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE
//        fragmentGalleryBinding.tvPlaceholder.visibility =
//            if (mediaType == MediaType.UNKNOWN) View.VISIBLE else View.GONE
//    }
//
//    // Check the type of media that user selected.
//    private fun loadMediaType(uri: Uri): MediaType {
//        val mimeType = context?.contentResolver?.getType(uri)
//        mimeType?.let {
//            if (mimeType.startsWith("image")) return MediaType.IMAGE
//            if (mimeType.startsWith("video")) return MediaType.VIDEO
//        }
//
//        return MediaType.UNKNOWN
//    }
//
//    private fun setUiEnabled(enabled: Boolean) {
//        fragmentGalleryBinding.fabGetContent.isEnabled = enabled
//        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdMinus.isEnabled =
//            enabled
//        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdPlus.isEnabled =
//            enabled
//        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdMinus.isEnabled =
//            enabled
//        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdPlus.isEnabled =
//            enabled
//        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdMinus.isEnabled =
//            enabled
//        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdPlus.isEnabled =
//            enabled
//        fragmentGalleryBinding.bottomSheetLayout.spinnerDelegate.isEnabled =
//            enabled
//    }
//
//    private fun recognitionError() {
//        activity?.runOnUiThread {
//            fragmentGalleryBinding.progress.visibility = View.GONE
//            setUiEnabled(true)
//            updateDisplayView(MediaType.UNKNOWN)
//        }
//    }
//
//    override fun onError(error: String, errorCode: Int) {
//        recognitionError()
//        activity?.runOnUiThread {
//            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
//            if (errorCode == GestureRecognizerHelper.GPU_ERROR) {
//                fragmentGalleryBinding.bottomSheetLayout.spinnerDelegate.setSelection(
//                    GestureRecognizerHelper.DELEGATE_CPU,
//                    false
//                )
//            }
//        }
//    }
//
//    override fun onResults(resultBundle: GestureRecognizerHelper.ResultBundle) {
//        // no-op
//    }
//
//    companion object {
//        private const val TAG = "GalleryFragment"
//
//        // Value used to get frames at specific intervals for inference (e.g. every 300ms)
//        private const val VIDEO_INTERVAL_MS = 300L
//    }
//}
