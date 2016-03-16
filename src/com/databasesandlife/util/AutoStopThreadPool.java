package com.databasesandlife.util;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/**
 * A pool of threads which execute tasks, and which will shut down after all tasks have been completed.
 * Tasks, during their execution, may schedule further tasks for execution.
 * <p>
 * <h3>Usage</h3>
 * <p>
 * <p>Firstly the object is created, and then tasks are added using the <code>addTask</code> method.
 * (These tasks, when executed, may add other tasks, also using the <code>addTask</code> method.)
 * The <code>execute</code> method is then called; this starts the threads and returns when all tasks (and tasks created by
 * those tasks) have been completed.
 * <pre>
 *  String name = "my-task";  // Thread name; visible in debugger
 *  int workerThreadCount = 4;
 *  final AutoStopThreadPool pool = new AutoStopThreadPool(name, workerThreadCount);
 *  pool.addTask(new Runnable() { ....; if (..) pool.addTask(...); });
 *  pool.execute();   // Blocks until all tasks completed; rethrows any exception thrown by a task</pre></p>
 *
 * <p>The threads are only created when the <code>execute</code> method is called: if they were started when the object was instanciated
 * then they would stop again immediately due to there being no tasks at that point in time.</p>
 *
 * <p>There is no way to specify a maximum size of the queue of tasks.
 * The queue of tasks to be executed is necessarily unbounded:
 * if it had a fixed size, and adding a new task would block until space was available, and the entity adding the task
 * was itself a task, deadlock could occur. If there were <i>n</i> threads, the queue was full, and all <i>n</i> threads tried to add new tasks,
 * then they would all block waiting for space to be available in the queue, yet space would never become available as all tasks
 * would be waiting.</p>
 *
 * <h3>Difference from JVM Thread Pools</h3>
 *
 * <p>Thread pools such as {@link ExecutorService} offered by the JVM have two phases:
 * <ol>
 * <li>Firstly tasks are added and can be executed,</li>
 * <li>Secondly the pool is "shut down" and one can wait until all tasks have been completed;
 * no more tasks may be added</li>
 * </ol>
 * However, if tasks may schedule further tasks for execution, then tasks can necessarily only execute in the first phase.
 * But waiting for all tasks to be completed can only occur in the second phase.
 * Therefore it is not possible to wait until all tasks have been completed, if tasks can schedule other tasks.
 *
 * <p>The object is named after cassette players which boast the feature "auto stop", meaning they stop automatically
 * once the cassette is finished.</p>
 *
 * @author This source is copyright <a href="http://www.databasesandlife.com">Adrian Smith</a> and licensed under the LGPL 3.
 * @version $Revision: 2249 $
 */
public class AutoStopThreadPool {

    protected String threadNamePrefix;
    protected int workerThreadCount;

    /**
     * Initial number of permits is (1-threadCount); each thread calls release() on completion; exeute() waits for acquire()
     */
    protected Semaphore completedSemaphore;

    /**
     * Tasks which have not been started yet
     */
    protected Queue<Runnable> notStartedTaskQueue = new LinkedList<Runnable>();

    /**
     * Counts the number of tasks which are either in notStartedTaskQueue OR are currently being executed
     */
    protected int notCompletedTaskCount = 0;

    /**
     * If not null, then a task has thrown an exception which should be rethrown by execute()
     */
    protected RuntimeException thrown = null;

    private CompletionListener completionListener;

    protected class Worker implements Runnable {
        public void run() {
            try {
                while (true) {
                    Runnable nextTask;
                    synchronized (AutoStopThreadPool.this) {
                        nextTask = notStartedTaskQueue.poll();  // null if queue is empty
                    }

                    // If queue is empty then we can't start a new task;
                    //   If no task is running then no new tasks will ever be produced so shut down
                    //   If some tasks are sill running, they might produce new tasks so just wait and try again
                    if (nextTask == null) {
                        synchronized (AutoStopThreadPool.this) {
                            if (notCompletedTaskCount == 0) break;
                        }

                        try {
                            Thread.sleep(100);
                        } // 0.1 s
                        catch (InterruptedException e) {
                        }
                        continue;
                    }

                    try {
                        nextTask.run();
                    } catch (RuntimeException e) {
                        thrown = e;
                    } catch (Exception e) {
                        thrown = new RuntimeException(e);
                    } finally {
                        synchronized (AutoStopThreadPool.this) {
                            notCompletedTaskCount--;
                        }
                    }
                }
            } catch (Throwable e) {
                thrown = new RuntimeException(e);
            } // Throwable ends while loop, e.g. out of memory
            finally {
                completedSemaphore.release();
            }
        }
    }

    public AutoStopThreadPool(String name, int workerThreadCount) {
        this.threadNamePrefix = name;
        this.workerThreadCount = workerThreadCount;
    }

    public AutoStopThreadPool(String name, int workerThreadCount, CompletionListener listener) {
        this.threadNamePrefix = name;
        this.workerThreadCount = workerThreadCount;
        this.completionListener = listener;
    }

    /**
     * Add this runnable to the queue of tasks to be executed.
     * It will only start executing when the <code>execute</code> method is called.
     */
    public synchronized void addTask(Runnable r) {
        notStartedTaskQueue.add(r);
        notCompletedTaskCount++;
    }

    /**
     * Starts the threads and execute all tasks, returning once they have all been completed.
     *
     * @throws RuntimeException if a task has thrown an exception.
     *                          (Regrettably checked exceptions cannot be safely or usefully thrown,
     *                          <a href="http://www.databasesandlife.com/checked-exceptions-and-java-callables/" target=blank>more info</a>)
     */
    public void execute() {
        completedSemaphore = new Semaphore(1 - workerThreadCount);

        for (int i = 0; i < workerThreadCount; i++)
            new Thread(new Worker(), threadNamePrefix + "-" + i).start();

        completedSemaphore.acquireUninterruptibly();

        if (completionListener != null) {
            completionListener.onAllTaskComplete();
        }
        if (thrown != null) throw thrown;
    }

    public interface CompletionListener {
        void onAllTaskComplete();
    }
}
