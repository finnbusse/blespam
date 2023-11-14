package de.finnbusse.blespam.ui.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import de.finnbusse.blespam.R

class PreferencesFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}