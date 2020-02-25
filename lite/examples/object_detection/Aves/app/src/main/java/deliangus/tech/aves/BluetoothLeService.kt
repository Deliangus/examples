package deliangus.tech.aves

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import deliangus.tech.aves.BluetoothLeService
import java.sql.DriverManager
import java.util.*

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ /**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
class BluetoothLeService : Service() {
    private val mBinder: IBinder = LocalBinder()
    var intentAction: String? = null
    var context: Context? = null
    var characteristicRead: BluetoothGattCharacteristic? = null
    var mrssi = 999
    var rssiStatus = false
    //public Intent mgattServiceIntent;
    var mOutString = " "
    var mInString = " "
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mBluetoothGattServer: BluetoothGattServer? = null
    private var mConnectionState = STATE_DISCONNECTED
    // Implements callback methods for GATT events that the app cares about.  For example,
// connection change and services discovered.
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                mConnectionState = STATE_CONNECTED
                rssiStatus = mBluetoothGatt!!.readRemoteRssi()
                Log.i(TAG, "Connected to GATT server.")
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt!!.discoverServices())
                broadcastUpdate(intentAction)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                mConnectionState = STATE_DISCONNECTED
                Log.i(TAG, "Disconnected from GATT server.")
                broadcastUpdate(intentAction)
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt,
                                      rssi: Int,
                                      status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mrssi = rssi
                Log.w(TAG, "onReadRemoteRssi: " + "rssi Updated")
                broadcastUpdate(ACTION_RSSI_AVAILABLE)
            } else {
                Log.w(TAG, "onReadRemoteRssi: $status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt,
                                          characteristic: BluetoothGattCharacteristic,
                                          status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                             characteristic: BluetoothGattCharacteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }
    private var currentDevice: BluetoothDevice? = null
    private val bluetoothGattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        /**
         * 1.连接状态发生变化时
         * @param device
         * @param status
         * @param newState
         */
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.e(TAG, String.format("1.onConnectionStateChange：device name = %s, address = %s", device.name, device.address))
            Log.e(TAG, String.format("1.onConnectionStateChange：status = %s, newState =%s ", status, newState))
            super.onConnectionStateChange(device, status, newState)
            currentDevice = device
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            super.onServiceAdded(status, service)
            Log.e(TAG, String.format("onServiceAdded：status = %s", status))
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) {
            Log.e(TAG, String.format("onCharacteristicReadRequest：device name = %s, address = %s", device.name, device.address))
            Log.e(TAG, String.format("onCharacteristicReadRequest：requestId = %s, offset = %s", requestId, offset))
            mBluetoothGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.value)
            //            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        /**
         * 3. onCharacteristicWriteRequest,接收具体的字节
         * @param device
         * @param requestId
         * @param characteristic
         * @param preparedWrite
         * @param responseNeeded
         * @param offset
         * @param requestBytes
         */
        override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, requestBytes: ByteArray) {
            Log.e(TAG, String.format("3.onCharacteristicWriteRequest：device name = %s, address = %s", device.name, device.address))
            mBluetoothGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, requestBytes)
            //4.处理响应内容
            intentAction = ACTION_RESPONSE_TO_CLIENT
            onResponseToClient(requestBytes, device, requestId, characteristic)
        }

        /**
         * 2.描述被写入时，在这里执行 bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS...  收，触发 onCharacteristicWriteRequest
         * @param device
         * @param requestId
         * @param descriptor
         * @param preparedWrite
         * @param responseNeeded
         * @param offset
         * @param value
         */
        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) {
            Log.e(TAG, String.format("2.onDescriptorWriteRequest：device name = %s, address = %s", device.name, device.address))
            //            Log.e(TAG, String.format("2.onDescriptorWriteRequest：requestId = %s, preparedWrite = %s, responseNeeded = %s, offset = %s, value = %s,", requestId, preparedWrite, responseNeeded, offset, OutputStringUtil.toHexString(value)));
// now tell the connected device that this was all successfull
            mBluetoothGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
        }

        /**
         * 5.特征被读取。当回复响应成功后，客户端会读取然后触发本方法
         * @param device
         * @param requestId
         * @param offset
         * @param descriptor
         */
        override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor) {
            Log.e(TAG, String.format("onDescriptorReadRequest：device name = %s, address = %s", device.name, device.address))
            Log.e(TAG, String.format("onDescriptorReadRequest：requestId = %s", requestId))
            //            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            mBluetoothGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            super.onNotificationSent(device, status)
            Log.e(TAG, String.format("5.onNotificationSent：device name = %s, address = %s", device.name, device.address))
            Log.e(TAG, String.format("5.onNotificationSent：status = %s", status))
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            super.onMtuChanged(device, mtu)
            Log.e(TAG, String.format("onMtuChanged：mtu = %s", mtu))
        }

        override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
            super.onExecuteWrite(device, requestId, execute)
            Log.e(TAG, String.format("onExecuteWrite：requestId = %s", requestId))
        }
    }

    private fun broadcastUpdate(action: String?) {
        val intent = Intent(action)
        context!!.sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String,
                                characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)
        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
// carried out as per profile specifications:
// http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT == characteristic.uuid) {
            val flag = characteristic.properties
            var format = -1
            if (flag and 0x01 != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16
                Log.d(TAG, "Heart rate format UINT16.")
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8
                Log.d(TAG, "Heart rate format UINT8.")
            }
            val heartRate = characteristic.getIntValue(format, 1)
            Log.d(TAG, String.format("Received heart rate: %d", heartRate))
            intent.putExtra(EXTRA_DATA, heartRate.toString())
        } else { // For all other profiles, writes the data formatted in HEX.
            val data = characteristic.value
            if (data != null && data.size > 0) {
                val stringBuilder = StringBuilder(data.size)
                for (byteChar in data) stringBuilder.append(String.format("%02X ", byteChar))
                intent.putExtra(EXTRA_DATA, String(data) + "\n" + stringBuilder.toString())
            }
        }
        context!!.sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String?,
                                msg: String) {
        val intent = Intent(action)
        intent.putExtra(EXTRA_DATA, msg)
        context!!.sendBroadcast(intent)
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean { // After using a given device, you should make sure that BluetoothGatt.close() is called
// such that resources are cleaned up properly.  In this particular example, close() is
// invoked when the UI is disconnected from the Service.
        close()
        return super.onUnbind(intent)
    }

    fun initialize(theBluetoothAdapter: BluetoothAdapter?, theContext: Context?, theBluetoothManager: BluetoothManager?): Boolean { // For API level 18 and above, get a reference to BluetoothAdapter through
// BluetoothManager.
        mBluetoothManager = theBluetoothManager
        mBluetoothAdapter = theBluetoothAdapter
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        //mgattServiceIntent = intent;
        context = theContext
        return true
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            return if (mBluetoothGatt!!.connect()) {
                mConnectionState = STATE_CONNECTING
                true
            } else {
                false
            }
        }
        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
// parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        mBluetoothDeviceAddress = address
        mConnectionState = STATE_CONNECTING
        return true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.disconnect()
    }

    val rssi: Int
        get() {
            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized")
                return -1
            }
            rssiStatus = mBluetoothGatt!!.readRemoteRssi()
            return mrssi
        }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    fun close() {
        if (mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.readCharacteristic(characteristic)
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic,
                                      enabled: Boolean) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)
        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT == characteristic.uuid) {
            val descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG))
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            mBluetoothGatt!!.writeDescriptor(descriptor)
        }
    }

    private fun initServices(context: Context) {
        mBluetoothGattServer = mBluetoothManager?.openGattServer(context, bluetoothGattServerCallback)
        val service = BluetoothGattService(UUID_SERVER, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        //add a read characteristic.
        characteristicRead = BluetoothGattCharacteristic(UUID_CHARREAD, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
        //add a descriptor
        val descriptor = BluetoothGattDescriptor(UUID_DESCRIPTOR, BluetoothGattCharacteristic.PERMISSION_WRITE)
        characteristicRead!!.addDescriptor(descriptor)
        service.addCharacteristic(characteristicRead)
        //add a write characteristic.
        val characteristicWrite = BluetoothGattCharacteristic(UUID_CHARWRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE)
        service.addCharacteristic(characteristicWrite)
        mBluetoothGattServer?.addService(service)
        Log.e(TAG, "2. initServices ok")
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    private fun onResponseToClient(reqeustBytes: ByteArray, device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic) {
        Log.e(TAG, String.format("4.onResponseToClient：device name = %s, address = %s", device.name, device.address))
        Log.e(TAG, String.format("4.onResponseToClient：requestId = %s", requestId))
        //        String msg = OutputStringUtil.transferForPrint(reqeustBytes);
        val msg = String(reqeustBytes)
        DriverManager.println("4.收到:$msg")
        broadcastUpdate(intentAction, msg)
        currentDevice = device
    }

    private fun sendToClient(message: String) {
        characteristicRead!!.value = message.toByteArray()
        if (currentDevice != null) mBluetoothGattServer!!.notifyCharacteristicChanged(currentDevice, characteristicRead, false)
        DriverManager.println("Me:$message")
        mOutString = message
    }

    val supportedGattServices: List<BluetoothGattService>?
        get() = if (mBluetoothGatt == null) null else mBluetoothGatt!!.services

    inner class LocalBinder : Binder() {
        val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    companion object {
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
        const val ACTION_RSSI_AVAILABLE = "com.example.bluetooth.le.ACTION_RSSI_AVAILABLE"
        const val ACTION_RESPONSE_TO_CLIENT = "com.example.bluetooth.le.ACTION_RESPONSE_TO_CLIENT"
        val UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT)
        private val TAG = BluetoothLeService::class.java.simpleName
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2
        private val UUID_SERVER = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARREAD = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARWRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
        private val UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}