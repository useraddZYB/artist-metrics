package com.xsj.programmerartist.metrics.service;


import com.xsj.programmerartist.metrics.bean.Tag;
import com.xsj.programmerartist.metrics.util.Watch;

/**
 * @Author zyb
 * @Date 2024/11/27
 **/
public class TagMetrics extends AbstractMetrics {

    /**
     * single
     */
    private static final TagMetrics tagMetrics = new TagMetrics();
    private TagMetrics() { }
    public static TagMetrics getInstance() {
        return tagMetrics;
    }

    private Tag tag;

    /**
     *
     * @param metricsNamePre
     * @param tag            统一的tag，如果后续调用了含有tag参数的打点API，则会被覆盖掉，以打点API传入的为准
     */
    public TagMetrics(String metricsNamePre, Tag tag) {
        if(null != metricsNamePre) {
            this.metricsNamePre = metricsNamePre.trim();
        }
        if(null != tag) {
            this.tag = tag;
        }
    }

    /**
     * "_qps"指标名后缀不用写，底层自动追加
     *
     * @param metrics
     * @param tag     会覆盖构造函数里传的tag
     * @return
     */
    public TagMetrics qps(String metrics, Tag tag) {
        super.abQps(metrics, tag);
        return this;
    }

    /**
     * "_qps"指标名后缀不用写，底层自动追加
     *
     * @param metrics
     * @return
     */
    public TagMetrics qps(String metrics) {
        super.abQps(metrics, this.tag);
        return this;
    }

    /**
     * "_qps"指标名后缀不用写，底层自动追加
     *
     * @return
     */
    public TagMetrics qps() {
        super.abQps("", this.tag);
        return this;
    }

    /**
     * 求平均值、p99、p90、p50
     * "_latency"指标名后缀不用写，底层自动追加
     *
     * @param metrics
     * @param value
     * @param tag     会覆盖构造函数里传的tag
     */
    public TagMetrics latency(String metrics, long value, Tag tag) {
        super.abLatency(metrics, value, tag);
        return this;
    }

    /**
     * 求平均值、p99、p90、p50
     * "_latency"指标名后缀不用写，底层自动追加
     *
     * @param metrics
     * @param watch   一个计数器工具，传入后底层自动计算耗时
     * @param tag     会覆盖构造函数里传的tag
     */
    public TagMetrics latency(String metrics, Watch watch, Tag tag) {
        super.abLatency(metrics, watch, tag);
        return this;
    }

    /**
     * 求平均值、p99、p90、p50
     * "_latency"指标名后缀不用写，底层自动追加
     *
     * @param metrics
     * @param value
     */
    public TagMetrics latency(String metrics, long value) {
        super.abLatency(metrics, value, this.tag);
        return this;
    }

    /**
     * 求平均值、p99、p90、p50
     * "_latency"指标名后缀不用写，底层自动追加
     *
     * @param metrics
     * @param watch   一个计数器工具，传入后底层自动计算耗时
     */
    public TagMetrics latency(String metrics, Watch watch) {
        super.abLatency(metrics, watch, this.tag);
        return this;
    }

    /**
     * 求平均值、p99、p90、p50
     * "_latency"指标名后缀不用写，底层自动追加
     *
     * @param value
     */
    public TagMetrics latency(long value) {
        super.abLatency("", value, this.tag);
        return this;
    }

    /**
     * 事件发生了，比如：异常率、缓存命中率
     * "_ratio"指标名后缀不用写，底层自动追加
     *
     * 注意：此函数需要与ratioNo搭配使用
     *
     * @param metrics
     * @param tag     会覆盖构造函数里传的tag
     */
    public TagMetrics ratioYes(String metrics, Tag tag) {
        this.abRatioYes(metrics, tag);
        return this;
    }

    /**
     *
     * @param metrics
     */
    public TagMetrics ratioYes(String metrics) {
        this.abRatioYes(metrics, tag);
        return this;
    }

    /**
     * 事件没有发生，比如：异常率、缓存命中率
     * "_ratio"指标名后缀不用写，底层自动追加
     *
     * 注意：此函数需要与ratioYes搭配使用
     *
     * @param metrics
     * @param tag     会覆盖构造函数里传的tag
     */
    public TagMetrics ratioNo(String metrics, Tag tag) {
        this.abRatioNo(metrics, tag);
        return this;
    }

    /**
     * 事件没有发生，比如：异常率、缓存命中率
     * "_ratio"指标名后缀不用写，底层自动追加
     *
     * 注意：此函数需要与ratioYes搭配使用
     *
     * @param metrics
     */
    public TagMetrics ratioNo(String metrics) {
        this.abRatioNo(metrics, this.tag);
        return this;
    }

    /**
     * 事件是否发生了，比如：异常率、缓存命中率
     * "_ratio"指标名后缀不用写，底层自动追加
     *
     * @param metrics
     * @param yes
     * @param tag     会覆盖构造函数里传的tag
     */
    public TagMetrics ratio(String metrics, boolean yes, Tag tag) {
        this.abRatio(metrics, yes, tag);
        return this;
    }

    /**
     * 事件是否发生了，比如：异常率、缓存命中率
     * "_ratio"指标名后缀不用写，底层自动追加
     *
     * @param metrics
     * @param yes
     */
    public TagMetrics ratio(String metrics, boolean yes) {
        this.abRatio(metrics, yes, this.tag);
        return this;
    }

    /**
     * 测量值，通常用做 线程池线程大小、队列大小，缓存大小等
     * 每秒一个打点，建议使用 @see Metrics.registerGauge
     *
     * @param metrics
     * @param value
     * @param tag     会覆盖构造函数里传的tag
     */
    public TagMetrics gauge(String metrics, long value, Tag tag) {
        this.abGauge(metrics, value, tag);
        return this;
    }

    /**
     * 测量值，通常用做 线程池线程大小、队列大小，缓存大小等
     * 每秒一个打点，建议使用 @see Metrics.registerGauge
     *
     * @param metrics
     * @param value
     */
    public TagMetrics gauge(String metrics, long value) {
        this.abGauge(metrics, value, this.tag);
        return this;
    }

    /**
     * 求和
     *
     * @param metrics
     * @param value
     * @param tag     会覆盖构造函数里传的tag
     */
    public TagMetrics count(String metrics, long value, Tag tag) {
        this.abCount(metrics, value, tag);
        return this;
    }

    /**
     * 求和
     *
     * @param metrics
     * @param value
     */
    public TagMetrics count(String metrics, long value) {
        this.abCount(metrics, value, this.tag);
        return this;
    }

    /**
     * 求平均值等
     *
     * @param metrics
     * @param value
     * @param tag     会覆盖构造函数里传的tag
     */
    public TagMetrics mean(String metrics, long value, Tag tag) {
        this.abMean(metrics, value, tag);
        return this;
    }

    /**
     * 求平均值等
     *
     * @param metrics
     * @param value
     */
    public TagMetrics mean(String metrics, long value) {
        this.abMean(metrics, value, this.tag);
        return this;
    }

    /**
     * 求不重复的字符串值value的数量，比如每秒在线用户数
     *
     * @param metrics
     * @param value   比如 UID
     * @param tag     会覆盖构造函数里传的tag
     */
    public TagMetrics set(String metrics, String value, Tag tag) {
        this.abSet(metrics, value, tag);
        return this;
    }

    /**
     * 求不重复的字符串值value的数量，比如每秒在线用户数
     *
     * @param metrics
     * @param value   比如 UID
     */
    public TagMetrics set(String metrics, String value) {
        this.abSet(metrics, value, this.tag);
        return this;
    }
}
