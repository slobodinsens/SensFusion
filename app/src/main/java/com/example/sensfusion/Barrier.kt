@file:Suppress("DEPRECATION")

package com.example.sensfusion

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.UUID

class BarrierActivity : AppCompatActivity() {

    private val targetDeviceName = "BarrierDevice" // Name of the target device
    private var deviceAddress: String? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val barrierUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID for SPP
    private val requestCodePermissions = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.barrier)

        // Check Bluetooth permissions
        checkAndRequestPermissions()

        // Buttons
        val openBarrierButton: CardView = findViewById(R.id.openBarrierButton)
        val closeBarrierButton: CardView = findViewById(R.id.closeBarrierButton)
        val bluetoothSettingsButton: CardView = findViewById(R.id.bluetoothSettingsButton)

        // Bluetooth Settings Button
        bluetoothSettingsButton.setOnClickListener {
            deviceAddress = getPairedDeviceByName(targetDeviceName)
            if (deviceAddress == null) {
                Log.e("BarrierActivity", "Device with name '$targetDeviceName' not found.")
            }
        }

        // Open Barrier Button
        openBarrierButton.setOnClickListener {
            if (connectToBarrier()) {
                sendCommand("OPEN")
            }
        }

        // Close Barrier Button
        closeBarrierButton.setOnClickListener {
            if (connectToBarrier()) {
                sendCommand("CLOSE")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePermissions) {
            val deniedPermissions = permissions.zip(grantResults.toTypedArray()).filter { it.second != PackageManager.PERMISSION_GRANTED }
            if (deniedPermissions.isNotEmpty()) {
                Log.e("BarrierActivity", "Permissions denied: ${deniedPermissions.map { it.first }}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getPairedDeviceByName(deviceName: String): String? {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        for (device in pairedDevices.orEmpty()) {
            if (device.name == deviceName) {
                Log.i("BarrierActivity", "Paired device found: ${device.name} (${device.address})")
                return device.address
            }
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private fun connectToBarrier(): Boolean {
        if (deviceAddress == null) {
            Log.e("BarrierActivity", "Device address is null. Make sure to select a device first.")
            return false
        }

        val device = bluetoothAdapter.getRemoteDevice(deviceAddress!!)
        try {
            if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(barrierUUID)
                bluetoothSocket?.connect()
                Log.i("BarrierActivity", "Connected to barrier device: ${device.name}")
            }
            return true
        } catch (e: IOException) {
            Log.e("BarrierActivity", "Failed to connect to the barrier device.", e)
            disconnect()
            return false
        }
    }

    private fun sendCommand(command: String) {
        try {
            if (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
                val outputStream = bluetoothSocket!!.outputStream
                outputStream.write(command.toByteArray())
                outputStream.flush()
                Log.i("BarrierActivity", "Command sent: $command")
            } else {
                Log.e("BarrierActivity", "Bluetooth socket is not connected.")
            }
        } catch (e: IOException) {
            Log.e("BarrierActivity", "Failed to send command.", e)
        }
    }

    private fun disconnect() {
        try {
            bluetoothSocket?.close()
            Log.i("BarrierActivity", "Disconnected from barrier device.")
        } catch (e: IOException) {
            Log.e("BarrierActivity", "Failed to disconnect from barrier device.", e)
        }
    }
}
