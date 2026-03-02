package com.xsj.programmerartist.metrics.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * latency耗时，监控打点注解（方法级别）
 *
 * @author zyb
 * @date 2024/11/29
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MetricLatency {

    /**
     * 可选参数，不填写则默认取 类名_方法名，且做全小写处理
     * 指标名：比如 video_event_service_user_like
     *
     * @return
     */
    String name() default "";

    /**
     * 可选参数，不填写则默认为控
     * 自定义监控tag：比如 {"tag1", "value1", "tag2", "value2" ...}
     * @return
     */
    String[] tags() default {};
    
}
