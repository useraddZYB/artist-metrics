package com.xsj.programmerartist.metrics.service;


import com.xsj.programmerartist.metrics.bean.Tag;
import com.xsj.programmerartist.metrics.constant.Const;
import com.xsj.programmerartist.metrics.util.ControlConfig;
import com.xsj.programmerartist.metrics.util.Watch;

import java.util.Random;

import static com.xsj.programmerartist.metrics.constant.Const.Protocol.*;


/**
 * @Author zyb
 * @Date 2024/11/27
 **/
public abstract class AbstractMetrics {
    protected static final Tag NULL_TAG = null;
    protected String metricsNamePre = "";

    /** 采样 */
    private static final int SAMPLE_MAX                    = 100;
    private static final ThreadLocal<Random> SAMPLE_RANDOM = ThreadLocal.withInitial(() -> new Random());


    /**
     *
     */
    public AbstractMetrics() {
    }

    /**
     *
     * @param metrics
     * @param tag
     */
    protected AbstractMetrics abQps(String metrics, Tag tag) {
        if(!acquireSwithPermit()) { return this; }
        String wholeMetrics = wrapMetrics(metrics) + METRICS_SUFFIX_QPS;

        if(!acquireSamplePermit(Const.MetricsType.COUNTER, wholeMetrics)
                || ControlConfig.globalQpsSamplePercent<=0) {
            return this;
        }

        // 是否命中自定义明细采样
        boolean hitName2Sample = ControlConfig.metricsName2SamplePercent.size()>0
                && ControlConfig.metricsName2SamplePercent.containsKey(wholeMetrics);
        if(hitName2Sample) {
            AsyncSend.getInstance().qps(wholeMetrics, tag);
        }else {
            // qps指标 加一层全局qps采样
            if(this.doAcquireSample(ControlConfig.globalQpsSamplePercent)) {
                AsyncSend.getInstance().qpsRecover(wholeMetrics, tag);
            }
        }

        return this;
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    AbstractMetrics abLatency(String metrics, long value, Tag tag) {
        if(!acquireSwithPermit()) { return this; }
        String wholeMetrics = wrapMetrics(metrics) + METRICS_SUFFIX_LATENCY;
        if(!acquireSamplePermit(Const.MetricsType.LATENCY, wholeMetrics)) { return this; }

        AsyncSend.getInstance().latency(wholeMetrics, value, tag);
        return this;
    }

    /**
     *
     * @param metrics
     * @param watch
     * @param tag
     * @return
     */
    AbstractMetrics abLatency(String metrics, Watch watch, Tag tag) {
        if(!acquireSwithPermit()) { return this; }

        long cost           = watch.costAndReStart();
        String wholeMetrics = wrapMetrics(metrics) + METRICS_SUFFIX_LATENCY;
        if(!acquireSamplePermit(Const.MetricsType.LATENCY, wholeMetrics)) { return this; }

        AsyncSend.getInstance().latency(wholeMetrics, cost, tag);
        return this;
    }

    /**
     *
     * @param metrics
     * @param tag
     * @return
     */
    AbstractMetrics abRatioYes(String metrics, Tag tag) {
        if(!acquireSwithPermit()) { return this; }
        String wholeMetrics = wrapMetrics(metrics) + METRICS_SUFFIX_RATIO;
        if(!acquireSamplePermit(Const.MetricsType.MEAN, wholeMetrics)) { return this; }

        AsyncSend.getInstance().ratioYes(wholeMetrics, tag);
        return this;
    }

    /**
     *
     * @param metrics
     * @param tag
     * @return
     */
    AbstractMetrics abRatioNo(String metrics, Tag tag) {
        if(!acquireSwithPermit()) { return this; }
        String wholeMetrics = wrapMetrics(metrics) + METRICS_SUFFIX_RATIO;
        if(!acquireSamplePermit(Const.MetricsType.MEAN, wholeMetrics)) { return this; }

        AsyncSend.getInstance().ratioNo(wholeMetrics, tag);
        return this;
    }

    /**
     *
     * @param metrics
     * @param yes
     * @param tag
     * @return
     */
    AbstractMetrics abRatio(String metrics, boolean yes, Tag tag) {
        if(!acquireSwithPermit()) { return this; }
        String wholeMetrics = wrapMetrics(metrics) + METRICS_SUFFIX_RATIO;
        if(!acquireSamplePermit(Const.MetricsType.MEAN, wholeMetrics)) { return this; }

        AsyncSend.getInstance().ratio(wholeMetrics, yes, tag);
        return this;
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    AbstractMetrics abGauge(String metrics, long value, Tag tag) {
        if(!acquireSwithPermit()) { return this; }
        String wholeMetrics = wrapMetrics(metrics);
        if(!acquireSamplePermit(Const.MetricsType.GAUGE, wholeMetrics)) { return this; }

        AsyncSend.getInstance().gauge(wholeMetrics, value, tag);
        return this;
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    AbstractMetrics abCount(String metrics, long value, Tag tag) {
        if(!acquireSwithPermit()) { return this; }
        String wholeMetrics = wrapMetrics(metrics);
        if(!acquireSamplePermit(Const.MetricsType.COUNTER, wholeMetrics)) { return this; }

        AsyncSend.getInstance().count(wholeMetrics, value, tag);
        return this;
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    AbstractMetrics abMean(String metrics, long value, Tag tag) {
        if(!acquireSwithPermit()) { return this; }
        String wholeMetrics = wrapMetrics(metrics);
        if(!acquireSamplePermit(Const.MetricsType.MEAN, wholeMetrics)) { return this; }

        AsyncSend.getInstance().mean(wholeMetrics, value, tag);
        return this;
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    AbstractMetrics abSet(String metrics, String value, Tag tag) {
        if(!acquireSwithPermit()) { return this; }
        String wholeMetrics = wrapMetrics(metrics);
        if(!acquireSamplePermit(Const.MetricsType.SET, wholeMetrics)) { return this; }

        AsyncSend.getInstance().set(wholeMetrics, value, tag);
        return this;
    }







    // ============================================= tool ===================================================


    /**
     *
     * @param metrics
     * @return
     */
    private String wrapMetrics(String metrics) {
        if(null==metricsNamePre || "".equals(metricsNamePre)) {
            return metrics;
        }

        metrics = null!=metrics ? metrics : "";
        return metricsNamePre + (!"".equals(metrics) ? METRICS_SEP + metrics : metrics);
    }


    /**
     *
     * @return
     */
    private boolean acquireSwithPermit() {
        return ControlConfig.globalSwitch;
    }
    /**
     * 自定义采样优先级 高于 全局采样
     *
     * @param type
     * @param wholeMetrics
     * @return
     */
    private boolean acquireSamplePermit(Const.MetricsType type, String wholeMetrics) {
        // 自定义采样
        if(ControlConfig.metricsName2SamplePercent.size() > 0) {
            Integer mSample = ControlConfig.metricsName2SamplePercent.get(wholeMetrics);
            if(null != mSample) {
                return doAcquireSample(mSample);
            }
        }

        // 全局采样
        if(ControlConfig.GLOBAL_SAMPLE_M_TYPES.contains(type)) {
            return doAcquireSample(ControlConfig.globalSamplePercent);
        }

        return true;
    }
    /**
     *
     * @param samplePercent
     * @return
     */
    private boolean doAcquireSample(int samplePercent) {
        if(samplePercent >= SAMPLE_MAX) {
            return true;
        }else if(samplePercent <= 0) {
            return false;
        }else {
            return SAMPLE_RANDOM.get().nextInt(SAMPLE_MAX) < samplePercent;
        }
    }

}
