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
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mediapipe.examples.gesturerecognizer.GestureRecognizerHelper
import com.google.mediapipe.examples.gesturerecognizer.MainViewModel
import com.google.mediapipe.examples.gesturerecognizer.OverlayView
import com.google.mediapipe.examples.gesturerecognizer.R
import com.google.mediapipe.examples.gesturerecognizer.databinding.FragmentCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
//, BluetoothConnectionFragment.BluetoothActionListener
class CameraFragment : Fragment(),
    GestureRecognizerHelper.GestureRecognizerListener{

    companion object {
        private const val TAG = "Hand gesture recognizer, CameraFragment"

    }
    private var bluetoothGatt: BluetoothGatt? = null
    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var gestureRecognizerHelper: GestureRecognizerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var defaultNumResults = 1
    private val gestureRecognizerResultAdapter: GestureRecognizerResultsAdapter by lazy {
        GestureRecognizerResultsAdapter().apply {
            updateAdapterSize(defaultNumResults)
        }
    }
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

//    interface BluetoothActionListener {
//        fun sendLetterHToBLEDevice()
//    }


    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService

    @RequiresApi(Build.VERSION_CODES.S)
    fun sendLetterHToBLEDevice() {
        Log.d(TAG, "Inside sendLetterHToBLEDevice method.")

        bluetoothGatt?.services?.forEach { service ->
            Log.d(TAG, "Available service: ${service.uuid}")
        }
        val serviceUuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val characteristicUuid = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

        Log.d(TAG, "val service = bluetoothGatt?.getService(serviceUuid)")
        val service = bluetoothGatt?.getService(serviceUuid)
        Log.d(TAG, "Service retrieved: $service")
        Log.d(TAG, "val characteristic = service?.getCharacteristic(characteristicUuid)")
        val characteristic = service?.getCharacteristic(characteristicUuid)
        Log.d(TAG, "Characteristic retrieved: $characteristic")

        characteristic?.let {
            it.value = byteArrayOf('H'.code.toByte())
//            it.value = "HH".toByteArray(Charsets.US_ASCII)
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Loc permission not granted.");
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Bluetooth permission not granted.");
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
                } else {
                    Log.d(TAG, "Bluetooth scan permission granted. Initiating  sendLetterH...");
                }
            }
            Log.d(TAG, "bluetoothGatt?.writeCharacteristic(it)")
//            CoroutineScope(Dispatchers.Main).launch {
//                delay(10000)
//            }

            bluetoothGatt?.writeCharacteristic(it)
//            Toast.makeText(context, "H Sent", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "DONE: bluetoothGatt?.writeCharacteristic(it)")

        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(
                requireActivity(), R.id.fragment_container
            ).navigate(R.id.action_camera_to_permissions)
        }

        // Start the GestureRecognizerHelper again when users come back
        // to the foreground.
        backgroundExecutor.execute {
            if (gestureRecognizerHelper.isClosed()) {
                gestureRecognizerHelper.setupGestureRecognizer()
            }
        }
        /// added scan onResume()
//        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
//            } else {
//                bluetoothLeScanner?.startScan(leScanCallback)
//
//            }
//            // Consider adding logic to stop the scan after a certain period to save battery
//        } else {
//            // Optionally, you could request the permission here if not already requested
//            // But typically, you would have already requested it before reaching this point
//        }
//        Log.d(TAG2, "BLE scanner stopped.")
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onPause() {
        super.onPause()
        if (this::gestureRecognizerHelper.isInitialized) {
            viewModel.setMinHandDetectionConfidence(gestureRecognizerHelper.minHandDetectionConfidence)
            viewModel.setMinHandTrackingConfidence(gestureRecognizerHelper.minHandTrackingConfidence)
            viewModel.setMinHandPresenceConfidence(gestureRecognizerHelper.minHandPresenceConfidence)
            viewModel.setDelegate(gestureRecognizerHelper.currentDelegate)

            // Close the Gesture Recognizer helper and release resources
            backgroundExecutor.execute { gestureRecognizerHelper.clearGestureRecognizer() }
        }
//        bluetoothLeScanner?.stopScan(leScanCallback)

        /// added stopscan onPAUSE
//        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
//            } else {
//                bluetoothLeScanner?.stopScan(leScanCallback)
//            }
//            // Consider adding logic to stop the scan after a certain period to save battery
//        } else {
//            // Optionally, you could request the permission here if not already requested
//            // But typically, you would have already requested it before reaching this point
//        }
//        Log.d(TAG2, "BLE scanner stopped.")
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDestroyView() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
            }
        }
        bluetoothGatt?.close()
        bluetoothGatt = null // Help GC do its job


        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)
        // if we wanna merge cam and bcf
        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        return fragmentCameraBinding.root
    }

    //ble variables
    private val REQUEST_BLUETOOTH_CONNECT = 101
    private val targetDeviceMacAddress = "3C:A3:08:90:7D:62"
    private val TAG2 = "BT in CameraFragment"


    @RequiresApi(Build.VERSION_CODES.S)
    private fun connectToDevice(device: BluetoothDevice) {
        // Context could be null, hence the call to requireContext() inside onViewCreated.
        val context = context ?: return

        // Connect to the device. You can customize the autoConnect parameter based on your needs.
        // The BluetoothGattCallback is where you'll handle connection changes and Bluetooth GATT operations.

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.BLUETOOTH_SCAN),
                    1
                )
            } else {
                Log.d(TAG2, "Creating Gatt object bluetoothGatt");
                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted, request it
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CONNECT)
                } else {
                    // Permission is granted, you can proceed with Bluetooth operations
                    bluetoothGatt = device.connectGatt(context, false,
                                                        object : BluetoothGattCallback() {
                        override fun onConnectionStateChange( gatt: BluetoothGatt?,
                                                            status: Int,
                                                            newState: Int ) {
                            super.onConnectionStateChange(bluetoothGatt, status, newState)
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                Log.d(TAG2, "Successfully connected to the BLE device.")
//                                Toast.makeText(context, "Successfully connected to BLE device.", Toast.LENGTH_SHORT).show()
                                // Once connected, you can discover services or perform other operations.
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    Log.d(TAG2, "Connected to GATT server.")
                                    if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
                                    } else {
                                        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                                            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
                                        } else {
                                            Log.d(TAG2, "Attempting to start service discovery: " +
                                                    bluetoothGatt?.discoverServices())

                                        }
                                    }

                                }
                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                Log.d(TAG2, "Disconnected from the BLE device.")
                            }
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                            super.onServicesDiscovered(bluetoothGatt, status)
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                Log.d(TAG2, "Services discovered. You can now access the device's services.")
                                Log.d(TAG2, "Services discovered. Available services:")
                                bluetoothGatt?.services?.forEach { service ->
                                    Log.d(TAG2, "Service UUID: ${service.uuid}")
                                    service.characteristics.forEach { characteristic ->
                                        Log.d(TAG2, "  Characteristic UUID: ${characteristic.uuid}")
                                    }
                                }
                            } else {
                                Log.w(TAG2, "Service discovery failed with status: $status")
                            }
                        }
                    })
                }


                // Keep a reference to BluetoothGatt if you need to interact with the device later.
            }
        }
    }

    private val leScanCallback = object : ScanCallback() {

        @RequiresApi(Build.VERSION_CODES.S)
        override fun onScanResult(callbackType: Int,
                                  result: ScanResult?) {

            super.onScanResult(callbackType, result)
            result?.let {
                if (it.device.address.equals(targetDeviceMacAddress, ignoreCase = true)) {
                    // Device found. Now connect to the device.
                    Log.d(TAG2, "Target device found. MAC Address: ${it.device.address}. Connecting...")
                    connectToDevice(it.device)
                    // Optionally, stop scanning since the target device is found.
                    if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
                    } else {
                        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
                        } else {
                            Log.d(TAG2, "Stopping Bluetooth scanning, device found...");
                            bluetoothLeScanner?.stopScan(this)
                        }
                    }

                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG2, "Scan failed with error code: $errorCode")
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startScanning() {
        Log.d(TAG2, "startScanning called (inside startScanning)")
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG2, "startScanning: Bluetooth scan permission not granted. Requesting permission...")
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
            } else {
                Log.d(TAG2, "startScanning: Bluetooth scan permission granted: (bluetoothLeScanner?.startScan(leScanCallback))");
                Log.d(TAG2, "Bluetooth adapter enabled: ${bluetoothAdapter?.isEnabled}")
                Log.d(TAG2, "Bluetooth scanner instance: $bluetoothLeScanner")
//                val settings = ScanSettings.Builder()
//                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                    .build()
//                bluetoothLeScanner?.startScan(null, settings, leScanCallback)


                bluetoothLeScanner?.startScan(leScanCallback)

//                bluetoothLeScanner?.startScan(leScanCallback)
                if (bluetoothGatt == null) {
                    // Device doesn't support Bluetooth
                    Log.d(TAG2, "bluetoothLeScanner?.startScan(leScanCallback): no bluetoothGatt");
                } else {
                    Log.d(TAG2, "bluetoothLeScanner?.startScan(leScanCallback): yes bluetoothGatt");
                }

                Toast.makeText(context, "startScanning: bluetoothLeScanner done.", Toast.LENGTH_SHORT).show()
            }
            // Consider adding logic to stop the scan after a certain period to save battery
        } else {
            // Optionally, you could request the permission here if not already requested
            // But typically, you would have already requested it before reaching this point
//            Toast.makeText(context, "startScanning: Permission for location not granted. Unable to scan for BLE devices.", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //bt khabsa
//        val overlayView = view.findViewById<OverlayView>(R.id.overlay) // Ensure this ID matches your layout
//        overlayView.bluetoothActionListener = object : BluetoothConnectionFragment.BluetoothActionListener {
//            override fun H
//        }

        //bte khabsa 2
//        val overlayView = view.findViewById<OverlayView>(R.id.overlay)
//        overlayView.bluetoothActionListener = this

        with(fragmentCameraBinding.recyclerviewResults) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = gestureRecognizerResultAdapter
        }

        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        // Create the Hand Gesture Recognition Helper that will handle the
        // inference
        backgroundExecutor.execute {
            gestureRecognizerHelper = GestureRecognizerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minHandDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
                minHandTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
                minHandPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
                currentDelegate = viewModel.currentDelegate,
                gestureRecognizerListener = this
            )
        }
        // start scanning directly
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG2, "onViewCreated: Asking permissions")
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            Log.d(TAG2, "onViewCreated: Checking for BT scan permissions...")
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG2, "onViewCreated: Bluetooth scan permission not granted. Requesting permission...")
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
            } else {
                Log.d(TAG2, "onViewCreated: Bluetooth scan permission granted. Initiating Bluetooth scanning...");
                startScanning()

//                Toast.makeText(context, "onViewCreated: Outside startScanning.", Toast.LENGTH_SHORT).show()
            }
            Log.d(TAG2, "onViewCreated: startScanning finished")
        }
        val overlayView = view.findViewById<OverlayView>(R.id.overlay) // Use the actual ID of your OverlayView

        overlayView.bluetoothActionDelegate = object : OverlayView.BluetoothActionDelegate {
            override fun onPerformBluetoothAction() {
                Log.d(TAG2, "inside onPerformBluetoothAction")
                Log.d(TAG2, "calling sendLetterH")
                sendLetterHToBLEDevice()
//                CoroutineScope(Dispatchers.Main).launch {
//                    Log.d(TAG2, "calling sendLetterH")
//                    sendLetterHToBLEDevice()
//                    //delay(10000) // Delay for 5 seconds
//                }
                //sendLetterHToBLEDevice()
            }
        }


        // Attach listeners to UI control widgets
        initBottomSheetControls()
    }

    private fun initBottomSheetControls() {
        // init bottom sheet settings
        fragmentCameraBinding.bottomSheetLayout.detectionThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinHandDetectionConfidence
            )
        fragmentCameraBinding.bottomSheetLayout.trackingThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinHandTrackingConfidence
            )
        fragmentCameraBinding.bottomSheetLayout.presenceThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinHandPresenceConfidence
            )

        // When clicked, lower hand detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.detectionThresholdMinus.setOnClickListener {
            if (gestureRecognizerHelper.minHandDetectionConfidence >= 0.2) {
                gestureRecognizerHelper.minHandDetectionConfidence -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise hand detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.detectionThresholdPlus.setOnClickListener {
            if (gestureRecognizerHelper.minHandDetectionConfidence <= 0.8) {
                gestureRecognizerHelper.minHandDetectionConfidence += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, lower hand tracking score threshold floor
        fragmentCameraBinding.bottomSheetLayout.trackingThresholdMinus.setOnClickListener {
            if (gestureRecognizerHelper.minHandTrackingConfidence >= 0.2) {
                gestureRecognizerHelper.minHandTrackingConfidence -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise hand tracking score threshold floor
        fragmentCameraBinding.bottomSheetLayout.trackingThresholdPlus.setOnClickListener {
            if (gestureRecognizerHelper.minHandTrackingConfidence <= 0.8) {
                gestureRecognizerHelper.minHandTrackingConfidence += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, lower hand presence score threshold floor
        fragmentCameraBinding.bottomSheetLayout.presenceThresholdMinus.setOnClickListener {
            if (gestureRecognizerHelper.minHandPresenceConfidence >= 0.2) {
                gestureRecognizerHelper.minHandPresenceConfidence -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise hand presence score threshold floor
        fragmentCameraBinding.bottomSheetLayout.presenceThresholdPlus.setOnClickListener {
            if (gestureRecognizerHelper.minHandPresenceConfidence <= 0.8) {
                gestureRecognizerHelper.minHandPresenceConfidence += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference.
        // Current options are CPU and GPU
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
            viewModel.currentDelegate, false
        )
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long
                ) {
                    try {
                        gestureRecognizerHelper.currentDelegate = p2
                        updateControlsUi()
                    } catch(e: UninitializedPropertyAccessException) {
                        Log.e(TAG, "GestureRecognizerHelper has not been initialized yet.")

                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    // Update the values displayed in the bottom sheet. Reset recognition
    // helper.
    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.detectionThresholdValue.text =
            String.format(
                Locale.US,
                "%.2f",
                gestureRecognizerHelper.minHandDetectionConfidence
            )
        fragmentCameraBinding.bottomSheetLayout.trackingThresholdValue.text =
            String.format(
                Locale.US,
                "%.2f",
                gestureRecognizerHelper.minHandTrackingConfidence
            )
        fragmentCameraBinding.bottomSheetLayout.presenceThresholdValue.text =
            String.format(
                Locale.US,
                "%.2f",
                gestureRecognizerHelper.minHandPresenceConfidence
            )

        // Needs to be cleared instead of reinitialized because the GPU
        // delegate needs to be initialized on the thread using it when applicable
        backgroundExecutor.execute {
            gestureRecognizerHelper.clearGestureRecognizer()
            gestureRecognizerHelper.setupGestureRecognizer()
        }
        fragmentCameraBinding.overlay.clear()
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        recognizeHand(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun recognizeHand(imageProxy: ImageProxy) {
        gestureRecognizerHelper.recognizeLiveStream(
            imageProxy = imageProxy,
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after a hand gesture has been recognized. Extracts original
    // image height/width to scale and place the landmarks properly through
    // OverlayView. Only one result is expected at a time. If two or more
    // hands are seen in the camera frame, only one will be processed.
    override fun onResults(
        resultBundle: GestureRecognizerHelper.ResultBundle
    ) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {
                // Show result of recognized gesture
                val gestureCategories = resultBundle.results.first().gestures()
                if (gestureCategories.isNotEmpty()) {
                    gestureRecognizerResultAdapter.updateResults(
                        gestureCategories.first()
                    )
                } else {
                    gestureRecognizerResultAdapter.updateResults(emptyList())
                }

                fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
                    String.format("%d ms", resultBundle.inferenceTime)

                // Pass necessary information to OverlayView for drawing on the canvas
                fragmentCameraBinding.overlay.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                // Force a redraw
                fragmentCameraBinding.overlay.invalidate()
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            gestureRecognizerResultAdapter.updateResults(emptyList())

            if (errorCode == GestureRecognizerHelper.GPU_ERROR) {
                fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
                    GestureRecognizerHelper.DELEGATE_CPU, false
                )
            }
        }
    }
}
