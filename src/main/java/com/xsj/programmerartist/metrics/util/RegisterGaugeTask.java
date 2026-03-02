package com.xsj.programmerartist.metrics.util;

import com.xsj.programmerartist.metrics.bean.Tag;
import com.xsj.programmerartist.metrics.process.MetricsProcess;
import com.xsj.programmerartist.metrics.process.impl.PrometheusMetricsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测量值定时任务
 *
 * 如果使用了注册测量值，则会启动一个线程，否则不启动线程不开启任务
 *
 * @Author zyb
 * @Date 2024/11/27
 **/
public class RegisterGaugeTask {
    private static final Logger logger = LoggerFactory.getLogger(RegisterGaugeTask.class);

    private static final Map<String, Callable<Long>> metricsName2GaugeCall   = new HashMap<>();
    private static final List<Callable<Map<String, Long>>> callableBatchList = new ArrayList<>();
    private static final MetricsProcess innerMetrics                           = PrometheusMetricsProcessor.getInstance();

    private static final AtomicInteger errorTimes = new AtomicInteger(0);
    private static final int MAX_ERROR_LOG_TIMES  = 10;
    private static volatile boolean hasStart      = false;
    private static final int DELAY                = 3;
    private static final int PERIOD               = 1;

    private static ScheduledExecutorService schedulePool;

    /**
     * 已加锁
     *
     * @param name
     * @param callable
     * @param callableBatch
     */
    public static synchronized void add(String name, Callable<Long> callable, Callable<Map<String, Long>> callableBatch) {
        if(MetricsUtil.isNotBlank(name) && null!=callable) {
            metricsName2GaugeCall.put(name, callable);
        }else if(null != callableBatch){
            callableBatchList.add(callableBatch);
        }else {
            return;
        }

        if(!hasStart) {
            startTask();
            hasStart = true;
        }
    }

    /**
     *
     */
    private static void startTask() {
        schedulePool = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            private int count;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r,  "Metrics-RegisterGauge-" + (++count));
                t.setDaemon(true);
                return t;
            }
        });

        schedulePool.scheduleAtFixedRate(() -> {
            if(!ControlConfig.globalSwitch) { return; }

            // 单个的
            if(metricsName2GaugeCall.size() > 0) {
                metricsName2GaugeCall.entrySet().forEach(name2Call -> {
                    try {
                        Long val = name2Call.getValue().call();
                        if(null != val) {
                            gauge(name2Call.getKey(), val.longValue(), null);
                        }
                    } catch (Throwable e) {
                        if(errorTimes.get() < MAX_ERROR_LOG_TIMES) {
                            logger.error("[metrics][error] RegisterGauge task: error; times=" + errorTimes.incrementAndGet() + ", msg=" + e.getMessage(), e);
                        }
                    }
                });
            }

            // 成批的
            if(callableBatchList.size() > 0) {
                callableBatchList.forEach(callBatch -> {
                    try {
                        Map<String, Long> metricsName2val = callBatch.call();
                        if(null!=metricsName2val && metricsName2val.size()>0) {
                            metricsName2val.entrySet().forEach(name2Val -> {
                                if(null != name2Val.getValue()) { gauge(name2Val.getKey(), name2Val.getValue().longValue(), null); }
                            });
                        }
                    } catch (Throwable e) {
                        if(errorTimes.get() < MAX_ERROR_LOG_TIMES) {
                            logger.error("[metrics][error] RegisterGauge task: error; times=" + errorTimes.incrementAndGet() + ", msg=" + e.getMessage(), e);
                        }
                    }
                });
            }


        }, DELAY, PERIOD, TimeUnit.SECONDS);

    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     */
    private static void gauge(String metrics, long value, Tag tag) {
        innerMetrics.gauge(metrics, value, tag);
    }


}
