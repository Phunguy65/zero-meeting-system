package io.github.phunguy65.zms.di;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/** Qualifier for the LiveKit server URL. */
@Qualifier @Retention(RetentionPolicy.RUNTIME)
public @interface LiveKitUrl {}
