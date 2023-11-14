package de.finnbusse.blespam.ui.swiftPair

import android.R.attr.text
import android.R.id.checkbox
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import de.finnbusse.blespam.AdvertisementSetGenerators.ContinuityDevicePopUpAdvertisementSetGenerator
import de.finnbusse.blespam.AdvertisementSetGenerators.SwiftPairAdvertisementSetGenerator
import de.finnbusse.blespam.AppContext.AppContext
import de.finnbusse.blespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.finnbusse.blespam.Constants.LogLevel
import de.finnbusse.blespam.Enums.AdvertisementError
import de.finnbusse.blespam.Handlers.AdvertisementSetQueueHandler
import de.finnbusse.blespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.finnbusse.blespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.finnbusse.blespam.Models.AdvertisementSet
import de.finnbusse.blespam.Models.LogEntryModel
import de.finnbusse.blespam.R
import de.finnbusse.blespam.Services.AdvertisementLoopService
import de.finnbusse.blespam.Services.AdvertisementSetQueHandler
import de.finnbusse.blespam.Services.BluetoothLeAdvertisementService
import de.finnbusse.blespam.databinding.FragmentSwiftpairBinding


class SwiftPairFragment: Fragment(), IAdvertisementServiceCallback {
    private var _binding: FragmentSwiftpairBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var _viewModel: SwiftPairViewModel? = null
    private var _advertisementSetQueueHandler: AdvertisementSetQueueHandler = AppContext.getAdvertisementSetQueueHandler()
    private val _advertisementSets = SwiftPairAdvertisementSetGenerator().getAdvertisementSets()
    private val _logTag = "SwiftPairFragment"

    private lateinit var _toggleButton:Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(SwiftPairViewModel::class.java)
        _viewModel = viewModel
        _binding = FragmentSwiftpairBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _advertisementSetQueueHandler.addAdvertisementServiceCallback(this)
        _advertisementSetQueueHandler.clearAdvertisementSetCollection()
        _advertisementSetQueueHandler.addAdvertisementSetCollection(_advertisementSets)

        setupUi()

        return root
    }

    override fun onResume() {
        super.onResume()
        _advertisementSetQueueHandler.addAdvertisementServiceCallback(this)
        _advertisementSetQueueHandler.clearAdvertisementSetCollection()
        _advertisementSetQueueHandler.addAdvertisementSetCollection(_advertisementSets)
    }

    override fun onPause() {
        super.onPause()
        _advertisementSetQueueHandler.removeAdvertisementServiceCallback(this)
        if( _advertisementSetQueueHandler != null && _advertisementSetQueueHandler!!.isActive()){
            stopAdvertising()
            _advertisementSetQueueHandler.clearAdvertisementSetCollection()
        }
    }

    fun startAdvertising(){
        if( _advertisementSetQueueHandler != null){
            _advertisementSetQueueHandler!!.activate()

            val logEntry = LogEntryModel()
            logEntry.level = LogLevel.Info
            logEntry.message = "Started Advertising"
            _viewModel!!.addLogEntry(logEntry)

            _viewModel!!.isTransmitting.postValue(true)

            _toggleButton.text = "Stop Advertising"
        } else {
            val logEntry = LogEntryModel()
            logEntry.level = LogLevel.Info
            logEntry.message = "Could not start Advertising"
            _viewModel!!.addLogEntry(logEntry)
        }
    }

    fun stopAdvertising(){
        if( _advertisementSetQueueHandler != null){
            _advertisementSetQueueHandler!!.deactivate()

            val logEntry = LogEntryModel()
            logEntry.level = LogLevel.Info
            logEntry.message = "Stopped Advertising"
            _viewModel!!.addLogEntry(logEntry)

            _viewModel!!.isTransmitting.postValue(false)

            _toggleButton.text = "Start Advertising"
        } else {
            val logEntry = LogEntryModel()
            logEntry.level = LogLevel.Info
            logEntry.message = "Could not stop Advertising"
            _viewModel!!.addLogEntry(logEntry)
        }
    }

    fun setupUi(){
        if(_viewModel != null){

            // toggle button
            val toggleBtn: Button = binding.advertiseButton
            _toggleButton = toggleBtn
            //animation view
            val animationView: LottieAnimationView = binding.swiftPairAnimation

            val toggleOnClickListener = View.OnClickListener { view ->
                if( _advertisementSetQueueHandler != null){
                    if (! _advertisementSetQueueHandler!!.isActive()) {
                        startAdvertising()
                    } else {
                        stopAdvertising()
                    }
                }
            }

            toggleBtn.setOnClickListener(toggleOnClickListener)
            animationView.setOnClickListener(toggleOnClickListener)

            _viewModel!!.isTransmitting.observe(viewLifecycleOwner) {
                if(it == true){
                    animationView.repeatCount = LottieDrawable.INFINITE
                    animationView.playAnimation()
                } else {
                    animationView.cancelAnimation()
                }
            }

            // txPower
            val fastPairingTxPowerSeekbar = binding.swiftPairTxPowerSeekbar
            val fastPairingTxPowerSeekbarLabel: TextView = binding.swiftPairTxPowerSeekbarLabel
            fastPairingTxPowerSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

                    var newTxPowerLevel = progress
                    var newTxPowerLabel = "High"

                    when (progress) {
                        0 -> {
                            newTxPowerLabel = "Ultra Low"
                        }
                        1 -> {
                            newTxPowerLabel = "Low"
                        }
                        2 -> {
                            newTxPowerLabel = "Medium"
                        }
                        3 -> {
                            newTxPowerLabel = "High"
                        } else -> {
                        newTxPowerLevel = 3
                        newTxPowerLabel = "High"
                    }
                    }

                    fastPairingTxPowerSeekbarLabel.text = "TX Power: ${newTxPowerLabel}"
                    if(_advertisementSetQueueHandler != null){
                        _advertisementSetQueueHandler!!.setTxPowerLevel(newTxPowerLevel)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // you can probably leave this empty
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // you can probably leave this empty
                }
            })

            // seekbar
            val fastPairingRepeatitionSeekbar: SeekBar = binding.swiftPairRepeatitionSeekbar
            val fastPairingRepeatitionLabel: TextView = binding.swiftPairRepeatitionSeekbarLabel
            fastPairingRepeatitionSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    fastPairingRepeatitionLabel.text = "Advertise every ${progress} Seconds"
                    if( _advertisementSetQueueHandler != null){
                        _advertisementSetQueueHandler!!.setIntervalSeconds(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // currently not in use
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // currently not in use
                }
            })

            // status label
            val statusLabelFastPairing: TextView = binding.statusLabelswiftPair
            _viewModel!!.statusText.observe(viewLifecycleOwner) {
                statusLabelFastPairing.text = it
            }

            // log scroll view
            val logView: LinearLayout = binding.swiftPairLogLinearView
            _viewModel!!.logEntries.observe(viewLifecycleOwner) {
                logView.removeAllViews()
                it.reversed().map { logEntryModel ->
                    val logEntryTextView: TextView = TextView(logView.context)
                    logEntryTextView.text = logEntryModel.message

                    when (logEntryModel.level){
                        LogLevel.Info -> {
                            logEntryTextView.setTextColor(ContextCompat.getColor(logView.context, R.color.log_info))
                        }
                        LogLevel.Warning -> {
                            logEntryTextView.setTextColor(ContextCompat.getColor(logView.context, R.color.log_warning))
                        }
                        LogLevel.Error -> {
                            logEntryTextView.setTextColor(ContextCompat.getColor(logView.context, R.color.log_error))
                        }
                        LogLevel.Success -> {
                            logEntryTextView.setTextColor(ContextCompat.getColor(logView.context, R.color.log_success))
                        }
                    }

                    logView.addView(logEntryTextView)
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onAdvertisementSetStart(advertisementSet: AdvertisementSet?) {
        if(advertisementSet != null){
            var message = "Advertising: ${advertisementSet.deviceName}"
            _viewModel!!.setStatusText(message)

            var logEntry = LogEntryModel()
            logEntry.level = LogLevel.Info
            logEntry.message = message
            _viewModel!!.addLogEntry(logEntry)
        }
    }

    override fun onAdvertisementSetStop(advertisementSet: AdvertisementSet?) {
        Log.i(_logTag, "onAdvertisementSetStop called")
    }

    override fun onAdvertisementSetSucceeded(advertisementSet: AdvertisementSet?) {
        var logEntry = LogEntryModel()
        logEntry.level = LogLevel.Success
        logEntry.message = "Started advertising successfully"
        _viewModel!!.addLogEntry(logEntry)
    }

    override fun onAdvertisementSetFailed(advertisementSet: AdvertisementSet?, advertisementError: AdvertisementError) {
        var message = if (advertisementError == AdvertisementError.ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
            "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
        } else if (advertisementError == AdvertisementError.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
            "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
        } else if (advertisementError == AdvertisementError.ADVERTISE_FAILED_ALREADY_STARTED) {
            "ADVERTISE_FAILED_ALREADY_STARTED"
        } else if (advertisementError == AdvertisementError.ADVERTISE_FAILED_DATA_TOO_LARGE) {
            "ADVERTISE_FAILED_DATA_TOO_LARGE"
        } else if (advertisementError == AdvertisementError.ADVERTISE_FAILED_INTERNAL_ERROR) {
            "ADVERTISE_FAILED_INTERNAL_ERROR"
        } else {
            "Unknown Error"
        }

        var logEntry = LogEntryModel()
        logEntry.level = LogLevel.Error
        logEntry.message = message
        _viewModel!!.addLogEntry(logEntry)
    }
}