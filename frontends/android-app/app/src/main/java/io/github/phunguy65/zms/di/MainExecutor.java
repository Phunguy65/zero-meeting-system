package io.github.phunguy65.zms.di;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/** Qualifier for main thread executor (UI updates). */
@Qualifier @Retention(RetentionPolicy.RUNTIME)
public @interface MainExecutor {}
