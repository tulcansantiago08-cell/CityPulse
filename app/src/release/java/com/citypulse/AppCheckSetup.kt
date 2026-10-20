package com.citypulse

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** En release, Play Integrity certifica que las peticiones vienen de la app instalada desde Play. */
fun installAppCheck(@Suppress("UNUSED_PARAMETER") context: Context) {
    Firebase.appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
}
