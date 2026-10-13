package com.citypulse.ui.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

fun Context.hasLocationPermission(): Boolean = LocationPermissions.any {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

/** Pide el permiso de ubicación (precisa o aproximada; vale cualquiera de las dos). */
class LocationPermissionRequester internal constructor(
    private val context: Context,
    private val launch: () -> Unit,
) {
    /**
     * Si el usuario ya lo denegó de forma permanente, el sistema no vuelve a mostrar el
     * diálogo: en ese caso se abren los ajustes de la app.
     */
    fun request(afterDenial: Boolean = false) {
        val activity = context.findActivity()
        val canAskAgain = activity != null && LocationPermissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
        if (afterDenial && !canAskAgain) openAppSettings() else launch()
    }

    private fun openAppSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
fun rememberLocationPermissionRequester(onResult: (granted: Boolean) -> Unit): LocationPermissionRequester {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        onResult(result.values.any { it })
    }
    return remember(context, launcher) { LocationPermissionRequester(context) { launcher.launch(LocationPermissions) } }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
