package de.finnbusse.blespam.Interfaces.Services

import de.finnbusse.blespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.finnbusse.blespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.finnbusse.blespam.Models.AdvertisementSet

interface IAdvertisementService {
    fun startAdvertisement(advertisementSet: AdvertisementSet)
    fun stopAdvertisement()
    fun setTxPowerLevel(txPowerLevel:Int)

    fun addAdvertisementServiceCallback(callback: IAdvertisementServiceCallback)
    fun removeAdvertisementServiceCallback(callback: IAdvertisementServiceCallback)

    fun isLegacyService():Boolean

}