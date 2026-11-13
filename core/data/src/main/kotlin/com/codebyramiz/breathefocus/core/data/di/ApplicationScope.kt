package com.codebyramiz.breathefocus.core.data.di

import javax.inject.Qualifier

/** Process-wide scope for work that must outlive a screen, such as saving a session or publishing to the other device. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
