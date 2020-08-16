package onl.oss.androidx.concurrent;

public interface AsyncCallable<V> {

    void cancel();

    AsyncCallable<V> onCancelled(Async.Cancel callback);

    AsyncCallable<V> onSucceeded(Success<V> callback);

    AsyncCallable<V> onFailed(Async.Failure callback);

    AsyncCallable<V> onCompleted(Async.Complete callback);

    @FunctionalInterface
    interface Success<T> {
        void onSucceeded(T result) throws Exception;
    }
}
