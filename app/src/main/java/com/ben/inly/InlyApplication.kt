package com.ben.inly

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The master trigger for Hilt Dependency Injection.
 * Even though it's empty, this class must exist and be registered in the AndroidManifest
 * so Hilt knows where to attach all the @Inject dependencies.
 */
@HiltAndroidApp
class InlyApplication : Application()