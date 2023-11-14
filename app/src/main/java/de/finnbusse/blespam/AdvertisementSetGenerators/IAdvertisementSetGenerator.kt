package de.finnbusse.blespam.AdvertisementSetGenerators

import de.finnbusse.blespam.Models.AdvertisementSet

interface IAdvertisementSetGenerator {
    fun getAdvertisementSets():List<AdvertisementSet>
}