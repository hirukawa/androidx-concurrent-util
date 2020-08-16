package onl.oss.androidx.concurrent;

import androidx.lifecycle.MutableLiveData;

import onl.oss.androidx.lifecycle.RestorableViewModel;

public class WorkerResultViewModel<V> extends RestorableViewModel {
    private MutableLiveData<Task<V>> task;
    private MutableLiveData<Worker.State> state;
    private MutableLiveData<V> value;
    private MutableLiveData<Throwable> exception;

    public MutableLiveData<Task<V>> getTask() {
        if(task == null) {
            // タスクは永続化しません。
            // stateが SUCCEEDED, FAILED, CANCELLED のいずれにも至らない状態で task が失われた場合、
            // task が失敗したものとして扱います。（このような状況はプロセスキルによって発生します。）
            task = new MutableLiveData<Task<V>>();
        }
        return task;
    }

    public MutableLiveData<Worker.State> getState() {
        if(state == null) {
            state = getLiveData("state");
        }
        return state;
    }

    public MutableLiveData<V> getValue() {
        if(value == null) {
            value = getLiveData("value");
        }
        return value;
    }

    public MutableLiveData<Throwable> getException() {
        if(exception == null) {
            exception = getLiveData("exception");
        }
        return exception;
    }
}
