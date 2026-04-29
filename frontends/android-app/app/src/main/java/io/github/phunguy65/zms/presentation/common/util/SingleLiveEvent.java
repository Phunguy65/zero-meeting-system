package io.github.phunguy65.zms.presentation.common.util;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A lifecycle-aware observable that sends only new updates after subscription,
 * used for one-time events like navigation and Snackbar messages.
 *
 * <p>This avoids a common problem with events: on configuration change (like rotation)
 * an update can be emitted if the observer is active. This LiveData only calls the
 * observer if there's an explicit call to setValue() or call().
 *
 * <p>Note that only one observer is going to be notified of changes.
 *
 * @param <T> The type of data held by this instance
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private final AtomicBoolean pending = new AtomicBoolean(false);

    @MainThread
    @Override
    public void observe(LifecycleOwner owner, Observer<? super T> observer) {
        super.observe(owner, t -> {
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t);
            }
        });
    }

    @MainThread
    @Override
    public void setValue(@Nullable T t) {
        pending.set(true);
        super.setValue(t);
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    public void call() {
        setValue(null);
    }
}
