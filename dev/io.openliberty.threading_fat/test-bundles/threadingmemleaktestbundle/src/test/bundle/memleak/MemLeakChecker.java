/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.bundle.memleak;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class MemLeakChecker {
    private static final int MAX = 10000;

    private ScheduledExecutorService scheduledExecutorService;

    @Reference
    protected void setScheduledExecutorService(ScheduledExecutorService ses) {
        scheduledExecutorService = ses;
    }

    protected void unsetScheduledExecutorService(ScheduledExecutorService ses) {
        scheduledExecutorService = null;
    }

    @Activate
    protected void activate() {
        runScheduleCancelTest();
        runScheduleExecuteTest();
        runScheduleCloseTest();
    }

    private void runScheduleCloseTest() {
        // We have to create a new executor to avoid causing problems for the server
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        if (executorService instanceof AutoCloseable) {
            // Java 19
            try {
                ((AutoCloseable) executorService).close();
            } catch (Exception e) {
                System.out.println("runScheduleCloseTest FAILED - " + e.getMessage());
            }
            if (!executorService.isShutdown() || !executorService.isTerminated()) {
                System.out.println("runScheduleCloseTest FAILED - Executor Service not terminated after close");
            }
            System.out.println("runScheduleCloseTest PASSED - Java 19");
        } else {
            System.out.println("runScheduleCloseTest PASSED - Java is below version 19");
        }
    }

    private void runScheduleCancelTest() {
        List<WeakReference<ScheduledFuture<?>>> list = new ArrayList<WeakReference<ScheduledFuture<?>>>(MAX);
        for (int i = 0; i < MAX; i++) {
            ScheduledFuture<?> schedFuture = scheduledExecutorService.schedule(new Task("CancelMe", i, null), 10, TimeUnit.MINUTES);
            list.add(new WeakReference<ScheduledFuture<?>>(schedFuture));
            if (!schedFuture.cancel(false)) {
                System.out.println("Failed to cancel " + schedFuture);
                return;
            }
        }

        int retVal = doGarbageCollectCheck(list);
        if (retVal >= 0) {
            System.out.println("runScheduleCancelTest FAILED - scheduledFuture[" + retVal + "] not GC'd");
        } else {
            System.out.println(scheduledExecutorService.toString() + " executed and canceled " + MAX + " tasks");
            System.out.println("runScheduleCancelTest PASSED");
        }
    }

    private void runScheduleExecuteTest() {
        // use a lower maximum since this test will actually be executing tasks
        final int MAX = MemLeakChecker.MAX / 100;

        final CountDownLatch latch = new CountDownLatch(MAX);
        List<WeakReference<ScheduledFuture<?>>> list = new ArrayList<WeakReference<ScheduledFuture<?>>>(MAX);
        for (int i = 0; i < MAX; i++) {
            ScheduledFuture<?> schedFuture = scheduledExecutorService.schedule(new Task("RunMe", i, latch), 100, TimeUnit.MICROSECONDS);
            list.add(new WeakReference<ScheduledFuture<?>>(schedFuture));
            schedFuture = null;
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int retVal = doGarbageCollectCheck(list);
        if (retVal >= 0) {
            System.out.println("runScheduleExecuteTest FAILED - scheduledFuture[" + retVal + "] not GC'd");
        } else {
            System.out.println("runScheduleExecuteTest PASSED");
        }
    }

    private int doGarbageCollectCheck(List<WeakReference<ScheduledFuture<?>>> weakRefList) {
        int index = 0;
        int listSize = weakRefList.size();
        for (int i = 0; i < 10; ++i) {
            System.out.println("MemLeakChecker.doGarbageCollectCheck calling system gc " + i);
            System.gc();
            // give the GC some time to actually do its thing
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (index = 0; index < listSize; index++) {
                WeakReference<ScheduledFuture<?>> weakRef = weakRefList.get(index);
                ScheduledFuture<?> schedFuture = weakRef.get();
                if (schedFuture != null) {
                    break;
                }
            }

            if (index == listSize) {
                index = -1;
                break;
            }
        }

        return index;
    }

    private static class Task implements Runnable {
        private final String name;
        private final int id;
        private final CountDownLatch latch;

        Task(String name, int id, CountDownLatch latch) {
            this.name = name;
            this.id = id;
            this.latch = latch;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            if (latch != null) {
                latch.countDown();
            }
            if (id % 1000 == 0)
                System.out.println(name + id + " executed");

        }
    }
}
