package com.citypulse

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * En debug (emulador) se usa el proveedor de depuración: al arrancar, Logcat muestra un
 * "debug token" que hay que registrar en Firebase console → App Check → Administrar tokens.
 */
fun installAppCheck() {
    Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
