package com.xsj.programmerartist.metrics.process;


import com.xsj.programmerartist.metrics.bean.Tag;

/**
 * @Author zyb
 * @Date 2024/11/27
 **/
public interface MetricsProcess {

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    MetricsProcess count(String metrics, long value, Tag tag);

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    MetricsProcess latency(String metrics, long value, Tag tag);

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    MetricsProcess mean(String metrics, long value, Tag tag);

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    MetricsProcess rate(String metrics, int value, Tag tag);

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    MetricsProcess set(String metrics, String value, Tag tag);

    /**
     *
     * @param metrics
     * @param value
     * @param tag
     * @return
     */
    MetricsProcess gauge(String metrics, long value, Tag tag);

}
