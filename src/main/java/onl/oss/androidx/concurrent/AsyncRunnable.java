package onl.oss.androidx.concurrent;

public interface AsyncRunnable {

    void cancel();

    AsyncRunnable onCancelled(Async.Cancel callback);

    AsyncRunnable onSucceeded(Success callback);

    AsyncRunnable onFailed(Async.Failure callback);

    AsyncRunnable onCompleted(Async.Complete callback);

    @FunctionalInterface
    interface Success {
        void onSucceeded() throws Exception;
    }
}
