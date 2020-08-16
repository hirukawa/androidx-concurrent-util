package onl.oss.androidx.concurrent;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Task<V> extends FutureTask<V> implements Worker<V> {

    private Handler handler = new Handler(Looper.getMainLooper());
    private AtomicReference<V> valueUpdate = new AtomicReference<V>();
    private volatile boolean started = false;
    private State state = State.READY;
    private boolean running = false;
    private V value;
    private Throwable exception;

    public Task() {
        this(new TaskCallable<V>());
    }

    private Task(TaskCallable<V> callableAdapter) {
        super(callableAdapter);
        callableAdapter.task = this;
    }

    protected abstract V call() throws Exception;

    final void setState(State value) {
        checkThread();
        State s = getState();
        if(s != State.CANCELLED) {
            this.state = value;
            setRunning(value == State.SCHEDULED || value == State.RUNNING);

            switch(state) {
                case CANCELLED:
                    cancelled();
                    break;
                case FAILED:
                    failed();
                    break;
                case READY:
                    break;
                case RUNNING:
                    running();
                    break;
                case SCHEDULED:
                    scheduled();
                    break;
                case SUCCEEDED:
                    succeeded();
                    break;
                default:
                    throw new AssertionError("Should be unreachable");
            }
        }
    }

    @Override
    public State getState() {
        checkThread();
        return state;
    }

    protected void scheduled() {
    }

    protected void running() {
    }

    protected void succeeded() {
    }

    protected void cancelled() {
    }

    protected void failed() {
    }

    /*
    private void setValue(V value) {
        checkThread();
        this.value = value;
    }
    */

    @Override
    public final V getValue() {
        checkThread();
        return value;
    }

    private void _setException(Throwable value) {
        checkThread();
        this.exception = value;
    }

    @Override
    public final Throwable getException() {
        checkThread();
        return exception;
    }

    private void setRunning(boolean value) {
        checkThread();
        running = value;
    }

    @Override
    public final boolean isRunning() {
        checkThread();
        return running;
    }

    @Override
    public boolean cancel() {
        return cancel(true);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean flag = super.cancel(mayInterruptIfRunning);
        if(flag) {
            if(Looper.getMainLooper().getThread() == Thread.currentThread()) {
                setState(State.CANCELLED);
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setState(State.CANCELLED);
                    }
                });
            }
        }
        return flag;
    }

    protected void updateValue(V value) {
        if(Looper.getMainLooper().getThread() == Thread.currentThread()) {
            this.value = value;
        } else {
            if(valueUpdate.getAndSet(value) == null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Task.this.value = valueUpdate.getAndSet(null);
                    }
                });
            }
        }
    }

    private void checkThread() {
        if(started && Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Task must only be used from the Main Thread");
        }
    }

    private static final class TaskCallable<V> implements Callable<V> {
        private Task<V> task;

        private TaskCallable() {
        }

        @Override
        public V call() throws Exception {
            task.started = true;
            task.handler.post(new Runnable() {
                @Override
                public void run() {
                    task.setState(State.SCHEDULED);
                    task.setState(State.RUNNING);
                }
            });
            try {
                final V result = task.call();
                if(!task.isCancelled()) {
                    task.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            task.updateValue(result);
                            task.setState(State.SUCCEEDED);
                        }
                    });
                    return result;
                } else {
                    return null;
                }
            } catch(final Throwable t) {
                task.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        task._setException(t);
                        task.setState(State.FAILED);
                    }
                });
                if(t instanceof Exception) {
                    throw (Exception)t;
                } else {
                    throw new Exception(t);
                }
            }
        }
    }
}
