package com.xsj.programmerartist.metrics.process.impl;


import com.xsj.programmerartist.metrics.bean.Tag;
import com.xsj.programmerartist.metrics.constant.Const;
import com.xsj.programmerartist.metrics.MetricsConfig;
import com.xsj.programmerartist.metrics.process.MetricsProcess;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author zyb
 * @Date 2024/11/27
 **/
public class PrometheusMetricsProcessor implements MetricsProcess {

    /**
     *
     */
    private MeterRegistry meterRegistry;

    /**
     * single
     */
    private static final PrometheusMetricsProcessor prometheusInnerMetrics = new PrometheusMetricsProcessor();
    private PrometheusMetricsProcessor() {}
    public static PrometheusMetricsProcessor getInstance() { return prometheusInnerMetrics; }


    /**
     * 统计容器 缓存
     */
    private static volatile Map<String, Counter> counterCacheByMetricsAndTag             = new HashMap<>();
    private static volatile Map<String, DistributionSummary> latencyCacheByMetricsAndTag = new HashMap<>();
    private static volatile Map<String, DistributionSummary> meanCacheByMetricsAndTag    = new HashMap<>();
    private static volatile Map<String, DistributionSummary> ratioCacheByMetricsAndTag   = new HashMap<>();
    private static volatile Map<String, AtomicLong> gaugeCacheByMetricsAndTag            = new HashMap<>();

    private static final Object COUNTER_CACHE_LOCK = new Object();
    private static final Object GAUGE_CACHE_LOCK   = new Object();
    private static final Object LATENCY_CACHE_LOCK = new Object();
    private static final Object MEAN_CACHE_LOCK    = new Object();
    private static final Object RATIO_CACHE_LOCK   = new Object();



    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    @Override
    public MetricsProcess count(String metrics, long value, Tag tag) {
        Counter counter = this.getCacheOrPut(metrics, tag, counterCacheByMetricsAndTag, Const.MetricsType.COUNTER);
        counter.increment(value);
        return this;
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    @Override
    public MetricsProcess latency(String metrics, long value, Tag tag) {
        DistributionSummary summary = this.getCacheOrPut(metrics, tag, latencyCacheByMetricsAndTag, Const.MetricsType.LATENCY);
        summary.record(value);
        return this;
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    @Override
    public MetricsProcess mean(String metrics, long value, Tag tag) {
        DistributionSummary summary = this.getCacheOrPut(metrics, tag, meanCacheByMetricsAndTag, Const.MetricsType.MEAN);
        summary.record(value);
        return this;
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    @Override
    public MetricsProcess rate(String metrics, int value, Tag tag) {
        DistributionSummary summary = this.getCacheOrPut(metrics, tag, ratioCacheByMetricsAndTag, Const.MetricsType.RATIO);
        summary.record(value);
        return this;
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    @Override
    public MetricsProcess set(String metrics, String value, Tag tag) {
        throw new UnsupportedOperationException("prometheus is not support [set]");
    }

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    @Override
    public MetricsProcess gauge(String metrics, long value, Tag tag) {
        AtomicLong atomicLong = this.getCacheOrPut(metrics, tag, gaugeCacheByMetricsAndTag, Const.MetricsType.GAUGE);
        atomicLong.set(value);
        return this;
    }


    /**
     *
     * @param meterRegistry
     */
    public void _setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }


    /**
     *
     * @param metrics
     * @param tag
     * @param tCacheByMetricsAndTag
     * @param metricsType
     * @return
     * @param <T>
     */
    private <T> T getCacheOrPut(String metrics, Tag tag, Map<String, T> tCacheByMetricsAndTag, Const.MetricsType metricsType) {
        final String metricsWithTag = metrics + (null!=tag ? ":"+tag : "");

        T countOrGaugeOrSummary = tCacheByMetricsAndTag.get(metricsWithTag);
        if(null != countOrGaugeOrSummary) {
            return countOrGaugeOrSummary;
        }else {
            Object objLock = null;
            switch (metricsType) {
                case COUNTER:
                    objLock = COUNTER_CACHE_LOCK;
                    break;
                case LATENCY:
                    objLock = LATENCY_CACHE_LOCK;
                    break;
                case MEAN:
                    objLock = MEAN_CACHE_LOCK;
                    break;
                case RATIO:
                    objLock = RATIO_CACHE_LOCK;
                    break;
                case GAUGE:
                    objLock = GAUGE_CACHE_LOCK;
                    break;
            }

            synchronized (objLock) {
                countOrGaugeOrSummary =  tCacheByMetricsAndTag.get(metricsWithTag);
                if(null != countOrGaugeOrSummary) {
                    return countOrGaugeOrSummary;
                }

                List<io.micrometer.core.instrument.Tag> prometheusTags = this.toPrometheusTags(tag, MetricsConfig.getInstance().getGlobalTag());
                switch (metricsType) {
                    case COUNTER:
                        countOrGaugeOrSummary = (T) Counter.builder(metrics).tags(prometheusTags).register(meterRegistry);
                        break;
                    case LATENCY:
                        countOrGaugeOrSummary = (T) DistributionSummary.builder(metrics).tags(prometheusTags).publishPercentiles(0.5, 0.95, 0.99).register(meterRegistry);
                        break;
                    case MEAN:
                    case RATIO:
                        countOrGaugeOrSummary = (T) DistributionSummary.builder(metrics).tags(prometheusTags).register(meterRegistry);
                        break;
                    case GAUGE:
                        AtomicLong atomicLong = new AtomicLong();
                        Gauge.builder(metrics, atomicLong, AtomicLong::get).tags(prometheusTags).register(meterRegistry);
                        countOrGaugeOrSummary = (T) atomicLong;
                        break;
                }

                tCacheByMetricsAndTag.put(metricsWithTag, countOrGaugeOrSummary);
            }
        }

        return countOrGaugeOrSummary;
    }


    /**
     *
     * @param tag
     * @return
     */
    private List<io.micrometer.core.instrument.Tag> toPrometheusTags(Tag tag, Tag globalTag) {
        int size             = 0;
        if(null!=tag && null!=tag.getValue()) { size = tag.getValue().size(); }
        final int globalSize = globalTag.getValue().size();
        final int totalSize  = globalSize+size;

        List<io.micrometer.core.instrument.Tag> prometheusTags = new ArrayList<>(totalSize);

        if(globalSize > 0) {
            for(Tag.TagEntry tagEntry : globalTag.getValue()) {
                prometheusTags.add(io.micrometer.core.instrument.Tag.of(tagEntry.getName(), tagEntry.getValue().toString()));
            }
        }

        if(size > 0) {
            for(Tag.TagEntry tagEntry : tag.getValue()) {
                prometheusTags.add(io.micrometer.core.instrument.Tag.of(tagEntry.getName(), tagEntry.getValue().toString()));
            }
        }

        return prometheusTags;
    }




}
