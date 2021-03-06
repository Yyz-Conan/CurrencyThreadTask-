package task.executor;


import task.executor.joggle.IAttribute;
import task.executor.joggle.IConsumerAttribute;
import task.executor.joggle.IConsumerTaskExecutor;
import task.executor.joggle.ILoopTaskExecutor;

/**
 * 消费任务执行者
 * Created by dell on 2/9/2018.
 *
 * @author yyz
 */
public class ConsumerTaskExecutor extends LoopTaskExecutor implements IConsumerTaskExecutor {

    /***没有输任务则休眠标志位，为true则休眠*/
    private volatile boolean isIdleStateSleep = false;

    /***异步处理数据任务*/
    private volatile ILoopTaskExecutor asyncTaskExecutor = null;


    /**
     * 创建任务
     *
     * @param container 任务
     * @throws NullPointerException
     */
    protected ConsumerTaskExecutor(TaskContainer container) {
        super(container);
        BaseConsumerTask consumerTask = container.getTask();
        executorTask = new ConsumerEngine(this, consumerTask);
    }

    @Override
    public synchronized boolean changeTask(BaseLoopTask task) {
        ConsumerEngine consumerEngine = (ConsumerEngine) executorTask;
        consumerEngine.changeBaseConsumerTask((BaseConsumerTask) task);
        return super.changeTask(consumerEngine);
    }

    /**
     * 获取属性
     *
     * @return
     */
    @Override
    public <T> T getAttribute() {
        ConsumerEngine coreTask = (ConsumerEngine) executorTask;
        return (T) coreTask.getAttribute();
    }


    @Override
    public void setAttribute(IAttribute attribute) {
        ConsumerEngine coreTask = (ConsumerEngine) executorTask;
        coreTask.setAttribute((IConsumerAttribute) attribute);
    }

    @Override
    public void resumeTask() {
        super.resumeTask();
        if (asyncTaskExecutor != null) {
            asyncTaskExecutor.resumeTask();
        }
    }

    @Override
    public void pauseTask() {
        super.pauseTask();
        if (asyncTaskExecutor != null) {
            asyncTaskExecutor.pauseTask();
        }
    }

    /**
     * 设置缓存区没有数据线程进入休眠
     *
     * @param state true 进入休眠状态
     */
    @Override
    public void setIdleStateSleep(boolean state) {
        this.isIdleStateSleep = state;
    }

    protected boolean isIdleStateSleep() {
        return isIdleStateSleep;
    }

    @Override
    public void stopTask() {
        stopAsyncProcessData();
        super.stopTask();
    }

    /**
     * 开启异步处理数据模式,
     * 开启后 onCreateData ,onProcess 分别不同线程来执行
     */
    @Override
    public synchronized ILoopTaskExecutor startAsyncProcessData() {
        if (asyncTaskExecutor == null) {
            ConsumerEngine coreTask = (ConsumerEngine) executorTask;
            AsyncProcessDataTask asyncTask = new AsyncProcessDataTask(coreTask);
            asyncTaskExecutor = asyncTask.startTask();
        }
        return asyncTaskExecutor;
    }

    /**
     * 关闭异步处理数据模式
     */
    @Override
    public synchronized void stopAsyncProcessData() {
        if (asyncTaskExecutor != null) {
            asyncTaskExecutor.stopTask();
            asyncTaskExecutor = null;
        }
    }

    /**
     * 获取异步处理执行器
     *
     * @return
     */
    @Override
    public synchronized ILoopTaskExecutor getAsyncTaskExecutor() {
        return asyncTaskExecutor;
    }
}
