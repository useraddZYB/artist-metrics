package com.xsj.programmerartist.metrics.util;


import com.xsj.programmerartist.metrics.constant.Const;
import com.xsj.programmerartist.metrics.service.AsyncSend;

import java.util.*;

/**
 * @Author zyb
 * @Date 2024/11/27
 **/
public class ControlConfig {

    /** true ? 打点 : 不打点 */
    public static volatile boolean globalSwitch = true;

    /**
     * 全局采样比例：
     * 100表示不采样全部打点、30表示只发送30%的打点、0表示不打点
     * count类型打点，不采样，全部打点；
     *
     * */
    public static volatile int globalSamplePercent = 100;
    public static final Set<Const.MetricsType> GLOBAL_SAMPLE_M_TYPES
            = new HashSet<>(Arrays.asList(
                    Const.MetricsType.LATENCY,
                    Const.MetricsType.MEAN,
                    Const.MetricsType.GAUGE)
                );
    // qps 采样，及值恢复
    public static volatile int globalQpsSamplePercent  = 100;
    private static final int DEFAULT_RECOVER           = 1;
    private static volatile int globalQpsSampleRecover = DEFAULT_RECOVER;

    /**
     * 自定义采样比例：
     * 指标名:采样比例
     *
     */
    public static Map<String, Integer> metricsName2SamplePercent = new HashMap<>();

    /**
     *
     */
    public static Map<String, Set<String>> tagName2NormalValues = new HashMap<>();

    /**
     * 设置日志输出次数，debug打点协议输出error级别的日志
     */
    public static volatile int debugPrintProtocolTimes = 0;

    /**
     *
     * @param source
     * @return
     */
    public static int legalQpsSample(int source) {
        if(source <= 0) {
            globalQpsSampleRecover = DEFAULT_RECOVER;
            return 0;
        }else if(source >= 100) {
            globalQpsSampleRecover = DEFAULT_RECOVER;
            return 100;
        }else {
            if(source == 1) {
                globalQpsSampleRecover = 100;
                return 1;
            }else if(source <= 4) {
                globalQpsSampleRecover = 50;
                return 2;
            }else if(source <= 9) {
                globalQpsSampleRecover = 20;
                return 5;
            }else if(source <= 19) {
                globalQpsSampleRecover = 10;
                return 10;
            }else if(source <= 24) {
                globalQpsSampleRecover = 5;
                return 20;
            }else if(source <= 49) {
                globalQpsSampleRecover = 4;
                return 25;
            }else {
                globalQpsSampleRecover = 2;
                return 50;
            }
        }
    }

    /**
     *
     * @param newSample
     */
    public static boolean changeQpsSample(int newSample) {
        if(globalQpsSamplePercent != newSample) {
            globalQpsSamplePercent = newSample;
            AsyncSend._changeQpsVal(Const.API.QPS_VAL * globalQpsSampleRecover);
            return true;
        }

        return false;
    }
    public static int getQpsVal() {
        return AsyncSend.getQpsVal();
    }

    /**
     *
     * @param tagName
     * @param tagValue
     * @return
     */
    public static Object getTagValByClean(String tagName, Object tagValue) {
        if(tagName2NormalValues.size() == 0) { return tagValue; }

        Set<String> values = tagName2NormalValues.get(tagName);
        return (null==values || values.contains(tagValue.toString())) ? tagValue : Const.API.TAG_VAL_OTHERS;
    }
}
