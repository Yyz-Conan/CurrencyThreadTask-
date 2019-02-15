package task.executor;

import task.executor.joggle.IConsumerAttribute;

public class ConsumerEngine<D> extends BaseConsumerTask<D> {


    protected BaseConsumerTask mConsumerTask;
    private IConsumerAttribute mAttribute;
    private ConsumerTaskExecutor executor;

    public ConsumerEngine(ConsumerTaskExecutor executor, BaseConsumerTask task) {
        this.mConsumerTask = task;
        this.executor = executor;
    }

    protected void setAttribute(IConsumerAttribute attribute) {
        synchronized (ConsumerEngine.class) {
            this.mAttribute = attribute;
        }
    }

    protected void changeBaseConsumerTask(BaseConsumerTask task) {
        this.mConsumerTask = task;
    }

    protected IConsumerAttribute getAttribute() {
        synchronized (ConsumerEngine.class) {
            return mAttribute;
        }
    }

    @Override
    protected void onInitTask() {
        mConsumerTask.onInitTask();
    }

    @Override
    protected void onRunLoopTask() {
        mConsumerTask.onRunLoopTask();
        onCreateData();
        // 没有数据是否需要线程休眠
        if (executor.isIdleStateSleep() && (mAttribute == null || mAttribute.getCacheDataSize() == 0)) {
            executor.waitTask();
        } else {
            if (executor.getAsyncTaskExecutor() == null) {
                onProcess();
            }
        }
    }


    @Override
    protected void onCreateData() {
        mConsumerTask.onCreateData();
    }

    @Override
    protected void onProcess() {
        mConsumerTask.onProcess();
    }


    @Override
    protected void onIdleStop() {
        mConsumerTask.onIdleStop();
    }

    @Override
    protected void onDestroyTask() {
        mConsumerTask.onDestroyTask();
        if (!executor.getIdleStopState() && mAttribute != null) {
            mAttribute.clearCacheData();
        }
    }
}
