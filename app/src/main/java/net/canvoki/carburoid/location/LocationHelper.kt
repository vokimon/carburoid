package net.canvoki.carburoid.location

import android.content.Context
import net.canvoki.carburoid.R

class LocationHelper {
    companion object {
        fun getNotAvailableMessage(context: Context): String {
            return context.getString(R.string.location_not_available)
        }

        fun getErrorMessage(context: Context): String {
            return context.getString(R.string.location_error)
        }

        fun getForbiddenMessage(context: Context): String {
            return context.getString(R.string.location_forbidden)
        }
    }
}
