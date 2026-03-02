package com.xsj.programmerartist.metrics.service;


import com.xsj.programmerartist.metrics.util.Watch;

/**
 * 简单打点：无自定义tag
 *
 * @Author zyb
 * @Date 2024/11/27
 **/
public class SimpleMetrics extends AbstractMetrics {

    /**
     * single
     */
    private static final SimpleMetrics simpleMetrics = new SimpleMetrics();
    private SimpleMetrics() { }
    public static SimpleMetrics getInstance() {
        return simpleMetrics;
    }

    /**
     *
     * @param metricsNamePre
     */
    public SimpleMetrics(String metricsNamePre) {
        if(null != metricsNamePre) {
            this.metricsNamePre = metricsNamePre.trim();
        }
    }


    /**
     * "_qps"指标名后缀不用写，底层自动追加
     *
     * @param metrics
     * @return
     */
    public SimpleMetrics qps(String metrics) {
        super.abQps(metrics, NULL_TAG);
        return this;
    }

    /**
     * 求平均值、p99、p90、p50
     * "_latency"指标名后缀不用写，底层自动追加
     *
     * @param metrics
     * @param value
     * @return
     */
    public SimpleMetrics latency(String metrics, long value) {
        super.abLatency(metrics, value, NULL_TAG);
        return this;
    }

    /**
     * 求平均值、p99、p90、p50
     * "_latency"指标名后缀不用写，底层自动追加
     *
     * @param metrics
     * @param watch   一个计数器工具，传入后底层自动计算耗时
     * @return
     */
    public SimpleMetrics latency(String metrics, Watch watch) {
        super.abLatency(metrics, watch, NULL_TAG);
        return this;
    }

    /**
     * 事件发生了，比如：异常率、缓存命中率
     * "_ratio"指标名后缀不用写，底层自动追加
     *
     * 注意：此函数需要与ratioNo搭配使用
     *
     * @param metrics
     * @return
     */
    public SimpleMetrics ratioYes(String metrics) {
        super.abRatioYes(metrics, NULL_TAG);
        return this;
    }

    /**
     * 事件没有发生，比如：异常率、缓存命中率
     * "_ratio"指标名后缀不用写，底层自动追加
     *
     * 注意：此函数需要与ratioYes搭配使用
     *
     * @param metrics
     * @return
     */
    public SimpleMetrics ratioNo(String metrics) {
        super.abRatioNo(metrics, NULL_TAG);
        return this;
    }

    /**
     * 事件是否发生了，比如：异常率、缓存命中率
     * "_ratio"指标名后缀不用写，底层自动追加
     *
     * @param metrics
     * @param yes     事件发生 ? true : false
     * @return
     */
    public SimpleMetrics ratio(String metrics, boolean yes) {
        super.abRatio(metrics, yes, NULL_TAG);
        return this;
    }

    /**
     * 测量值，通常用做 线程池线程大小、队列大小，缓存大小等
     * 每秒一个打点，建议使用 @see Metrics.registerGauge
     *
     * @param metrics
     * @param value
     * @return
     */
    public SimpleMetrics gauge(String metrics, long value) {
        super.abGauge(metrics, value, NULL_TAG);
        return this;
    }

    /**
     * 求和
     *
     * @param metrics
     * @param value
     * @return
     */
    public SimpleMetrics count(String metrics, long value) {
        super.abCount(metrics, value, NULL_TAG);
        return this;
    }

    /**
     * 求平均值等
     *
     * @param metrics
     * @param value
     * @return
     */
    public SimpleMetrics mean(String metrics, long value) {
        super.abMean(metrics, value, NULL_TAG);
        return this;
    }

    /**
     * 求不重复的字符串值value的数量，比如每秒在线用户数
     *
     * @param metrics
     * @param value   比如 UID
     * @return
     */
    public SimpleMetrics set(String metrics, String value) {
        super.abSet(metrics, value, NULL_TAG);
        return this;
    }
}
