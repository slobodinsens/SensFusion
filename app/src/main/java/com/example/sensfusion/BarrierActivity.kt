@file:Suppress("DEPRECATION")

package com.example.sensfusion

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class BarrierActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var deviceAddress: String? = null
    private val requestCodePermissions = 1
    private val discoveredDevices = mutableListOf<BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.barrier)

        // Инициализация кнопок
        val openBarrierButton: CardView = findViewById(R.id.openBarrierButton)
        val closeBarrierButton: CardView = findViewById(R.id.closeBarrierButton)
        val bluetoothSettingsButton: CardView = findViewById(R.id.bluetoothSettingsButton)

        // Вибрация
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        fun vibrate() {
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }

        // Настройки Bluetooth
        bluetoothSettingsButton.setOnClickListener {
            vibrate()
            startDeviceDiscovery()
        }

        // Открытие шлагбаума
//        openBarrierButton.setOnClickListener {
//            vibrate()
//            deviceAddress?.let {
//                val barrier = Barrier(this)
//                if (barrier.connectToDevice(it)) {
//                    barrier.sendCommand("OPEN")
//                    barrier.disconnect()
//                } else {
//                    Log.e("BarrierActivity", "Failed to connect to the device.")
//                }
//            } ?: Log.e("BarrierActivity", "No device selected.")
//        }

        // Закрытие шлагбаума
//        closeBarrierButton.setOnClickListener {
//            vibrate()
//            deviceAddress?.let {
//                val barrier = Barrier(this)
//                if (barrier.connectToDevice(it)) {
//                    barrier.sendCommand("CLOSE")
//                    barrier.disconnect()
//                } else {
//                    Log.e("BarrierActivity", "Failed to connect to the device.")
//                }
//            } ?: Log.e("BarrierActivity", "No device selected.")
//        }

        // Проверка разрешений
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), requestCodePermissions)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDeviceDiscovery() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            return
        }

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        discoveredDevices.clear()
        bluetoothAdapter.startDiscovery()

        // Регистрируем ресивер для обнаружения устройств
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(deviceDiscoveryReceiver, filter)

        // Показываем список устройств после завершения сканирования
        AlertDialog.Builder(this)
            .setTitle("Searching for devices...")
            .setMessage("Scanning nearby devices. Please wait.")
            .setNegativeButton("Cancel") { dialog, _ ->
                bluetoothAdapter.cancelDiscovery()
                unregisterReceiver(deviceDiscoveryReceiver)
                dialog.dismiss()
            }
            .setOnDismissListener {
                showDiscoveredDevicesDialog()
            }
            .show()
    }

    private val deviceDiscoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_FOUND == intent.action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && !discoveredDevices.contains(device)) {
                    discoveredDevices.add(device)
                    Log.i("BarrierActivity", "Discovered device: ${device.name} (${device.address})")
                }
            }
        }
    }

    private fun showDiscoveredDevicesDialog() {
        if (discoveredDevices.isEmpty()) {
            Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceNames = discoveredDevices.map { "${it.name ?: "Unknown"} (${it.address})" }.toTypedArray()
        val deviceMap = discoveredDevices.associateBy { if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
            "${it.name ?: "Unknown"} (${it.address})" }

        AlertDialog.Builder(this)
            .setTitle("Select Bluetooth Device")
            .setItems(deviceNames) { _, which ->
                val selectedDeviceName = deviceNames[which]
                deviceAddress = deviceMap[selectedDeviceName]?.address
                Log.i("BarrierActivity", "Selected device: $selectedDeviceName")
                Toast.makeText(this, "Selected $selectedDeviceName", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(deviceDiscoveryReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e("BarrierActivity", "Receiver not registered: ${e.message}")
        }
    }
}
