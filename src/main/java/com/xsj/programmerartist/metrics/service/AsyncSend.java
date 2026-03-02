package com.xsj.programmerartist.metrics.service;


import com.xsj.programmerartist.metrics.bean.Tag;
import com.xsj.programmerartist.metrics.util.ControlConfig;
import com.xsj.programmerartist.metrics.MetricsConfig;
import com.xsj.programmerartist.metrics.process.MetricsProcess;
import com.xsj.programmerartist.metrics.process.impl.PrometheusMetricsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.xsj.programmerartist.metrics.constant.Const.API.*;

/**
 * @Author zyb
 * @Date 2024/11/27
 **/
public class AsyncSend {
    private static final Logger logger = LoggerFactory.getLogger(AsyncSend.class);

    private static final AtomicInteger errorTimes    = new AtomicInteger(0);
    private static final int MAX_ERROR_LOG_TIMES     = 20;
    private static final AtomicInteger outErrorTimes = new AtomicInteger(0);
    private static final int MAX_OUT_ERROR_LOG_TIMES = 20;
    private static int qpsVal = QPS_VAL;

    private static ExecutorService THREAD_POOL;
    private static volatile boolean BOOTSTRAP_START = false;

    private static MetricsProcess metricsProcess;

    /**
     * single
     */
    private static final AsyncSend asyncSend = new AsyncSend();
    private AsyncSend() { }
    public static AsyncSend getInstance() { return asyncSend; }


    /**
     *
     */
    public static void init() {
        if(null != MetricsConfig.getInstance().getProcessType()) {
            switch (MetricsConfig.getInstance().getProcessType()) {
                case PROMETHEUS:
                    metricsProcess = PrometheusMetricsProcessor.getInstance();
                    break;
            }
        }

        THREAD_POOL  = new ThreadPoolExecutor(
                10, 20, 300L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50000),
                new ThreadFactory() {
                    private int count;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r,  "pool-xsj-metrics-" + (++count));
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.AbortPolicy()
        );

        logger.info("[metrics][info] metricsProcess={}", metricsProcess);
    }


    /**
     *
     * @param metrics
     * @param tag
     * @return
     */
    public void qps(String metrics, Tag tag) {
        this.asyncSendMetrics(() -> metricsProcess.count(metrics, QPS_VAL, tag));
    }

    /**
     * globalQpsSampleRecover 值默认为1，如果设置了全局QPS采样率，则该值会变
     * 举例：如果采样50，则globalQpsSampleRecover == 2
     *
     * @param metrics
     * @param tag
     */
    public void qpsRecover(String metrics, Tag tag) {
        this.asyncSendMetrics(() -> metricsProcess.count(metrics, qpsVal, tag));
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     */
    public void latency(String metrics, long value, Tag tag) {
        this.asyncSendMetrics(() -> metricsProcess.latency(metrics, value, tag));
    }

    /**
     *
     * @param metrics
     * @param tag
     */
    public void ratioYes(String metrics, Tag tag) {
        this.asyncSendMetrics(() -> ratio(metrics, true, tag));
    }

    /**
     *
     * @param metrics
     * @param tag
     */
    public void ratioNo(String metrics, Tag tag) {
        this.asyncSendMetrics(() -> ratio(metrics, false, tag));
    }

    /**
     *
     * @param metrics
     * @param yes
     * @param tag
     */
    public void ratio(String metrics, boolean yes, Tag tag) {
        this.asyncSendMetrics(() -> metricsProcess.rate(metrics, yes ? RATE_YES_VAL : RATE_NO_VAL, tag));
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     */
    public void gauge(String metrics, long value, Tag tag) {
        this.asyncSendMetrics(() -> metricsProcess.gauge(metrics, value, tag));
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     */
    public void count(String metrics, long value, Tag tag) {
        this.asyncSendMetrics(() -> metricsProcess.count(metrics, value, tag));
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     */
    public void mean(String metrics, long value, Tag tag) {
        this.asyncSendMetrics(() -> metricsProcess.mean(metrics, value, tag));
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     */
    public void set(String metrics, String value, Tag tag) {
        this.asyncSendMetrics(() -> metricsProcess.set(metrics, value, tag));
    }


    /**
     *
     * @param newVal
     */
    public static void _changeQpsVal(int newVal) {
        qpsVal = newVal;
    }
    public static int getQpsVal() {
        return qpsVal;
    }

    public static void setBootstrapStart(boolean bootstrapStart) {
        BOOTSTRAP_START = bootstrapStart;
    }

    /**
     *
     * @param runnable
     */
    public void asyncSendMetrics(Runnable runnable) {
        if(!BOOTSTRAP_START || !ControlConfig.globalSwitch) return;

        try {
            THREAD_POOL.execute(() -> {
                try {
                    runnable.run();
                } catch (Throwable e) {
                    if(errorTimes.get() < MAX_ERROR_LOG_TIMES) {
                        logger.error("[metrics][error] FinalMetrics.asyncProduce(): error; times=" + errorTimes.incrementAndGet() + ", msg=" + e.getMessage(), e);
                    }
                }
            });
        } catch (Throwable e) {
            if(outErrorTimes.get() < MAX_OUT_ERROR_LOG_TIMES) {
                logger.error("[metrics][error] FinalMetrics.asyncProduce(): error" + outErrorTimes.incrementAndGet() + ", msg=" + e.getMessage(), e);
            }
        }
    }

}
