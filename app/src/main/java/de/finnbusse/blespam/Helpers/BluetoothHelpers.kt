package de.finnbusse.blespam.Helpers

import de.finnbusse.blespam.AppContext.AppContext
import de.finnbusse.blespam.AppContext.AppContext.Companion.bluetoothAdapter

class BluetoothHelpers {
    companion object {
        fun supportsBluetooth5():Boolean{
            var bluetoothAdapter = AppContext.getContext().bluetoothAdapter()
            if(bluetoothAdapter != null){
                if(bluetoothAdapter!!.isLe2MPhySupported
                    && bluetoothAdapter!!.isLeCodedPhySupported
                    && bluetoothAdapter!!.isLeExtendedAdvertisingSupported
                    && bluetoothAdapter!!.isLePeriodicAdvertisingSupported
                ){
                    return true
                }
            }
            return false
        }
    }
}