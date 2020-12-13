package com.ikanoshiokara.devicesearcher

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private val REQUEST_ENABLEBLUETOOTH: Int = 1 // Bluetooth機能の有効化要求時の識別コード
    private val MY_REQUEST_CODE: Int = 2 // 位置情報要求時の識別コード
    private lateinit var mBluetoothAdapter: BluetoothAdapter // アダプター

    private lateinit var device_num_list: LinearLayout // レイアウト

    private var mDeviceList: ArrayList<BluetoothDevice> = arrayListOf()
    private var mScanning: Boolean = false             // スキャン中かどうかのフラグ

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if(device == null){
                        Log.d("nullDevice", "Device is null")
                        return
                    }
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address
                    val deviceUUID = device?.uuids
                    val deviceRssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                    Log.d("device", "Device name: ${deviceName}, address:${deviceHardwareAddress}, UUID:${deviceUUID}, RSSI:${deviceRssi}")
                    //mDeviceList.add(device!!)
                    return
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Toast.makeText(
                            context,
                            "Bluetooth検出開始",
                            Toast.LENGTH_SHORT
                    ).show()
                    mScanning = true
                    return
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> { //cancelDiscoveryでも呼ばれる
                    if(mScanning) {
                        //mBluetoothAdapter.cancelDiscovery()
                        mScanning = false
                    }
                    /*val textView = TextView(context)
                    textView.text = "TimeStamp: ${SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Date())}\n" +
                            "DeviceOfNumber: ${mDeviceList.size}\n"

                    device_num_list.addView(textView)*/

                    Toast.makeText(
                        context,
                        "Bluetooth検出終了",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("discoveryFin", "Discovery finished")
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        device_num_list = findViewById(R.id.device_num_list)

        var bluetoothManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.getAdapter()
        if( null == mBluetoothAdapter )
        {    // Android端末がBluetoothをサポートしていない
            Toast.makeText(
                this,
                R.string.bluetooth_is_not_supported,
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        //
        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var isGpsEnabled: Boolean = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isGpsEnabled) {
            //startActivityForResult(Intent(LocationManager.PROVIDERS_CHANGED_ACTION), MY_REQUEST_CODE)
            startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), MY_REQUEST_CODE)
        }

        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))

        var updateButton: Button = findViewById(R.id.update_button)
        updateButton.setOnClickListener(View.OnClickListener {
            Log.d("clickUpdateButton", "Update Button Clicked!!")
            if(mBluetoothAdapter.cancelDiscovery()){
                Log.d("cancelSuccess", "cancelDiscovery() success")
            }else{
                Log.d("cancelFailed", "cancelDiscovery() failed")
            }

            if(!mScanning){
                if(mBluetoothAdapter.startDiscovery()){
                    Log.d("startSuccess", "startDiscovery() success")
                }else{
                    Log.d("startFailed", "startDiscovery() failed")
                }
            }
            Toast.makeText(
                    this,
                    "Update Button Clicked!!",
                    Toast.LENGTH_SHORT
            ).show()
        })

        var cancelButton: Button = findViewById(R.id.cancel_button)
        cancelButton.setOnClickListener(View.OnClickListener {
            Log.d("clickCancelButton", "Cancel Button Clicked!!")
            if(mBluetoothAdapter.cancelDiscovery()){
                Log.d("cancelSuccess", "cancelDiscovery() success")
            }else{
                Log.d("cancelFailed", "cancelDiscovery() failed")
            }

            Toast.makeText(
                this,
                "Cancel Button Clicked!!",
                Toast.LENGTH_SHORT
            ).show()
        })
    }

    private fun requestBluetoothFeature(){
        if(mBluetoothAdapter.isEnabled){
            return
        }

        // Bluetoothの有効化要求
        var enableBluetoothIntent: Intent = Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE )
        startActivityForResult(
            enableBluetoothIntent,
            REQUEST_ENABLEBLUETOOTH
        )
    }

    override fun onResume() {
        super.onResume()
        requestBluetoothFeature()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode){
            REQUEST_ENABLEBLUETOOTH -> {
                if (Activity.RESULT_CANCELED == resultCode) {
                    Toast.makeText(
                        this,
                        R.string.bluetooth_is_not_working,
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return
                }
            }
            MY_REQUEST_CODE -> {
                if (Activity.RESULT_CANCELED == resultCode) {
                    Toast.makeText(
                            this,
                            R.string.gps_is_not_working,
                            Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("discovery", "${mBluetoothAdapter.isDiscovering}")
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
        mBluetoothAdapter.cancelDiscovery()
        mScanning = false
    }
}