package de.finnbusse.blespam.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.util.Log
import de.finnbusse.blespam.AppContext.AppContext
import de.finnbusse.blespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.finnbusse.blespam.Enums.AdvertisementError
import de.finnbusse.blespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.finnbusse.blespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.finnbusse.blespam.Interfaces.Services.IAdvertisementService
import de.finnbusse.blespam.Models.AdvertisementSet
import de.finnbusse.blespam.PermissionCheck.PermissionCheck

class LegacyAdvertisementService: IAdvertisementService {

    // private
    private val _logTag = "AdvertisementService"
    private var _bluetoothAdapter:BluetoothAdapter? = null
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

    override fun startAdvertisement(advertisementSet:AdvertisementSet){
        if(_advertiser != null){
            if(advertisementSet.validate()){
                if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
                    val preparedAdvertisementSet = prepareAdvertisementSet(advertisementSet)
                    _advertiser!!.startAdvertising(preparedAdvertisementSet.advertiseSettings.build(), preparedAdvertisementSet.advertiseData.build(), preparedAdvertisementSet.advertisingCallback)
                    Log.d(_logTag, "Started Legacy Advertisement")
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

    override fun stopAdvertisement(){
        if(_advertiser != null){
            if(_currentAdvertisementSet != null){
                if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
                    _advertiser!!.stopAdvertising(_currentAdvertisementSet!!.advertisingCallback)

                    _advertisementServiceCallbacks.map {
                        it.onAdvertisementSetStop(_currentAdvertisementSet)
                    }
                    _currentAdvertisementSet = null
                } else {
                    Log.d(_logTag, "Missing permission to stop advertisement")
                }
            } else {
                Log.d(_logTag, "Current Legacy Advertising Set is null")
            }
        } else {
            Log.d(_logTag, "Advertiser is null")
        }
    }

    override fun setTxPowerLevel(txPowerLevel:Int){
        if(txPowerLevel >= AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW && txPowerLevel <= AdvertiseSettings.ADVERTISE_TX_POWER_HIGH){
            _txPowerLevel = txPowerLevel
        } else {
            Log.d(_logTag, "Invalid txPowerLevel specified: $txPowerLevel")
        }
    }

    fun prepareAdvertisementSet(advertisementSet: AdvertisementSet):AdvertisementSet{
        if(_txPowerLevel != null){
            advertisementSet.advertiseSettings.txPowerLevel = _txPowerLevel!!
            advertisementSet.advertisingSetParameters.txPowerLevel = _txPowerLevel!!
        }
        advertisementSet.advertisingCallback = getAdvertisingCallback()
        return advertisementSet
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
        return true
    }

    private fun getAdvertisingCallback():AdvertiseCallback{
        return object : AdvertiseCallback() {

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)

                val advertisementError = when (errorCode) {
                    AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> AdvertisementError.ADVERTISE_FAILED_ALREADY_STARTED
                    AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> AdvertisementError.ADVERTISE_FAILED_FEATURE_UNSUPPORTED
                    AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> AdvertisementError.ADVERTISE_FAILED_INTERNAL_ERROR
                    AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> AdvertisementError.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
                    AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> AdvertisementError.ADVERTISE_FAILED_DATA_TOO_LARGE
                    else -> {AdvertisementError.ADVERTISE_FAILED_UNKNOWN}
                }

                _advertisementServiceCallbacks.map {
                    it.onAdvertisementSetFailed(_currentAdvertisementSet, advertisementError)
                }
            }

            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                _advertisementServiceCallbacks.map {
                    it.onAdvertisementSetSucceeded(_currentAdvertisementSet)
                }
            }
        }
    }
}