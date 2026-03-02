package com.xsj.programmerartist.metrics.annotation.aop;

import com.xsj.programmerartist.metrics.Metrics;
import com.xsj.programmerartist.metrics.annotation.MetricException;
import com.xsj.programmerartist.metrics.annotation.MetricLatency;
import com.xsj.programmerartist.metrics.annotation.MetricMethod;
import com.xsj.programmerartist.metrics.annotation.MetricQPS;
import com.xsj.programmerartist.metrics.bean.Tag;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * spring 切面，实现发送 metrics 打点逻辑
 *
 * @author zyb
 * @date 2024/11/29
 */
@Component
@Aspect
public class AspectMetrics {
    private static final Logger log = LoggerFactory.getLogger(AspectMetrics.class);

    /*@Pointcut("@annotation(com.xsj.metrics.annotation.MetricQPS)")
    public void pointCutMethod() { }*/


    @Around("@annotation(metricQPS)")
    public Object around(ProceedingJoinPoint joinPoint, MetricQPS metricQPS) throws Throwable {
        Object result = this.doAround(joinPoint, metricQPS.name(), metricQPS.tags(),
                true, false, false, false);
        return result;
    }

    @Around("@annotation(metricLatency)")
    public Object around(ProceedingJoinPoint joinPoint, MetricLatency metricLatency) throws Throwable {
        Object result = this.doAround(joinPoint, metricLatency.name(), metricLatency.tags(),
                false, true, false, false);
        return result;
    }

    @Around("@annotation(metricException)")
    public Object around(ProceedingJoinPoint joinPoint, MetricException metricException) throws Throwable {
        Object result = this.doAround(joinPoint, metricException.name(), metricException.tags(),
                false, false, metricException.ratio(), metricException.qps());
        return result;
    }

    @Around("@annotation(metricMethod)")
    public Object around(ProceedingJoinPoint joinPoint, MetricMethod metricMethod) throws Throwable {
        Object result = this.doAround(joinPoint, metricMethod.name(), metricMethod.tags(),
                metricMethod.qps(), metricMethod.latency(), metricMethod.exceptionRatio(), metricMethod.exceptionQPS());
        return result;
    }


    /**
     *
     * @param joinPoint
     * @param name
     * @param tags
     * @param qps
     * @param latency
     * @param exceptionRatio
     * @param exceptionQPS
     * @return
     * @throws Throwable
     */
    public Object doAround(ProceedingJoinPoint joinPoint, String name, String[] tags,
                           boolean qps, boolean latency, boolean exceptionRatio, boolean exceptionQPS) throws Throwable {

        Object returnObj = null;

        Tag tag = null;
        if(null==name || name.length()==0) {     // 如果用户没在注解里设置监控名，则默认取 类名全小写_方法名全小写
            name = joinPoint.getSignature().getDeclaringType().getSimpleName().toLowerCase() + "_" + joinPoint.getSignature().getName().toLowerCase();
        }
        // 如果tags不成对，则格式错误，全部丢弃
        if(null!=tags && tags.length>0 && tags.length%2==0) {
            tag = this.toMetricTag(tags);
        }

        // 1、方法开始前，打qps监控
        this.safeSendMetrics(name, tag, qps, false, false, false, null, null);

        final long beginTime         = System.currentTimeMillis();
        final boolean checkException = exceptionRatio || exceptionQPS;

        try {
            if(checkException) {
                boolean error = false;
                try {
                    returnObj = joinPoint.proceed();    // 如果需要监控异常，则在 try catch里执行原方法逻辑

                } catch (Throwable e) {
                    error = true;                       // 这里只做监控判断，不对异常做处理，并需要抛出异常
                    throw e;
                } finally {
                    // 2、打异常监控
                    this.safeSendMetrics(name, tag, false, false, exceptionRatio, exceptionQPS, error, null);
                }
            }else {
                returnObj = joinPoint.proceed();       // 如果不需要监控异常，则直接执行原方法逻辑
            }

        } finally {
            // 3、方式执行完成，打耗时监控
            this.safeSendMetrics(name, tag, false, latency, false, false, null, beginTime);
        }

        return returnObj;
    }


    /**
     *
     * @param name
     * @param tag
     * @param qps
     * @param latency
     * @param exceptionRatio
     * @param exceptionQPS
     * @param error
     * @param beginTime
     */
    private void safeSendMetrics(String name, Tag tag,
                                 boolean qps, boolean latency, boolean exceptionRatio, boolean exceptionQPS,
                                 Boolean error, Long beginTime) {

        try {
            // qps
            if(qps) {
                if(null != tag) {
                    Metrics.tag("", tag).qps(name);
                }else {
                    Metrics.simple("").qps(name);
                }
            }

            // error
            final String errorName = name + "_error";
            if(null != tag) {
                if(exceptionRatio) { Metrics.tag("", tag).ratio(errorName, error); }
                if(exceptionQPS && error) { Metrics.tag("", tag).qps(errorName); }
            }else {
                if(exceptionRatio) { Metrics.simple("").ratio(errorName, error); }
                if(exceptionQPS && error) { Metrics.simple("").qps(errorName); }
            }

            // latency
            if(latency) {
                long cost = System.currentTimeMillis() - beginTime;
                if(null != tag) {
                    Metrics.tag("", tag).latency(name, cost);
                }else {
                    Metrics.simple("").latency(name, cost);
                }
            }

        } catch (Throwable e) {
            log.warn("AspectMetrics doAround qps error: " + e.getMessage());
            //  do nothing。不能影响原方法执行
        }

    }




    /**
     *
     * @param tags
     * @return
     */
    private Tag toMetricTag(String[] tags) {
        Tag tag = Tag.builder();
        for(int i=0; i<tags.length; i+=2) {
            tag.add(tags[i], tags[i+1]);
        }
        return tag;
    }


}
