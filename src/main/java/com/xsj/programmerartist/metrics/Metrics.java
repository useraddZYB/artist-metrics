package com.xsj.programmerartist.metrics;

import com.xsj.programmerartist.metrics.service.SimpleMetrics;
import com.xsj.programmerartist.metrics.service.TagMetrics;
import com.xsj.programmerartist.metrics.bean.Tag;
import com.xsj.programmerartist.metrics.util.RegisterGaugeTask;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 业务代码接入类
 *
 * @author zyb
 * @date 2024/11/27
 */
public class Metrics {

    /**
     * private
     */
    private Metrics() {

    }

    /**
     * 简单API
     *
     * @return
     */
    public static SimpleMetrics simple() {
        return SimpleMetrics.getInstance();
    }

    /**
     * 简单API，带统一的指标名前缀
     *
     * @param metricsNamePre
     * @return
     */
    public static SimpleMetrics simple(String metricsNamePre) {
        return new SimpleMetrics(metricsNamePre);
    }

    /**
     * 自定义TAG的API
     *
     * @return
     */
    public static TagMetrics tag() {
        return TagMetrics.getInstance();
    }

    /**
     * 自定义TAG的API，带统一的TAG
     *
     * @param tag
     * @return
     */
    public static TagMetrics tag(Tag tag) {
        return new TagMetrics("", tag);
    }

    /**
     * 自定义TAG的API，带统一的指标名前缀
     *
     * @param metricsNamePre
     * @return
     */
    public static TagMetrics tag(String metricsNamePre) {
        return new TagMetrics(metricsNamePre, null);
    }

    /**
     * 自定义TAG的API，带统一的指标名前缀 和 统一的TAG
     *
     * @param metricsNamePre
     * @param tag
     * @return
     */
    public static TagMetrics tag(String metricsNamePre, Tag tag) {
        return new TagMetrics(metricsNamePre, tag);
    }

    /**
     * 注册测量值，比如线程池线程大小、队列大小，缓存大小等
     *
     * @param metricsName
     * @param callable
     */
    public static void registerGauge(String metricsName, Callable<Long> callable) {
        RegisterGaugeTask.add(metricsName, callable, null);
    }

    /**
     * 注册一批测量值，比如redis集群的多个状态指标
     *
     * @param callableBatch Map<String, Long> 指标名 到 指标值的映射
     */
    public static void registerGaugeMany(Callable<Map<String, Long>> callableBatch) {
        RegisterGaugeTask.add(null, null, callableBatch);
    }

}
