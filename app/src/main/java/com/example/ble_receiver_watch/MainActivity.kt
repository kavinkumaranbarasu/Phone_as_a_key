package com.example.ble_receiver_watch

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.UUID

//@RequiresApi(api = Build.VERSION_CODES.S)
class MainActivity : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var handler: Handler? = null
    private var deviceNameTextView: TextView? = null
    private val Service_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
    private val BATTERY_LEVEL_UUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")
    private var smartwatchDevice: BluetoothDevice? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissions()
        deviceNameTextView = findViewById(R.id.deviceNameTextView)
        val scanbtn = findViewById<Button>(R.id.scanbtn)
        handler = Handler(Looper.getMainLooper())
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Welcome to RE App", Toast.LENGTH_SHORT).show()
        }
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_ADMIN), PERMISSION_REQUEST_LOCATION)
        }
        scanbtn.setOnClickListener { view: View? ->
            if (bluetoothAdapter != null && bluetoothAdapter!!.isEnabled) {
                //devices.clear();
                scanAndConnect()
            } else {
                Toast.makeText(this, "Bt is not enabled", Toast.LENGTH_SHORT).show()
            }
        }
        if (!checkBluetoothPermissions()) {
            return
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        val permission1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
        val permission2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        val permission3 = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
        val permission4 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val missingPermissions: MutableList<String> = ArrayList()
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (permission2 != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (permission3 != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (permission4 != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray<String>(), PERMISSION_REQUEST_BLUETOOTH)
            return false
        }
        return true
    }

    private fun checkPermissions() {
        val permission1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                runtimepermission,
                1
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (bluetoothAdapter != null && !bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            startActivity(enableBtIntent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                scanAndConnect()
            } else {
                Toast.makeText(this, "Bluetooth must be enabled to use this app", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanAndConnect()
            } else {
                Toast.makeText(this, "Location permission is required for Bluetooth scanning", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private var isScanning = false
    private fun scanAndConnect() {
        if (!isScanning) {
            isScanning = true
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            bluetoothAdapter!!.getBluetoothLeScanner().startScan(scanCallback)
            Log.d(TAG, "Scanning for devices...")
        }
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (isScanning) {
                stopScan()
            }
        }, 5000)
    }

    private fun getDeviceFromScanResult(result: ScanResult): BluetoothDevice {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter.getRemoteDevice(result.device.getAddress())
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            //TAG(Log.i(device.getAddress());
            Log.i("something", "scan working")
            //String deviceAddress = device.getAddress();
            if (!deviceAddresses.contains(device.getAddress())) {
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                deviceAddresses.add(device.getAddress() + " " + device.getName())
                if (deviceAddresses.contains(SMARTWATCH_MAC_ADDRESS)) {
                    smartwatchDevice = device
                }
                Log.d(TAG, "Device addresses: $deviceAddresses")
            }
            if (smartwatchDevice != null && deviceAddresses.contains(smartwatchDevice!!.getAddress())) {
                connectToDevice(smartwatchDevice!!)
            }
            //              deviceListAdapter.add(device.getName() != null ? device.getName() : "Unknown");
            if (device.getAddress() == SMARTWATCH_MAC_ADDRESS) {
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                bluetoothAdapter!!.getBluetoothLeScanner().stopScan(this)
                connectToDevice(device)
            }
            //            else {
//                Toast.makeText(MainActivity.this, "Wrong Device - Click correct device", Toast.LENGTH_SHORT).show();
//                return;
//
//            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "onScanFailed: error code $errorCode")
            stopScan()
        }
    }
    private fun startScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        bluetoothAdapter!!.getBluetoothLeScanner().startScan(scanCallback)
        isScanning = true
    }

    private fun stopScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        bluetoothAdapter!!.getBluetoothLeScanner().stopScan(scanCallback)
        isScanning = false
    }

    private val deviceAddresses: MutableSet<String> = HashSet()

    private fun updateDeviceList() {
        Log.d(TAG, "Updating device list...")
        Log.d(TAG, "DeviceAddresses values: $deviceAddresses")
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_BLUETOOTH_CONNECT)
        } else {
            val bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    super.onConnectionStateChange(gatt, status, newState)
                    Log.d("MainActivity", "Connection state changed. New state: $newState")
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            // Permission not granted
                            runOnUiThread { Toast.makeText(this@MainActivity, "Permission not granted to connect to Bluetooth devices", Toast.LENGTH_SHORT).show() }
                            return
                        }
                        runOnUiThread(Runnable {
                            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                return@Runnable
                            }
                            Toast.makeText(this@MainActivity, "Connected to device: " + device.getName(), Toast.LENGTH_SHORT).show()
                        })
                        if (!gatt.discoverServices()) {
                            Log.e("MainActivity", "Failed to start service discovery")
                        }
                        showDataCollectionDialog(gatt)
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d("MainActivity", "Disconnected from device: " + device.getName())
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    super.onServicesDiscovered(gatt, status)
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val services = gatt.getServices()
                        for (service in services) {
                            Log.i("GattService", "Service UUID: " + service.uuid)
                            val characteristics = service.characteristics
                            for (characteristic in characteristics) {
                                Log.i("GattCharacteristic", "Characteristic UUID: " + characteristic.uuid)
                            }
                        }
                    } else {
                        Log.e("MainActivity", "Service discovery failed with status: $status")
                    }
                }

                override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                    super.onCharacteristicRead(gatt, characteristic, status)
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (BATTERY_LEVEL_UUID == characteristic.uuid) {
                            val batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).toString()
                            Log.d("MainActivity", "Battery Level: $batteryLevel")

                            // Display battery level as a toast message
                            val message = "Battery Level: $batteryLevel"
                            runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
                            Handler(Looper.getMainLooper()).postDelayed({
                                val intent = Intent(this@MainActivity, NFC_Tag::class.java)
                                startActivity(intent)
                            }, 2000)
                        }
                    } else {
                        Log.e("MainActivity", "Characteristic read failed with status: $status")
                    }
                }
            }
            device.connectGatt(this, false, bluetoothGattCallback)
        }
    }

    private fun showDataCollectionDialog(gatt: BluetoothGatt) {
        Log.d("MainActivity", "showDataCollectionDialog called")
        runOnUiThread {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setMessage("Do you want to collect the data from this device?")
                .setTitle("Data Collection")
                .setPositiveButton("Yes") { dialog, id -> // Collect data
                    Log.d("MainActivity", "User clicked Yes on the dialog")
                    readDataFromDevice(gatt)
                }
                .setNegativeButton("No") { dialog, id -> dialog.dismiss() }
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun readDataFromDevice(gatt: BluetoothGatt) {
        val service = gatt.getService(Service_UUID)
        if (service != null) {
            val characteristic = service.getCharacteristic(BATTERY_LEVEL_UUID)
            if (characteristic != null) {
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    return
                }
                gatt.readCharacteristic(characteristic)
            } else {
                Log.e("MainActivity", "Battery level characteristic not found")
            }
        } else {
            Log.e("MainActivity", "Battery service not found")
        }
    }

    private fun showDeviceMismatchDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("The selected device is not the expected device.")
            .setTitle("Device Mismatch")
            .setPositiveButton("OK") { dialog, id -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.")
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.")
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                Log.d(TAG, "Connecting to from GATT server.")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                Log.d(TAG, "Disconnecting from GATT server.")
            }
            Log.d(TAG, "Connected to GATT1 server.")
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services = gatt.getServices()
                for (service in services) {
                    Log.i("GattService", "Service UUID: " + service.uuid)
                    val characteristics = service.characteristics
                    for (characteristic in characteristics) {
                        Log.i("GattCharacteristic", "Characteristic UUID: " + characteristic.uuid)
                    }
                }
            } else {
                Log.e("MainActivity", "Service discovery failed with status: $status")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val DEVICE_NAME2 = characteristic.getStringValue(0)
                Log.d(TAG, "DEVICE NAME: $DEVICE_NAME2")
                handler!!.post { deviceNameTextView!!.text = "DEVICE NAME: $DEVICE_NAME2" }
            }
            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            gatt.disconnect()
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.getSimpleName()
        private const val REQUEST_ENABLE_BT = 1
        private const val PERMISSION_REQUEST_LOCATION = 2
        private const val PERMISSION_REQUEST_BLUETOOTH = 3
        private const val PERMISSION_REQUEST_BLUETOOTH_CONNECT = 4
        private const val SMARTWATCH_MAC_ADDRESS = "59:D3:BF:36:49:C3" // "64:5D:F4:A6:08:FA" - Galaxy  "F7:6B:D5:29:36:8F"  - GSW "F8:C9:19:1A:07:36" - Fire-bolt
        private val runtimepermission = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}