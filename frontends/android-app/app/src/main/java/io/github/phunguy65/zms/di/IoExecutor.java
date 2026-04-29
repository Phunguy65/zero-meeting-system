package io.github.phunguy65.zms.di;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/** Qualifier for I/O-bound executor (network, database, file operations). */
@Qualifier @Retention(RetentionPolicy.RUNTIME)
public @interface IoExecutor {}
