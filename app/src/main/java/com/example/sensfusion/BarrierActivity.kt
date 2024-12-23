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
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.TextView
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
    private val discoveredDevices: MutableList<BluetoothDevice> = mutableListOf()
    private val scanDuration = 15000L
    private var isReceiverRegistered = false
    private var scanningDialog: AlertDialog? = null // Для управления всплывающим окном

    private lateinit var radarView: RadarView
    private lateinit var connectedDeviceTextView: TextView // Для отображения подключённого устройства

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.barrier)

        connectedDeviceTextView = findViewById(R.id.connectedDeviceTextView) // Инициализация TextView
        val bluetoothSettingsButton: CardView = findViewById(R.id.bluetoothSettingsButton)
        radarView = findViewById(R.id.radarView)

        // Устанавливаем начальный текст TextView
        connectedDeviceTextView.text = "No device connected"

        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        fun vibrate() {
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }

        bluetoothSettingsButton.setOnClickListener {
            vibrate()
            radarView.startSweepAnimation()
            startDeviceDiscovery()
        }

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

        if (!isReceiverRegistered) {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(deviceDiscoveryReceiver, filter)
            isReceiverRegistered = true
        }

        // Открываем всплывающее окно сканирования
        scanningDialog = AlertDialog.Builder(this)
            .setTitle("Scanning for devices...")
            .setNegativeButton("Stop") { dialog, _ ->
                bluetoothAdapter.cancelDiscovery()
                if (isReceiverRegistered) {
                    unregisterReceiver(deviceDiscoveryReceiver)
                    isReceiverRegistered = false
                }
                radarView.stopSweepAnimation()
                dialog.dismiss()
            }
            .show()

        Handler(mainLooper).postDelayed({
            bluetoothAdapter.cancelDiscovery()
            if (isReceiverRegistered) {
                unregisterReceiver(deviceDiscoveryReceiver)
                isReceiverRegistered = false
            }
            radarView.stopSweepAnimation()
            scanningDialog?.dismiss() // Закрываем окно после завершения
            showDiscoveredDevicesDialog()
        }, scanDuration)
    }

    private val deviceDiscoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_FOUND == intent.action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val rssi: Int = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                if (device != null && !discoveredDevices.contains(device)) {
                    discoveredDevices.add(device)
                    radarView.updateDevices(
                        discoveredDevices.map { device ->
                            (device.name ?: "Unknown") to rssi
                        }
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDiscoveredDevicesDialog() {
        if (discoveredDevices.isEmpty()) {
            Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceNames = discoveredDevices.map { it.name ?: "Unknown Device" }
        AlertDialog.Builder(this)
            .setTitle("Available Devices")
            .setItems(deviceNames.toTypedArray()) { _, which ->
                val selectedDevice = discoveredDevices[which]
                deviceAddress = selectedDevice.address

                AlertDialog.Builder(this)
                    .setTitle("Connect to Device")
                    .setMessage("To connect to ${selectedDevice.name}, go to Bluetooth settings.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setPositiveButton("OK", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentlyConnectedDevice(): BluetoothDevice? {
        val bondedDevices = bluetoothAdapter?.bondedDevices ?: return null
        for (device in bondedDevices) {
            try {
                // Проверяем состояние соединения через рефлексию
                val isConnected = device.javaClass
                    .getMethod("isConnected")
                    .invoke(device) as Boolean

                if (isConnected) {
                    return device
                }
            } catch (e: Exception) {
                Log.e("BarrierActivity", "Error checking connection state for ${device.name}: ${e.message}")
            }
        }
        return null
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()

        // Закрываем диалог сканирования, если он ещё активен
        scanningDialog?.dismiss()

        radarView.stopSweepAnimation()

        val connectedDevice = getCurrentlyConnectedDevice()
        if (connectedDevice != null) {
            // Обновляем текст TextView
            connectedDeviceTextView.text = "Connected to: ${connectedDevice.name} (${connectedDevice.address})"
            Log.d("BarrierActivity", "Connected to ${connectedDevice.name} (${connectedDevice.address})")
        } else {
            connectedDeviceTextView.text = "No device connected"
            Log.d("BarrierActivity", "No devices connected")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        radarView.stopSweepAnimation()
        try {
            if (isReceiverRegistered) {
                unregisterReceiver(deviceDiscoveryReceiver)
                isReceiverRegistered = false
            }
        } catch (e: IllegalArgumentException) {
            Log.e("BarrierActivity", "Receiver not registered: ${e.message}")
        }
    }
}
