package onl.oss.androidx.concurrent;

public interface Worker<V> {

    enum State {
        READY,
        SCHEDULED,
        RUNNING,
        SUCCEEDED,
        CANCELLED,
        FAILED
    }

    State getState();

    V getValue();

    Throwable getException();

    boolean isRunning();

    boolean cancel();
}
