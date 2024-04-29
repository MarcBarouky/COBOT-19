package com.google.mediapipe.examples.gesturerecognizer.fragment
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.mediapipe.examples.gesturerecognizer.OverlayView
import com.google.mediapipe.examples.gesturerecognizer.R

import com.google.mediapipe.examples.gesturerecognizer.databinding.FragmentBluetoothConnectionBinding
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
//, OverlayView.GestureActionListener
class BluetoothConnectionFragment : Fragment(){
    private val TAG = "BluetoothConnectionFragment"
    private var _binding: FragmentBluetoothConnectionBinding? = null
    private val binding get() = _binding!!
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private val targetDeviceMacAddress = "3C:A3:08:90:7D:62"
    private val REQUEST_BLUETOOTH_CONNECT = 101
    private var bluetoothGatt: BluetoothGatt? = null
    private var gatt: BluetoothGatt? = null

    private var results: GestureRecognizerResult? = null
//    private var helloDetected = false
//    private var gestureActionListener: OverlayView.GestureActionListener? = null




    // Initialize a callback for BLE scan results.
//    private val leScanCallback = object : ScanCallback() {
//        override fun onScanResult(callbackType: Int, result: ScanResult?) {
//            super.onScanResult(callbackType, result)
//            // Handle each scan result (this is called for every BLE device discovered).
//            result?.let {
//                // Here you might want to connect to the device or do something with the scan result.
//               // Log.d("BLEScan", "Device found: ${it.device.address}")
//            }
//        }
//    }
//// jamal
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        val overlayView = view.findViewById<OverlayView>(R.id.overlay_view)
//        overlayView.setGestureActionListener(this)
//    }

    //overlay LISTENER STUFF
//    @RequiresApi(Build.VERSION_CODES.S)
//    override fun onHelloGestureDetected() {
//        Log.d(TAG, "calling sendLetterH")
//
//        sendLetterHToBLEDevice()
//    }

    // bt listener
//    interface BluetoothActionListener {
//        fun sendLetterHToBLEDevice()
//    }

//    @RequiresApi(Build.VERSION_CODES.S)
//    fun sendLetterHToBLEDevice() {
//        Log.d(TAG, "Inside sendLetterHToBLEDevice method.")
//        val serviceUuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb") // Replace with actual UUID
//        val characteristicUuid = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb") // Replace with actual UUID
//        val service = bluetoothGatt?.getService(serviceUuid)
//        val characteristic = service?.getCharacteristic(characteristicUuid)
//        characteristic?.let {
//            it.value = byteArrayOf('H'.code.toByte())
//            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
//            } else {
//                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
//                } else {
//                    Log.d(TAG, "Bluetooth scan permission granted. Initiating  sendLetterH...");
//                }
//            }
//            bluetoothGatt?.writeCharacteristic(it)
//        }
//    }

    private val leScanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                if (it.device.address.equals(targetDeviceMacAddress, ignoreCase = true)) {
                    // Device found. Now connect to the device.
                    Log.d(TAG, "Target device found. MAC Address: ${it.device.address}. Connecting...")
                    connectToDevice(it.device)
                    // Optionally, stop scanning since the target device is found.
                    if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "asking permissions")
                        ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
                    } else {
                        Log.d(TAG, "Button clicked. Calling startScanning...")
                        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "Bluetooth scan permission not granted. Requesting permission...")
                            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
                        } else {
                            Log.d(TAG, "Bluetooth scan permission granted. Initiating Bluetooth scanning...");
                            bluetoothLeScanner?.stopScan(this)
                        }
                    }

                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun permissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "asking permissions")
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            Log.d(TAG, "Button clicked. Calling startScanning...")
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Bluetooth scan permission not granted. Requesting permission...")
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
            } else {
                Log.d(TAG, "Bluetooth scan permission granted. Initiating Bluetooth scanning...");
            }
            Log.d(TAG, "startScanning finished")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun sendDummy(){
        Log.d(TAG, "Inside sendLetterHToBLEDevice method.")

        bluetoothGatt?.services?.forEach { service ->
            Log.d(TAG, "Available service: ${service.uuid}")
        }
        val serviceUuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb") // Replace with actual UUID
        val characteristicUuid = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb") // Replace with actual UUID\

        Log.d(TAG, "val service = bluetoothGatt?.getService(serviceUuid)")
        val service = bluetoothGatt?.getService(serviceUuid)
        Log.d(TAG, "Service retrieved: $service")
        Log.d(TAG, "val characteristic = service?.getCharacteristic(characteristicUuid)")
        val characteristic = service?.getCharacteristic(characteristicUuid)
        Log.d(TAG, "Characteristic retrieved: $characteristic")

    //    characteristic?.let {
    //        it.value = byteArrayOf('S'.code.toByte())
    //        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
    //            Log.d(TAG, "Loc permission not granted.");
    //            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
    //        } else {
    //            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
    //                Log.d(TAG, "Bluetooth permission not granted.");
    //                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
    //            } else {
    //                Log.d(TAG, "Bluetooth scan permission granted. Initiating  sendLetterH...");
    //            }
    //        }
    //        Log.d(TAG, "bluetoothGatt?.writeCharacteristic(it)")
    //       // bluetoothGatt?.writeCharacteristic(it)
    //        val success = bluetoothGatt?.writeCharacteristic(it)
    //        if (success == true) {
    //            Log.d(TAG, "Successfully wrote 'S' to the characteristic.")
    //        } else {
    //            Log.d(TAG, "Failed to write 'S' to the characteristic.")
    //        }
    //    }

    //    val serviceUuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb") // Replace with actual UUID
    //    val characteristicUuid = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val service2 = bluetoothGatt?.getService(serviceUuid)
        val characteristic2 = service2?.getCharacteristic(characteristicUuid)

        characteristic2?.let { char ->
            char.value = byteArrayOf('S'.code.toByte())
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
                } else {
                    Log.d(TAG, "send S permission granted. Initiating SendS ...");
                }
                Log.d(TAG, "permission check for sendS finished")
            }
            val success = bluetoothGatt?.writeCharacteristic(char)
            if (success == true) {
                Log.d(TAG, "Successfully wrote 'S' to the characteristic.")
            } else {
                Log.d(TAG, "Failed to write 'S' to the characteristic.")
            }
        } ?: run {
            Log.d(TAG, "Service or Characteristic not found.")
        }

    }

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
            Log.d(TAG, "asking permissions")
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            Log.d(TAG, "Button clicked. Calling startScanning...")
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Bluetooth scan permission not granted. Requesting permission...")
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.BLUETOOTH_SCAN),
                    1
                )
            } else {
                Log.d(TAG, "Bluetooth scan permission granted. Initiating Bluetooth scanning...");
                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted, request it
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CONNECT)
                } else {
                    // Permission is granted, you can proceed with Bluetooth operations
                    var gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(
                            gatt: BluetoothGatt?,
                            status: Int,
                            newState: Int
                        ) {
                            super.onConnectionStateChange(gatt, status, newState)
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                Log.d(TAG, "Successfully connected to the BLE device.")
                                // Once connected, you can discover services or perform other operations.
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    Log.d(TAG, "Connected to GATT server.")
                                    if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
                                    } else {
                                        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                                            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
                                        } else {
                                            Log.d(TAG, "Attempting to start service discovery: " +
                                                    gatt?.discoverServices())
                                        }
                                        Log.d(TAG, "onConnectionStateChange: startScanning finished")
                                    }

                                }
                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                Log.d(TAG, "Disconnected from the BLE device.")
                            }
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                            super.onServicesDiscovered(gatt, status)
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                Log.d(TAG, "Services discovered. You can now access the device's services.")
                                Log.d(TAG, "Services discovered. Available services:")
                                gatt?.services?.forEach { service ->
                                    Log.d(TAG, "Service UUID: ${service.uuid}")
                                    service.characteristics.forEach { characteristic ->
                                        Log.d(TAG, "  Characteristic UUID: ${characteristic.uuid}")
                                    }
                                }
                               sendDummy()
                            } else {
                                Log.w(TAG, "Service discovery failed with status: $status")
                            }
                        }
                    })
                }


                // Keep a reference to BluetoothGatt if you need to interact with the device later.
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBluetoothConnectionBinding.inflate(inflater, container, false)
        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated")

        super.onViewCreated(view, savedInstanceState)
        //LISTENER STUFF
//        val overlayView = view.findViewById<OverlayView>(R.id.overlay)
//        overlayView.setGestureActionListener(this)

        Log.d(TAG, "super.onViewCreated")

        binding.connectButton.setOnClickListener {
            Log.d(TAG, "setOnCLickListener")

            if (!requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(context, "BLE not supported", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "asking permissions")
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
            } else {
                Log.d(TAG, "Button clicked. Calling startScanning...")
                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Bluetooth scan permission not granted. Requesting permission...")
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
                } else {
                    Log.d(TAG, "Bluetooth scan permission granted. Initiating Bluetooth scanning...");
                    startScanning()
                }
                Log.d(TAG, "connectButton: startScanning finished")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startScanning() {
        Log.d(TAG, "startScanning called")
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "if permissions are granted")
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Bluetooth scan permission not granted. Requesting permission...")
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 1)
            } else {
                Log.d(TAG, "Bluetooth scan permission granted. Initiating Bluetooth scanning...");
                if (bluetoothAdapter?.isEnabled() == true) {
                    Log.d(TAG, "ADAPTER ENABLED");
                    bluetoothLeScanner?.startScan(leScanCallback)
                }else{
                    Log.d(TAG, "ADAPTER DISABLED");
                }
//                bluetoothLeScanner?.startScan(leScanCallback)
            }
            // Consider adding logic to stop the scan after a certain period to save battery
        } else {
            Log.d(TAG, "if we get here, we should request for permissions")
            // Optionally, you could request the permission here if not already requested
            // But typically, you would have already requested it before reaching this point
            Toast.makeText(context, "Permission for location not granted. Unable to scan for BLE devices.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

