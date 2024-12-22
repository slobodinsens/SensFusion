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
    private val discoveredDevices: MutableList<BluetoothDevice> = mutableListOf<BluetoothDevice>()

    private lateinit var radarView: RadarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.barrier)

        val openBarrierButton: CardView = findViewById(R.id.openBarrierButton)
        val closeBarrierButton: CardView = findViewById(R.id.closeBarrierButton)
        val bluetoothSettingsButton: CardView = findViewById(R.id.bluetoothSettingsButton)
        radarView = findViewById(R.id.radarView)

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

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(deviceDiscoveryReceiver, filter)

        AlertDialog.Builder(this)
            .setTitle("Scanning for devices...")
            .setNegativeButton("Stop") { dialog, _ ->
                bluetoothAdapter.cancelDiscovery()
                unregisterReceiver(deviceDiscoveryReceiver)
                radarView.stopSweepAnimation()
                dialog.dismiss()
            }
            .show()
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
                        discoveredDevices.map { it.name ?: "Unknown" to rssi }
                    )
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        radarView.stopSweepAnimation()
        try {
            unregisterReceiver(deviceDiscoveryReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e("BarrierActivity", "Receiver not registered: ${e.message}")
        }
    }
}
