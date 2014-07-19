
package imagelogic.threads;

import imagelogic.utils.ImageUtils;
import imagelogic.utils.Log;

import java.util.ArrayDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.TargetApi;
import android.os.Handler;
import android.os.Message;
import android.os.Process;

/**
 * 图片线程类,拷贝AsyncTask.java源码 修改了线程池属性，让并发线程按顺序执行
 * @author LeeFranker
 *
 * @param <Params>
 * @param <Progress>
 * @param <Result>
 */
public abstract class ImageAsyncTask<Params, Progress, Result> {
    private static final String TAG = "ImageAsyncTask";
    // 核心线程数
    private static final int CORE_POOL_SIZE = 1;
    // 最大线程数
    private static final int MAXIMUM_POOL_SIZE = 2;
    // 活跃数
    private static final int KEEP_ALIVE = 1;

    private static final int MESSAGE_POST_RESULT = 0x1;
    private static final int MESSAGE_POST_PROGRESS = 0x2;

    private static final InternalHandler sHandler = new InternalHandler();

    // net thread factory
    private static final ThreadFactory sNetThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "Net Thread #" + mCount.getAndIncrement());
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        }
    };

    // cache thread factory
    private static final ThreadFactory sCacheThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            Thread tread = new Thread(r, "Cache Thread #" + mCount.getAndIncrement());
            tread.setPriority(Thread.NORM_PRIORITY - 1);
            return tread;
        }
    };

    public static final Executor SERIAL_EXECUTOR = ImageUtils.hasHoneycomb() ? new SerialExecutor()
            : Executors.newSingleThreadExecutor(sCacheThreadFactory);

    public static Executor NET_THREAD_EXECUTOR = Executors.newFixedThreadPool(4, sNetThreadFactory);

    private final WorkerRunnable<Params, Result> mWorker;

    private final FutureTask<Result> mFuture;

    private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(
            10);

    private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;

    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, sPoolWorkQueue, sCacheThreadFactory,
            new ThreadPoolExecutor.DiscardOldestPolicy());

    // 默认线程任务未执行
    private volatile Status mStatus = Status.PENDING;

    private final AtomicBoolean mCancelled = new AtomicBoolean();
    private final AtomicBoolean mTaskInvoked = new AtomicBoolean();

    @TargetApi(11)
    private static class SerialExecutor implements Executor {
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
        Runnable mActive;

        public synchronized void execute(final Runnable r) {
            mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }

    // 线程执行状态
    public enum Status {
        /** 线程还没有执行 */
        PENDING,
        /** 线程正在运行 */
        RUNNING,
        /** 线程执行完毕 */
        FINISHED,
    }

    /** @hide */
    public static void init() {
        sHandler.getLooper();
    }

    public static void setDefaultExecutor(Executor exec) {
        sDefaultExecutor = exec;
    }

    // 构造方法
    public ImageAsyncTask() {
        // 异步执行任务对象
        mWorker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                mTaskInvoked.set(true);
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                return postResult(doInBackground(mParams));
            }
        };
        /**
         * FutureTask类是Future 的一个实现，并实现了Runnable，所以可通过Excutor(线程池)
         * 来执行,也可传递给Thread对象执行
         */
        /**
         * 可使用 FutureTask 包装 Callable 或 Runnable 对象。因为 FutureTask
         * 实现了Runnable，所以可将 FutureTask 提交给 Executor 执行。
         */
        mFuture = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                try {
                    /**
                     * Future有个get方法而获取结果只有在计算完成时获取，否则会一直阻塞直到任务转入完成状态，
                     * 然后会返回结果或者抛出异常
                     */
                    postResultIfNotInvoked(get());
                } catch (InterruptedException e) {
                } catch (ExecutionException e) {
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }

    private void postResultIfNotInvoked(Result result) {
        final boolean wasTaskInvoked = mTaskInvoked.get();
        if (!wasTaskInvoked) {
            postResult(result);
        }
    }

    private Result postResult(Result result) {
        @SuppressWarnings("unchecked")
        Message message = sHandler.obtainMessage(MESSAGE_POST_RESULT, new AsyncTaskResult<Result>(
                this, result));
        message.sendToTarget();
        return result;
    }

    public final Status getStatus() {
        return mStatus;
    }

    protected abstract Result doInBackground(Params... params);

    protected void onPreExecute() {

    }

    protected void onPostExecute(Result result) {

    }

    protected void onProgressUpdate(Progress... values) {

    }

    protected void onCancelled(Result result) {
        onCancelled();
    }

    protected void onCancelled() {

    }

    // 任务是否取消
    public final boolean isCancelled() {
        return mCancelled.get();
    }

    // 取消任务操作
    public final boolean cancel(boolean mayInterruptIfRunning) {
        mCancelled.set(true);
        return mFuture.cancel(mayInterruptIfRunning);
    }

    // 获取任务结果操作
    public final Result get() throws InterruptedException, ExecutionException {
        return mFuture.get();
    }

    // 多长时间后获取任务结果操作
    public final Result get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return mFuture.get(timeout, unit);
    }

    // 执行线程任务
    public final ImageAsyncTask<Params, Progress, Result> execute(Params... params) {
        return executeOnExecutor(sDefaultExecutor, params);
    }

    // 开始执行线程任务
    @SuppressWarnings("incomplete-switch")
    public final ImageAsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec,
            Params... params) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    Log.i(TAG, "不能执行运行中的任务");
                case FINISHED:
                    Log.i(TAG, "任务执行完毕，该任务只执行一次");
            }
            return null;
        }
        Log.d(TAG, "第一次执行任务");
        // 初始化任务开始状态
        mStatus = Status.RUNNING;
        // 开始执行
        onPreExecute();
        // 异步任务的数据
        mWorker.mParams = params;
        exec.execute(mFuture);
        // 返回自己
        return this;
    }

    // 执行线程任务
    public static void execute(Runnable runnable) {
        sDefaultExecutor.execute(runnable);
    }

    // 执行线程刷新任务
    protected final void publishProgress(Progress... values) {
        if (!isCancelled()) {
            sHandler.obtainMessage(MESSAGE_POST_PROGRESS,
                    new AsyncTaskResult<Progress>(this, values)).sendToTarget();
        }
    }

    // 取消执行任务
    private void finish(Result result) {
        if (isCancelled()) {
            // 取消操作-用户可以实现取消操作
            onCancelled(result);
        } else {
            // 没有取消操作-执行任务结果操作（用户自己可以实现）
            Log.d(TAG, "执行onPostExecute");
            onPostExecute(result);
        }
        mStatus = Status.FINISHED;// 重置任务状态
    }

    private static class InternalHandler extends Handler {
        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            @SuppressWarnings("rawtypes")
            AsyncTaskResult result = (AsyncTaskResult) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    // Log.d(TAG, "handler接收，处理finish");
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    break;
            }
        }
    }

    // 注：Callable封装了异步运行的任务，比Runnable对了一个返回值
    private static abstract class WorkerRunnable<Params, Result> implements Callable<Result> {
        Params[] mParams;
    }

    // 异步任务结果对象
    private static class AsyncTaskResult<Data> {
        @SuppressWarnings("rawtypes")
        final ImageAsyncTask mTask;
        final Data[] mData;

        @SuppressWarnings("rawtypes")
        AsyncTaskResult(ImageAsyncTask task, Data... data) {
            mTask = task;
            mData = data;
        }
    }
}
