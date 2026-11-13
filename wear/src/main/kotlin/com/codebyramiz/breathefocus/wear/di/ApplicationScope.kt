package com.codebyramiz.breathefocus.wear.di

import javax.inject.Qualifier

/** Process-wide scope for work that must outlive a screen, such as saving a finished session. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
