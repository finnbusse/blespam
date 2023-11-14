package de.finnbusse.blespam.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import de.finnbusse.blespam.AppContext.AppContext
import de.finnbusse.blespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.finnbusse.blespam.Enums.AdvertisementError
import de.finnbusse.blespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.finnbusse.blespam.Interfaces.Services.IAdvertisementService
import de.finnbusse.blespam.Models.AdvertisementSet
import de.finnbusse.blespam.PermissionCheck.PermissionCheck

class ModernAdvertisementService: IAdvertisementService{

    // private
    private val _logTag = "AdvertisementService"
    private var _bluetoothAdapter: BluetoothAdapter? = null
    private var _advertiser: BluetoothLeAdvertiser? = null
    private var _advertisementServiceCallbacks:MutableList<IAdvertisementServiceCallback> = mutableListOf()
    private var _currentAdvertisementSet: AdvertisementSet? = null
    private var _txPowerLevel:Int? = null

    init {
        _bluetoothAdapter = AppContext.getContext().bluetoothAdapter()
        if(_bluetoothAdapter != null){
            _advertiser = _bluetoothAdapter!!.bluetoothLeAdvertiser
        }
    }

    fun prepareAdvertisementSet(advertisementSet: AdvertisementSet):AdvertisementSet{
        if(_txPowerLevel != null){
            advertisementSet.advertiseSettings.txPowerLevel = _txPowerLevel!!
            advertisementSet.advertisingSetParameters.txPowerLevel = _txPowerLevel!!
        }
        advertisementSet.advertisingSetCallback = getAdvertisingSetCallback()
        return advertisementSet
    }



    // Callback Implementation
    override fun startAdvertisement(advertisementSet: AdvertisementSet) {
        if(_advertiser != null){
            if(advertisementSet.validate()){
                if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
                    val preparedAdvertisementSet = prepareAdvertisementSet(advertisementSet)
                    _advertiser!!.startAdvertisingSet(preparedAdvertisementSet.advertisingSetParameters.build(), preparedAdvertisementSet.advertiseData.build(), null, null, null, preparedAdvertisementSet.advertisingSetCallback)
                    Log.d(_logTag, "Started Modern Advertisement")
                    _currentAdvertisementSet = preparedAdvertisementSet
                    _advertisementServiceCallbacks.map {
                        it.onAdvertisementSetStart(advertisementSet)
                    }
                } else {
                    Log.d(_logTag, "Missing permission to execute advertisement")
                }
            } else {
                Log.d(_logTag, "Advertisement Set could not be validated")
            }
        } else {
            Log.d(_logTag, "Advertiser is null")
        }
    }

    override fun stopAdvertisement() {
        if(_advertiser != null){
            if(_currentAdvertisementSet != null){
                if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
                    _advertiser!!.stopAdvertisingSet(_currentAdvertisementSet!!.advertisingSetCallback)
                    _currentAdvertisementSet = null
                } else {
                    Log.d(_logTag, "Missing permission to stop advertisement")
                }
            } else {
                Log.d(_logTag, "Current Modern Advertising Set is null")
            }
        } else {
            Log.d(_logTag, "Advertiser is null")
        }
    }

    override fun setTxPowerLevel(txPowerLevel: Int) {
        if(txPowerLevel >= AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW && txPowerLevel <= AdvertiseSettings.ADVERTISE_TX_POWER_HIGH){
            _txPowerLevel = txPowerLevel
        } else {
            Log.d(_logTag, "Invalid txPowerLevel specified: $txPowerLevel")
        }
    }

    override fun addAdvertisementServiceCallback(callback: IAdvertisementServiceCallback){
        if(!_advertisementServiceCallbacks.contains(callback)){
            _advertisementServiceCallbacks.add(callback)
        }
    }
    override fun removeAdvertisementServiceCallback(callback: IAdvertisementServiceCallback){
        if(_advertisementServiceCallbacks.contains(callback)){
            _advertisementServiceCallbacks.remove(callback)
        }
    }

    override fun isLegacyService(): Boolean {
        return false
    }

    private fun getAdvertisingSetCallback(): AdvertisingSetCallback {
        return object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
                if(status == AdvertisingSetCallback.ADVERTISE_SUCCESS){
                    // SUCCESS
                    _advertisementServiceCallbacks.map{
                        it.onAdvertisementSetSucceeded(_currentAdvertisementSet)
                    }
                } else{
                    // FAIL
                    val advertisementError = when (status) {
                        AdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED -> AdvertisementError.ADVERTISE_FAILED_ALREADY_STARTED
                        AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> AdvertisementError.ADVERTISE_FAILED_FEATURE_UNSUPPORTED
                        AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> AdvertisementError.ADVERTISE_FAILED_INTERNAL_ERROR
                        AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> AdvertisementError.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
                        AdvertisingSetCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> AdvertisementError.ADVERTISE_FAILED_DATA_TOO_LARGE
                        else -> {AdvertisementError.ADVERTISE_FAILED_UNKNOWN}
                    }

                    _advertisementServiceCallbacks.map{
                        it.onAdvertisementSetFailed(_currentAdvertisementSet, advertisementError)
                    }
                }
            }

            override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {

            }

            override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int) {

            }

            override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
                _advertisementServiceCallbacks.map{
                    it.onAdvertisementSetStop(_currentAdvertisementSet)
                }
            }
        }
    }

}