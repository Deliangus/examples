package deliangus.tech.aves

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.charset.StandardCharsets
import java.util.*

class MainActivity : AppCompatActivity() {
    private var mCharSet = StandardCharsets.UTF_8
    private var tv_value = arrayOfNulls<TextView>(16)
    private var tv_key = arrayOfNulls<TextView>(16)
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mBluetoothLeService: BluetoothLeService? = null
    private var mAdvertiseCallback: AdvertiseCallback? = null
    private var Trajectory_Queue: Queue<IntArray>? = null
    private var sensorAPI: SensorAPI? = null

    private fun updateTextView(i: Int, name: String, value: String) {
        tv_value[i]!!.text = value
        tv_key[i]!!.text = name
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv_value[0] = tv_value_0
        tv_value[1] = tv_value_1
        tv_value[2] = tv_value_2
        tv_value[3] = tv_value_3
        tv_value[4] = tv_value_4
        tv_value[5] = tv_value_5
        tv_value[6] = tv_value_6
        tv_value[7] = tv_value_7
        tv_value[8] = tv_value_8
        tv_value[9] = tv_value_9
        tv_value[10] = tv_value_10
        tv_value[11] = tv_value_11
        tv_value[12] = tv_value_12
        tv_value[13] = tv_value_13
        tv_value[14] = tv_value_14
        tv_value[15] = tv_value_15

        tv_key[0] = tv_key_0
        tv_key[1] = tv_key_1
        tv_key[2] = tv_key_2
        tv_key[3] = tv_key_3
        tv_key[4] = tv_key_4
        tv_key[5] = tv_key_5
        tv_key[6] = tv_key_6
        tv_key[7] = tv_key_7
        tv_key[8] = tv_key_8
        tv_key[9] = tv_key_9
        tv_key[10] = tv_key_10
        tv_key[11] = tv_key_11
        tv_key[12] = tv_key_12
        tv_key[13] = tv_key_13
        tv_key[14] = tv_key_14
        tv_key[15] = tv_key_15
        Trajectory_Queue = LinkedList()
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothLeAdvertiser = mBluetoothAdapter!!.getBluetoothLeAdvertiser()
        mBluetoothLeScanner = mBluetoothAdapter!!.getBluetoothLeScanner()
        mBluetoothLeService = BluetoothLeService()
        checkBlueTooth()
        sensorAPI =
            object : SensorAPI((getSystemService(Context.SENSOR_SERVICE) as SensorManager)) {
                override fun onSensorChanged(event: SensorEvent) {
                    super.onSensorChanged(event)
                    var accelerate_values: FloatArray = this.getAccelerate_values()
                    var velocity_values: FloatArray = this.getVelocity_values()
                    var vector_values: FloatArray = this.getVector_values()
                    var linear_acce_values: FloatArray = this.linear_acce_values()
                    var geo_rotation_values: FloatArray = this.geo_mag_rotation_values()
                    var acceleration: FloatArray = this.acceleration()

                    var theta =
                        Math.toDegrees(Math.acos(geo_rotation_values[3].toDouble())).toFloat() * 2
                    var sin_theta_2 = Math.sin(theta.toDouble() / 2).toFloat()
                    var rot_axis: FloatArray = floatArrayOf(
                        geo_rotation_values[0] / sin_theta_2,
                        geo_rotation_values[1] / sin_theta_2,
                        geo_rotation_values[2] / sin_theta_2
                    )
                    updateTextView(
                        0,
                        "Velocity_x",
                        String.format("%4.2f", velocity_values[0])
                    )
                    updateTextView(
                        1,
                        "Velocity_y",
                        String.format("%4.2f", velocity_values[1])
                    )
                    updateTextView(
                        2,
                        "Velocity_z",
                        String.format("%4.2f", velocity_values[2])
                    )
                    updateTextView(3, "Roll", String.format("%4.2f", rot_axis[0]))
                    updateTextView(4, "Pitch", String.format("%4.2f", rot_axis[1]))
                    updateTextView(5, "Azumith", String.format("%4.2f", rot_axis[2]))
                    updateTextView(6, "Vector_X", String.format("%f", vector_values[0]))
                    updateTextView(7, "Vector_Y", String.format("%f", vector_values[1]))
                    updateTextView(8, "Vector_Z", String.format("%f", vector_values[2]))
                    //float pitch_cos = Math.cos(Math.toRadians(this.pitch));
                    //float pitch_sin = Math.sin(Math.toRadians(this.pitch));
                    //float roll_cos = Math.cos(Math.toRadians(this.roll));
                    //float roll_sin = Math.sin(Math.toRadians(this.roll));
                    //float p = 1 / Math.sqrt(pitch_cos * pitch_cos / (roll_cos * roll_cos) + pitch_sin * pitch_sin);
                    //float r = pitch_cos * p / roll_cos;
                    //float x = roll_sin * r;
                    //float y = pitch_sin * p;
                    //float z = roll_cos * r;
                    var kflt_angles = kflt_angles()
                    updateTextView(9, "K_filter_1", String.format("%4.2f", kflt_angles[0]))
                    updateTextView(10, "K_filter_2", String.format("%4.2f", kflt_angles[1]))
                    updateTextView(11, "K_filter_3", String.format("%4.2f", kflt_angles[2]))
                    val gravity = 9.91
                    val adjust_acce: FloatArray = calculateAdjustedAcce()
                    Trajectory_Queue!!.offer(
                        intArrayOf(
                            (this.getLast_time_Linear_acce() % 1000).toInt(),
                            adjust_acce[0].toInt(),
                            adjust_acce[1].toInt(),
                            adjust_acce[2].toInt()
                        )
                    )
                    val mes =
                        "\t" + (this.getLast_time_Linear_acce() % 1000).toInt() + "," + vector_values[0].toInt() + "," + vector_values[1].toInt() + "," + vector_values[2].toInt()
                    val mesbyte = mes.toByteArray()
                    updateTextView(
                        12,
                        "Linear_Acce_x",
                        String.format("%4.2f", acceleration[0])
                    )
                    updateTextView(
                        13,
                        "Linear_Acce_y",
                        String.format("%4.2f", acceleration[1])
                    )
                    //tv_ad_z.setText(String.format("%4.2f", this.acce_instant_z-adjust_z));
                    updateTextView(
                        14,
                        "Linear_Acce_z",
                        String.format("%4.2f", acceleration[2])
                    )
                    updateTextView(
                        15,
                        "Delta_time",
                        String.format("%f", theta)
                    )
                    //Math.toDegrees(Math.acos(geo_rotation_values[3].toDouble())).toFloat()*2
                    Log.e(
                        "Delta Time",
                        String.format(",%f,%f,%f", acceleration[0], acceleration[1], acceleration[2])
                    )
                }
            }
        //sensorAPI.registerListener((SensorManager) getSystemService(Context.SENSOR_SERVICE));
    }

    private fun sendByAdvertise(outString: String, messageCount: Int) {
        val initial_packet = ByteArray(3)
        initial_packet[0] = messageCount.toByte()
        sendingContinuePacket(initial_packet, outString)
    }

    private fun sendingContinuePacket(initial_packet: ByteArray, CHARACTERS: String) {
        val INITIAL_MESSAGE_PACKET_LENGTH = initial_packet.size
        val BYTES_IN_CONTINUE_PACKET = 31 - 16 - mBluetoothAdapter!!.name.length
        val TAG = "sendingContinuePacket"
        // Check the data length is large how many times with Default Data (BLE)
        val times = CHARACTERS.length / BYTES_IN_CONTINUE_PACKET
        Log.i(TAG, "CHARACTERS.length() " + CHARACTERS.length)
        Log.i(TAG, "times $times")
        val sending_continue_hex = ByteArray(BYTES_IN_CONTINUE_PACKET)
        for (time in 0..times) {
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if (time == times) {
                Log.i(TAG, "LAST PACKET ")
                val character_length = (CHARACTERS.length
                        - BYTES_IN_CONTINUE_PACKET * times)
                initial_packet[1] = time.toByte()
                initial_packet[2] = times.toByte()
                Log.i(TAG, "character_length $character_length")
                val sending_last_hex = ByteArray(character_length)
                for (i in sending_last_hex.indices) {
                    sending_last_hex[i] =
                        CHARACTERS.toByteArray(mCharSet)[sending_continue_hex.size * time + i]
                }
                val last_packet = ByteArray(character_length + INITIAL_MESSAGE_PACKET_LENGTH)
                System.arraycopy(
                    initial_packet, 0, last_packet,
                    0, initial_packet.size
                )
                System.arraycopy(
                    sending_last_hex, 0, last_packet,
                    initial_packet.size, sending_last_hex.size
                )
                if (initial_packet[0] == (-1).toByte()) {
                    sendHeadPacket(last_packet)
                } else {
                    sendOnePacket(last_packet)
                }
            } else {
                Log.i(TAG, "CONTINUE PACKET ")
                val character_length = sending_continue_hex.size
                initial_packet[1] = time.toByte()
                initial_packet[2] = times.toByte()
                for (i in sending_continue_hex.indices) {
                    Log.i(
                        TAG, "Send stt : "
                                + (sending_continue_hex.size * time + i)
                    )
                    sending_continue_hex[i] =
                        CHARACTERS.toByteArray()[sending_continue_hex.size * time + i]
                }
                val sending_continue_packet =
                    ByteArray(character_length + INITIAL_MESSAGE_PACKET_LENGTH)
                System.arraycopy(
                    initial_packet, 0, sending_continue_packet,
                    0, initial_packet.size
                )
                System.arraycopy(
                    sending_continue_hex, 0, sending_continue_packet,
                    initial_packet.size, sending_continue_hex.size
                )
                if (initial_packet[0] == (-1).toByte()) {
                    sendHeadPacket(sending_continue_packet)
                } else {
                    sendOnePacket(sending_continue_packet)
                }
            }
        }
    }

    fun sendOnePacket(data: ByteArray?) {
        val settings = AdvertiseSettings.Builder()
        settings.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        settings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        settings.setConnectable(true)
        val advertiseData = AdvertiseData.Builder()
        advertiseData.setIncludeDeviceName(true)
        advertiseData.setIncludeTxPowerLevel(false)
        advertiseData.addServiceUuid(ParcelUuid(UUID_SERVER))
        advertiseData.addServiceData(ParcelUuid(UUID_SERVER), data)
        mAdvertiseCallback = getmAdvertiseCallback()
        mBluetoothLeAdvertiser!!.startAdvertising(
            settings.build(),
            advertiseData.build(),
            mAdvertiseCallback
        )
        //mAdvertiseButton.setText(R.string.advertise1);
//setTimeout();
    }

    fun sendHeadPacket(data: ByteArray?) {
        val settings = AdvertiseSettings.Builder()
        settings.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        settings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        settings.setConnectable(true)
        val advertiseData = AdvertiseData.Builder()
        advertiseData.setIncludeDeviceName(true)
        advertiseData.setIncludeTxPowerLevel(true)
        advertiseData.addServiceUuid(ParcelUuid(UUID_SERVER))
        advertiseData.addServiceData(ParcelUuid(UUID_SERVER), data)
        val outString = String(data!!, mCharSet)
        mAdvertiseCallback = getmAdvertiseCallback()
        mBluetoothLeAdvertiser!!.startAdvertising(
            settings.build(),
            advertiseData.build(),
            mAdvertiseCallback
        )
        //mAdvertiseButton.setText(R.string.advertise1);
//setTimeout();
    }

    private fun stopAdvertising() {
        Log.d("stopAdvertising", "Service: Stopping Advertising")
        if (mBluetoothLeAdvertiser != null && mAdvertiseCallback != null) {
            mBluetoothLeAdvertiser!!.stopAdvertising(mAdvertiseCallback)
            mAdvertiseCallback = null
        }
        //mAdvertiseButton.setText(R.string.advertise);
    }


    private fun getmAdvertiseCallback(): AdvertiseCallback {
        return object : AdvertiseCallback() {
            var TAG = "initGATT"
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.d(TAG, "BLE advertisement ")
                //mAdvertiseButton.setText(R.string.advertise4);
                Toast.makeText(this@MainActivity, "advertise success", Toast.LENGTH_SHORT).show()
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "Failed to add BLE advertisement, reason: $errorCode")
                //mAdvertiseButton.setText(R.string.advertise2);
                Toast.makeText(
                    this@MainActivity,
                    "advertise failure, reason: $errorCode",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkBlueTooth() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Device does not support bluetooth", Toast.LENGTH_SHORT).show()
        } else {
            if (!mBluetoothAdapter!!.isEnabled) {
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1)
                Toast.makeText(applicationContext, "Bluetooth Turned ON", Toast.LENGTH_SHORT).show()
            }
        }
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ble no supported", Toast.LENGTH_SHORT).show()
            finish()
        }
        if (!mBluetoothAdapter!!.isMultipleAdvertisementSupported) {
            Toast.makeText(this, "Multiple advertisement not supported", Toast.LENGTH_SHORT).show()
        }
        /*
        if (!mBluetoothAdapter.isLe2MPhySupported()) {
            Log.e("check", "2M PHY not supported!");

        }
        if (!mBluetoothAdapter.isLeExtendedAdvertisingSupported()) {
            Log.e("check", "LE Extended Advertising not supported!");

        }

        int maxDataLength = mBluetoothAdapter.getLeMaximumAdvertisingDataLength();
        */
    }

    companion object {
        private val UUID_SERVER = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb")
    }
}