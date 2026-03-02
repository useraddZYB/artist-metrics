package com.xsj.programmerartist.metrics;

import com.xsj.programmerartist.metrics.service.AsyncSend;
import com.xsj.programmerartist.metrics.util.ControlConfig;
import com.xsj.programmerartist.metrics.util.MetricsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *  启动类
 *
 * @Author zyb
 * @Date 2024/11/27
 **/
public class MBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(MBootstrap.class);

    private static volatile boolean start = false;
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private MBootstrap() {}


    /**
     * 启动
     *
     */
    public static void startup() {
        if(start) return;

        boolean success = false;
        lock.writeLock().lock();
        try {
            if(start) return;

            final String flag = " ****************************";

            // 1，检查配置
            MetricsConfig config;
            try {
                config = MetricsUtil.checkOrInitMConfig(logger);
                AsyncSend.setBootstrapStart(true);
                AsyncSend.init();

                logger.info("[metrics][info] MBootstrap.startup(): MConfig=" + config + flag);
                System.out.println("[metrics] MBootstrap.startup(): MConfig=" + config + flag);
            } catch (Throwable e) {
                logger.error("[metrics][error] MBootstrap.startup(): initMConfig error; msg = " + e.getMessage(), e);
                throw new RuntimeException("[metrics] MBootstrap.startup(): initMConfig error; msg = " + e.getMessage(), e);
            }

            logger.info("[metrics][info] MBootstrap.startup(): success" + flag);
            System.out.println("[metrics] MBootstrap.startup(): success" + flag);
            success = true;

        } finally {
            lock.writeLock().unlock();
            if(!start) start = true;

            if(!success) logger.error("[metrics][error] MBootstrap.startup(): failed");
        }
    }




    // ======================================= 可选 ===========================================

    /**
     * 可选的自动更新的全局配置
     */
    public static class AutoConfig {
        private static volatile boolean invokeAuto = false;

        private static final int DELAY         = 1;
        private static final int PERIOD        = 10;
        private static final String SEP_OUTTER = ",";
        private static final String SEP_INNER  = ":";
        private static final String SEP_SEMI   = ";";

        private Callable<Boolean> callSwitch                      = null;
        private Callable<Integer> callSample                      = null;
        private Callable<Integer> callQpsSample                   = null;
        private Callable<String> callMetricName2Sample            = null;
        private Callable<Map<String, String>> callMName2SampleMap = null;
        private Callable<Map<String, String>> callGetCleanTag     = null;
        private String oldMetricName2Sample                       = "";
        private String oldCleanTag                                = "";

        private AutoConfig() { }
        private static final AutoConfig INSTANCE = new AutoConfig();
        public static AutoConfig getInstance() { return INSTANCE; }

        /**
         *
         * @param debugPrintProtocolTimes
         * @return
         */
        public AutoConfig debugPrintProtocolTimes(int debugPrintProtocolTimes) {
            try {
                if(debugPrintProtocolTimes > 0) {
                    ControlConfig.debugPrintProtocolTimes = debugPrintProtocolTimes;
                    if(logger.isInfoEnabled()) {
                        logger.info("[metrics][warn] debugPrintProtocolTimes success, change debugPrintProtocolTimes to be=" + debugPrintProtocolTimes);
                    }else {
                        logger.error("[metrics][warn] debugPrintProtocolTimes success, change debugPrintProtocolTimes to be=" + debugPrintProtocolTimes);
                    }
                }
            } catch (Exception e) {
                // doNothing
            }
            return this;
        }

        /**
         * 定时更新
         *
         * 全局开关：        开关==true ? 表示可以正常打点 : 表示禁止打点
         * 全局采样：        100表示不采样全部打点、30表示只发送30%的打点、0表示不打点；qps、count不做采样
         * 全局QPS采用：     强制qps采样，系统会通过反比例换算使恢复值，少量误差
         * 自定义采样string：eg: "cs_query_qps:30,cs_query_latency:70"
         * 自定义采样map：   eg: map.put("cs_query_qps", "30"); map.put("cs_query_latency","70");
         */
        public AutoConfig autoSwitch(Callable<Boolean> callSwitch) { this.callSwitch = callSwitch; return this; }
        public AutoConfig autoSample(Callable<Integer> callSample) { this.callSample = callSample; return this; }
        public AutoConfig autoQpsSample(Callable<Integer> callQpsSample) { this.callQpsSample = callQpsSample; return this; }
        public AutoConfig autoMetricName2Sample(Callable<String> callMetricName2Sample) { this.callMetricName2Sample = callMetricName2Sample; return this; }
        public AutoConfig autoMetricName2SampleMap(Callable<Map<String, String>> callMName2SampleMap) {
            this.callMetricName2Sample = null;
            this.callMName2SampleMap   = callMName2SampleMap; return this;
        }

        /**
         * tag清理：针对非传入的"常规"tag值，全部将值替换为others
         * 目的：解决部分业务tag的值过多，比如appId有几百个，但是重要的就十几个，全部正常输出会导致监控页没法看，也对监控服务端压力过大
         *
         * @param tagName2NormalValues eg: map.put("appId", "oppo,xiaomi,vivo"); map.put("xx", "v1,v2,v3");
         * @return
         * @throws Exception 首次执行发生在调用该函数的时候，必须成功，失败的话服务应该停止启动
         */
        public AutoConfig autoGetCleanTag(Callable<Map<String, String>> tagName2NormalValues) throws Exception {
            this.callGetCleanTag = tagName2NormalValues;
            this.doGetCleanTag(true);
            return this;
        }

        /**
         *
         *
         */
        public void doAuto() {
            if(invokeAuto) return;

            ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                private int count;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r,  "Metrics-DoAuto-Thread-" + (++count));
                    t.setDaemon(true);
                    return t;
                }
            });

            es.scheduleAtFixedRate(() -> {

                // 1, 全局开关
                try {
                    if(null != callSwitch) {
                        Boolean newSwitch = callSwitch.call();
                        if(null!=newSwitch && ControlConfig.globalSwitch!=newSwitch.booleanValue()) {
                            ControlConfig.globalSwitch = newSwitch;
//                            MetricsConsumer.getInstance()._changeSwitch();
                            String flag = ControlConfig.globalSwitch ?  " +++++++++++++++++++++++++++++" : "---------------------------";
                            if(logger.isInfoEnabled()) {
                                logger.info("[metrics][warn] autoSwitch success, change globalSwitch to be=" + newSwitch + flag);
                            }else {
                                logger.error("[metrics][warn] autoSwitch success, change globalSwitch to be=" + newSwitch + flag);
                            }
                        }
                    }
                } catch (Exception e) {
                    // doNothing
                }

                // 2, 全局采样
                try {
                    if(null != callSample) {
                        Integer newSample = callSample.call();
                        if(null!=newSample && ControlConfig.globalSamplePercent!=newSample.intValue()) {
                            ControlConfig.globalSamplePercent = newSample;
                            if(logger.isInfoEnabled()) {
                                logger.info("[metrics][warn] autoSample success, change globalSamplePercent to be=" + newSample);
                            }else {
                                logger.error("[metrics][warn] autoSample success, change globalSamplePercent to be=" + newSample);
                            }
                        }
                    }
                } catch (Exception e) {
                    // doNothing
                }

                // 2.1, 全局QPS采样
                try {
                    if(null != callQpsSample) {
                        Integer newQpsSample = callQpsSample.call();
                        if(null != newQpsSample) {
                            newQpsSample = ControlConfig.legalQpsSample(newQpsSample);
                            if(true == ControlConfig.changeQpsSample(newQpsSample.intValue())) {
                                if(logger.isInfoEnabled()) {
                                    logger.info("[metrics][warn] autoQpsSample success, change globalQpsSamplePercent to be=" + newQpsSample + ", qpsVal=" + ControlConfig.getQpsVal());
                                }else {
                                    logger.error("[metrics][warn] autoQpsSample success, change globalQpsSamplePercent to be=" + newQpsSample + ", qpsVal=" + ControlConfig.getQpsVal());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // doNothing
                }

                // 3, 自定义采样
                try {
                    if(null != callMetricName2Sample) {
                        String newM2sStr = callMetricName2Sample.call();
                        if(null!=newM2sStr && !oldMetricName2Sample.equals(newM2sStr)) {
                            Map<String, Integer> newM2s = new HashMap<>();

                            if(!"".equals(newM2sStr.trim())) {
                                String[] m2sArr = newM2sStr.split(SEP_OUTTER);
                                if(null != m2sArr) {
                                    for(String m2s : m2sArr) {
                                        String[] ms = m2s.split(SEP_INNER);
                                        if(null!=ms && ms.length>=2) {
                                            try {
                                                newM2s.put(ms[0], Integer.parseInt(ms[1]));
                                            } catch (NumberFormatException e) {
                                                // doNothing
                                            }
                                        }
                                    }
                                }
                            }

                            ControlConfig.metricsName2SamplePercent = newM2s;
                            this.oldMetricName2Sample                = newM2sStr;
                            if(logger.isInfoEnabled()) {
                                logger.info("[metrics][warn] autoMetricName2Sample success, change metricName2Sample to be=" + newM2sStr);
                            }else {
                                logger.error("[metrics][warn] autoMetricName2Sample success, change metricName2Sample to be=" + newM2sStr);
                            }
                        }
                    }

                    if(null != callMName2SampleMap) {
                        boolean changed = false;
                        Map<String, String> newM2sTmp = callMName2SampleMap.call();
                        // 格式转化
                        Map<String, Integer> newM2s   = new HashMap<>();
                        if(null!=newM2sTmp && newM2sTmp.size()>0) {
                            newM2sTmp.entrySet().forEach(k2v -> {
                                try {
                                    Integer v = Integer.parseInt(k2v.getValue());
                                    newM2s.put(k2v.getKey(), v);
                                } catch (Exception e) {
                                }
                            });
                        }

                        // 清空
                        if((null==newM2s || newM2s.size()==0) && ControlConfig.metricsName2SamplePercent.size()>0) {
                            ControlConfig.metricsName2SamplePercent = new HashMap<>();
                            oldMetricName2Sample = "";
                            changed = true;
                        }else {  // 修改
                            List<String> keys = new ArrayList<>(newM2s.keySet());
                            Collections.sort(keys);  // for equals
                            StringBuilder sb = new StringBuilder();
                            keys.forEach(key -> sb.append(key + SEP_INNER + newM2s.get(key) + SEP_OUTTER));

                            if(!oldMetricName2Sample.equals(sb.toString())) {
                                oldMetricName2Sample = sb.toString();
                                ControlConfig.metricsName2SamplePercent = newM2s;
                                changed = true;
                            }
                        }

                        if(changed) {
                            if(logger.isInfoEnabled()) {
                                logger.info("[metrics][warn] autoMName2SampleMap success, change metricName2Sample to be=" + oldMetricName2Sample);
                            }else {
                                logger.error("[metrics][warn] autoMName2SampleMap success, change metricName2Sample to be=" + oldMetricName2Sample);
                            }
                        }
                    }
                } catch (Exception e) {
                    // doNothing
                }

                // 4，整理合法tag
                try {
                    if(null != callGetCleanTag) {
                        this.doGetCleanTag(false);
                    }
                } catch (Exception e) {
                    logger.info("[metrics][warn] autoGetCleanTag fail, msg=" + e.getMessage());
                }

            }, DELAY, PERIOD, TimeUnit.SECONDS);

            invokeAuto = true;
        }

        /**
         *
         */
        private void doGetCleanTag(final boolean firstExecutor) throws Exception {
            boolean changed = false;
            Map<String, String> tagName2ValStr = callGetCleanTag.call();
            // 隐含逻辑：之前填了，后面清空了，不允许（担心取数据失败造成的），除非重启
            if(null!=tagName2ValStr || tagName2ValStr.size()>0) {
                List<String> keys = new ArrayList<>(tagName2ValStr.keySet());
                Collections.sort(keys);
                StringBuilder sb = new StringBuilder();
                keys.forEach(key -> sb.append(key + SEP_INNER + tagName2ValStr.get(key) + SEP_SEMI));

                // 说明更改了
                if(!oldCleanTag.equals(sb.toString())) {
                    // 格式转化
                    Map<String, Set<String>> tagName2Vals = new HashMap<>();
                    tagName2ValStr.entrySet().forEach(k2v -> {
                        try {
                            if(MetricsUtil.isNotBlank(k2v.getValue())) {
                                String[] valArr = k2v.getValue().split(",");
                                Set<String> vals = new HashSet<>();
                                for(String val : valArr) { vals.add(val); }
                                tagName2Vals.put(k2v.getKey(), vals);
                            }
                        } catch (Exception e) {
                            if(firstExecutor) {
                                throw e;
                            }
                        }
                    });

                    if(tagName2Vals.size() > 0) {
                        ControlConfig.tagName2NormalValues = tagName2Vals;
                        changed = true;
                        oldCleanTag = sb.toString();
                    }else {
                        throw new RuntimeException("execute doGetCleanTag fail, tagName2Vals.size=0; tagName2ValStr=" + tagName2ValStr);
                    }
                }
            }

            if(changed) {
                if(logger.isInfoEnabled()) {
                    logger.info("[metrics][warn] autoGetCleanTag success: firstExecutor=" + firstExecutor + ", change tagName2NormalValues to be=" + oldCleanTag);
                }else {
                    logger.error("[metrics][warn] autoGetCleanTag success: firstExecutor=" + firstExecutor + ", change tagName2NormalValues to be=" + oldCleanTag);
                }
            }
        }

    }

}
