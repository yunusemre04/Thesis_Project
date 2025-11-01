package com.example.positiondeterminer.ui.utils

import android.content.Context
import com.example.positiondeterminer.R

object ActivityTranslator {
    /**
     * Translates activity names from English to the current device language
     * @param activity The activity name in English (e.g., "Walking", "WALKING")
     * @param context Android context for accessing string resources
     * @return Translated activity name based on device language
     */
    fun translate(activity: String, context: Context): String {
        return when (activity.uppercase().replace("_", " ")) {
            "WALKING" -> context.getString(R.string.walking)
            "WALKING UPSTAIRS" -> context.getString(R.string.walking_upstairs)
            "WALKING DOWNSTAIRS" -> context.getString(R.string.walking_downstairs)
            "SITTING" -> context.getString(R.string.sitting)
            "STANDING" -> context.getString(R.string.standing)
            "LAYING" -> context.getString(R.string.laying)
            else -> activity // Return original if not found
        }
    }
}
