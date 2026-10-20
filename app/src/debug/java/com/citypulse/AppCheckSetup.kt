package com.citypulse

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * En debug (emulador o móvil de pruebas) se usa el proveedor de depuración de App Check.
 *
 * Si `local.properties` define APP_CHECK_DEBUG_TOKEN (un token ya registrado en Firebase console
 * → App Check → Administrar tokens de depuración), se fija antes de arrancar: así no cambia al
 * reinstalar la app o al probar en otro dispositivo. Si no, Firebase genera uno y lo escribe en Logcat.
 */
fun installAppCheck(context: Context) {
    val token = BuildConfig.APP_CHECK_DEBUG_TOKEN
    if (token.isNotBlank()) {
        val store = "com.google.firebase.appcheck.debug.store.${FirebaseApp.getInstance().persistenceKey}"
        context.getSharedPreferences(store, Context.MODE_PRIVATE).edit {
            putString("com.google.firebase.appcheck.debug.DEBUG_SECRET", token)
        }
    }
    Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
