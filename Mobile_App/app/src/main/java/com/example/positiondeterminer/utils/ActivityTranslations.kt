package com.example.positiondeterminer.utils

import android.content.Context
import com.example.positiondeterminer.R

fun getActivityTranslation(context: Context, activity: String): String {
    return when (activity.uppercase()) {
        "WALKING" -> context.getString(R.string.walking)
        "WALKING_UPSTAIRS" -> context.getString(R.string.walking_upstairs)
        "WALKING_DOWNSTAIRS" -> context.getString(R.string.walking_downstairs)
        "SITTING" -> context.getString(R.string.sitting)
        "STANDING" -> context.getString(R.string.standing)
        "LAYING" -> context.getString(R.string.laying)
        else -> activity
    }
}
