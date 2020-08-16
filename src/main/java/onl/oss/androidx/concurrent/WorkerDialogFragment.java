package onl.oss.androidx.concurrent;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;

import java.io.NotActiveException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import onl.oss.androidx.lifecycle.RestorableViewModelProvider;

public abstract class WorkerDialogFragment<V> extends DialogFragment {

    private Callable<V> worker;
    private WorkerResultViewModel<V> workerResultViewModel;

    public String getRequestKey() {
        return getTag();
    }

    protected void setWorkerResult(WorkerResult<V> result) {
        getParentFragmentManager().setFragmentResult(getRequestKey(), result.toBundle());
    }

    public void show(@NonNull FragmentManager manager, @NonNull String requestKey, @NonNull Callable<V> worker) {
        this.worker = worker;
        super.show(manager, requestKey);
    }

    public int show(@NonNull FragmentTransaction transaction, @NonNull String requestKey, @NonNull Callable<V> worker) {
        this.worker = worker;
        return super.show(transaction, requestKey);
    }

    public void showNow(@NonNull FragmentManager manager, @NonNull String requestKey, @NonNull Callable<V> worker) {
        this.worker = worker;
        super.showNow(manager, requestKey);
    }

    @NonNull
    @Override
    public LayoutInflater onGetLayoutInflater(@Nullable Bundle savedInstanceState) {
        LayoutInflater layoutInflater = super.onGetLayoutInflater(savedInstanceState);
        onSetupWorker(savedInstanceState);
        return layoutInflater;
    }

    protected void onSetupWorker(Bundle savedInstanceState) {
        if(savedInstanceState == null && worker == null) {
            throw new IllegalStateException("worker is null");
        }
        @SuppressWarnings("unchecked")
        WorkerResultViewModel<V> vm = (WorkerResultViewModel<V>)new RestorableViewModelProvider(requireActivity(), savedInstanceState).get(WorkerResultViewModel.class);
        workerResultViewModel = vm;
        workerResultViewModel.getState().observe(requireActivity(), new Observer<Worker.State>() {
            @Override
            public void onChanged(Worker.State state) {
                switch(state) {
                    case READY:
                        break;
                    case SCHEDULED:
                        break;
                    case RUNNING:
                        break;
                    case SUCCEEDED:
                        setWorkerResult(new WorkerResult<V>(workerResultViewModel.getValue().getValue()));
                        dismiss();
                        break;
                    case FAILED:
                        setWorkerResult(new WorkerResult<V>(workerResultViewModel.getException().getValue()));
                        dismiss();
                        break;
                    case CANCELLED:
                        setWorkerResult(new WorkerResult<V>(Worker.State.CANCELLED));
                        // キャンセルの場合は dismiss を呼び出す必要はありません。
                        // dismiss によってタスクがキャンセルされるからです。
                        break;
                }
            }
        });
        if(worker == null) {
            Task<V> task = workerResultViewModel.getTask().getValue();
            if(task == null) {
                Worker.State state = workerResultViewModel.getState().getValue();
                if(state != Worker.State.SUCCEEDED && state != Worker.State.FAILED && state != Worker.State.CANCELLED) {
                    workerResultViewModel.getException().setValue(new ExecutionException("The task has been lost", new NotActiveException()));
                    workerResultViewModel.getState().setValue(Worker.State.FAILED);
                }
            }
        } else {
            final Callable<V> callable = worker;
            worker = null;
            workerResultViewModel.getState().setValue(Worker.State.READY);
            Task<V> task = new Task<V>() {
                @Override
                protected V call() throws Exception {
                    return callable.call();
                }
                @Override
                protected void scheduled() {
                    workerResultViewModel.getState().setValue(State.SCHEDULED);
                }
                @Override
                protected void running() {
                    workerResultViewModel.getState().setValue(State.RUNNING);
                }
                @Override
                protected void succeeded() {
                    workerResultViewModel.getValue().setValue(getValue());
                    workerResultViewModel.getState().setValue(State.SUCCEEDED);
                }
                @Override
                protected void failed() {
                    workerResultViewModel.getException().setValue(getException());
                    workerResultViewModel.getState().setValue(State.FAILED);
                }
                @Override
                protected void cancelled() {
                    workerResultViewModel.getState().setValue(State.CANCELLED);
                }
            };
            workerResultViewModel.getTask().setValue(task);
            Async.getExecutor().execute(task);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        // onDismissはアプリがバックグランドにまわったときにonStop～onDestroyのときにも呼ばれます。
        // ユーザーの操作によってダイアログがなくなる場合と区別するためにライフサイクルRESUMEDであることを確認します。
        if(getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            Task<V> task = workerResultViewModel.getTask().getValue();
            if(task != null && task.isRunning()) {
                task.cancel();
            }
        }
        super.onDismiss(dialog);
    }
}
