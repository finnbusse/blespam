package de.finnbusse.blespam.Interfaces.Callbacks

import de.finnbusse.blespam.Enums.AdvertisementError
import de.finnbusse.blespam.Models.AdvertisementSet

interface IAdvertisementServiceCallback {
    fun onAdvertisementSetStart(advertisementSet: AdvertisementSet?)
    fun onAdvertisementSetStop(advertisementSet: AdvertisementSet?)
    fun onAdvertisementSetSucceeded(advertisementSet: AdvertisementSet?)
    fun onAdvertisementSetFailed(advertisementSet: AdvertisementSet?, advertisementError: AdvertisementError)
}