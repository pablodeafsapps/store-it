package org.deafsapps.storeit.di

import org.koin.core.module.Module
import org.koin.ksp.generated.module

internal actual fun platformModule(): Module = IosModule().module
