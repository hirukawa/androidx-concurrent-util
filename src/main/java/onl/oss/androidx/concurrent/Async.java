package onl.oss.androidx.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class Async<V> implements AsyncRunnable, AsyncCallable<V> {

    private static ThreadFactory defaultThreadFactory;

    public static ThreadFactory getDefaultThreadFactory() {
        return defaultThreadFactory;
    }

    public static void setDefaultThreadFactory(ThreadFactory factory) {
        defaultThreadFactory = factory;
        executor = null;
    }

    private static Executor executor;

    public static Executor getExecutor() {
        if(executor == null) {
            ThreadFactory factory = getDefaultThreadFactory();
            if(factory == null) {
                factory = new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable);
                        thread.setDaemon(true);
                        return thread;
                    }
                };
            }
            executor = Executors.newCachedThreadPool(factory);
        }
        return executor;
    }

    public static void setExecutor(Executor executor) {
        Async.executor = executor;
    }

    public static AsyncRunnable execute(final LooseRunnable runnable) {
        Async<Void> async = new Async<Void>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                runnable.run();
                return null;
            }
        });
        getExecutor().execute(async.task);
        return async;
    }

    public static <V> AsyncCallable<V> execute(Callable<V> callable) {
        Async<V> async = new Async<V>(callable);
        getExecutor().execute(async.task);
        return async;
    }

    private Task<V> task;
    private Cancel cancel;
    private AsyncRunnable.Success runnableSuccess;
    private AsyncCallable.Success<V> callableSuccess;
    private Failure failure;
    private Complete complete;

    private Async(final Callable<V> callable) {
        task = new Task<V>() {
            @Override
            protected V call() throws Exception {
                return callable.call();
            }
            @Override
            protected void cancelled() {
                Async.this.cancelled();
            }
            @Override
            protected void succeeded() {
                Async.this.succeeded();
            }
            @Override
            protected void failed() {
                Async.this.failed();
            }
        };
    }

    @Override
    public void cancel() {
        task.cancel(true);
    }

    protected void cancelled() {
        Exception exception = null;
        try {
            if(cancel != null) {
                cancel.onCancelled();
            }
        } catch(Exception e) {
            exception = e;
        } finally {
            if(complete != null) {
                try {
                    complete.onCompleted(Worker.State.CANCELLED);
                } catch(Exception e) {
                    if(exception == null) {
                        exception = e;
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
    }

    protected void succeeded() {
        Exception exception = null;
        try {
            if(runnableSuccess != null) {
                runnableSuccess.onSucceeded();
            } else if(callableSuccess != null) {
                callableSuccess.onSucceeded(task.getValue());
            }
        } catch(Exception e) {
            exception = e;
        } finally {
            if(complete != null) {
                try {
                    complete.onCompleted(Worker.State.SUCCEEDED);
                } catch(Exception e) {
                    if(exception == null) {
                        exception = e;
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
    }

    protected void failed() {
        Throwable t = task.getException();
        if(t instanceof Error) {
            throw (Error)t;
        }
        Exception exception = null;
        try {
            if(failure != null) {
                failure.onFailed((Exception)t);
            } else {
                exception = (Exception)t;
            }
        } catch(Exception e) {
            exception = e;
        } finally {
            if(complete != null) {
                try {
                    complete.onCompleted(Worker.State.FAILED);
                } catch(Exception e) {
                    if(exception == null) {
                        exception = e;
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
    }

    public Async<V> onCancelled(Cancel callback) {
        this.cancel = callback;
        return this;
    }

    public Async<V> onSucceeded(AsyncRunnable.Success callback) {
        this.callableSuccess = null;
        this.runnableSuccess = callback;
        return this;
    }

    public Async<V> onSucceeded(AsyncCallable.Success<V> callback) {
        this.runnableSuccess = null;
        this.callableSuccess = callback;
        return this;
    }

    public Async<V> onFailed(Failure callback) {
        this.failure = callback;
        return this;
    }

    public Async<V> onCompleted(Complete callback) {
        this.complete = callback;
        return this;
    }

    @FunctionalInterface
    public interface Cancel {
        void onCancelled() throws Exception;
    }

    @FunctionalInterface
    public interface Failure {
        void onFailed(Exception exception) throws Exception;
    }

    @FunctionalInterface
    public interface Complete {
        void onCompleted(Worker.State state) throws Exception;
    }
}
