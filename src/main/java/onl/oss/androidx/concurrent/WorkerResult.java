package onl.oss.androidx.concurrent;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import java.io.Serializable;

@SuppressWarnings("serial")
public class WorkerResult<V> implements Serializable {

    private Worker.State state;
    private V value;
    private Throwable exception;

    public WorkerResult(Worker.State state) {
        this.state = state;
    }

    public WorkerResult(V value) {
        this.value = value;
        this.state = Worker.State.SUCCEEDED;
    }

    public WorkerResult(Throwable exception) {
        this.exception = exception;
        this.state = Worker.State.FAILED;
    }

    public Worker.State getState() {
        return state;
    }

    public V getValue() {
        return value;
    }

    public Throwable getException() {
        return exception;
    }

    public void dispatch(AsyncCallable.Success<V> onSucceeded) {
        dispatch(onSucceeded, null, null, null);
    }

    public void dispatch(AsyncCallable.Success<V> onSucceeded, Async.Complete onCompleted) {
        dispatch(onSucceeded, null, null, onCompleted);
    }

    public void dispatch(AsyncCallable.Success<V> onSucceeded, Async.Failure onFailed, Async.Complete onCompleted) {
        dispatch(onSucceeded, onFailed, null, onCompleted);
    }

    public void dispatch(AsyncCallable.Success<V> onSucceeded, Async.Failure onFailed) {
        dispatch(onSucceeded, onFailed, null, null);
    }

    public void dispatch(AsyncCallable.Success<V> onSucceeded, Async.Failure onFailed, Async.Cancel onCancelled) {
        dispatch(onSucceeded, onFailed, onCancelled, null);
    }

    public void dispatch(Async.Complete onCompleted) {
        dispatch((AsyncCallable.Success<V>)null, (Async.Failure)null, (Async.Cancel) null, onCompleted);
    }

    public void dispatch(AsyncCallable.Success<V> onSucceeded, Async.Failure onFailed, Async.Cancel onCancelled, Async.Complete onCompleted) {
        Exception exception = null;
        try {
            if(state == Worker.State.SUCCEEDED && onSucceeded != null) {
                onSucceeded.onSucceeded(getValue());
            } else if(state == Worker.State.FAILED) {
                Throwable t = getException();
                if(t instanceof Error) {
                    throw (Error)t;
                }
                onFailed.onFailed((Exception)t);
            } else if(state == Worker.State.CANCELLED && onCancelled != null) {
                onCancelled.onCancelled();
            }
        } catch(Exception e) {
            exception = e;
        } finally {
            if(onCompleted != null && (state == Worker.State.SUCCEEDED
                    || (state == Worker.State.FAILED && !(getException() instanceof Error))
                    || state == Worker.State.CANCELLED)) {
                try {
                    onCompleted.onCompleted(state);
                } catch(Exception e) {
                    if(exception == null) {
                        exception = e;
                    }
                }
            }
        }
        if(exception != null) {
            Thread.UncaughtExceptionHandler ueh = Thread.currentThread().getUncaughtExceptionHandler();
            if(ueh != null) {
                ueh.uncaughtException(Thread.currentThread(), exception);
            } else {
                throw new AsyncWrappedException(exception);
            }
        }
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(WorkerResult.class.getCanonicalName(), this);
        return bundle;
    }

    public static <T> WorkerResult<T> fromBundle(Bundle bundle) {
        @SuppressWarnings("unchecked")
        WorkerResult<T> workerResult = (WorkerResult<T>)bundle.getSerializable(WorkerResult.class.getCanonicalName());
        return workerResult;
    }

    public static <V> void dispatch(Fragment fragment, String requestKey, AsyncCallable.Success<V> onSucceeded) {
        dispatch(fragment, requestKey, onSucceeded, null, null, null);
    }

    public static <V> void dispatch(Fragment fragment, String requestKey, AsyncCallable.Success<V> onSucceeded, Async.Complete onCompleted) {
        dispatch(fragment, requestKey, onSucceeded, null, null, onCompleted);
    }

    public static <V> void dispatch(Fragment fragment, String requestKey, AsyncCallable.Success<V> onSucceeded, Async.Failure onFailed, Async.Complete onCompleted) {
        dispatch(fragment, requestKey, onSucceeded, onFailed, null, onCompleted);
    }

    public static <V> void dispatch(Fragment fragment, String requestKey, AsyncCallable.Success<V> onSucceeded, Async.Failure onFailed) {
        dispatch(fragment, requestKey, onSucceeded, onFailed, null, null);
    }

    public static <V> void dispatch(Fragment fragment, String requestKey, AsyncCallable.Success<V> onSucceeded, Async.Failure onFailed, Async.Cancel onCancelled) {
        dispatch(fragment, requestKey, onSucceeded, onFailed, onCancelled, null);
    }

    public static <V> void dispatch(Fragment fragment, String requestKey, Async.Complete onCompleted) {
        dispatch(fragment, requestKey, null, null, null, onCompleted);
    }

    public static <V> void dispatch(Fragment fragment, String requestKey, AsyncCallable.Success<V> onSucceeded, Async.Failure onFailed, Async.Cancel onCancelled, Async.Complete onCompleted) {
        final AsyncCallable.Success<V> _onSucceeded = onSucceeded;
        final Async.Failure _onFailed = onFailed;
        final Async.Cancel _onCancelled = onCancelled;
        final Async.Complete _onCompleted = onCompleted;
        fragment.getParentFragmentManager().setFragmentResultListener(requestKey, fragment.getViewLifecycleOwner(), new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                WorkerResult<V> workerResult = WorkerResult.fromBundle(result);
                workerResult.dispatch(_onSucceeded, _onFailed, _onCancelled, _onCompleted);
            }
        });
    }
}
