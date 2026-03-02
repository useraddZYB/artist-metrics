package com.xsj.programmerartist.metrics;

import com.xsj.programmerartist.metrics.bean.Tag;
import com.xsj.programmerartist.metrics.service.SimpleMetrics;
import com.xsj.programmerartist.metrics.util.Watch;

import java.util.HashMap;
import java.util.Map;

/**
 * 集成示范代码
 *
 * @author zyb
 * @date 2024/11/28
 */
public class MetricsDemoTest {

    /*@Autowired
    private MeterRegistry meterRegistry;*/

    static {
        try {
            // 使用代码做初始化：代码注入少量配置，不需要新建或修改配置文件
            // startBootstrapByCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        /*testMetric();
        registerSomeGauge();*/
    }


    /**
     *
     */
    private static void startBootstrapByCode() throws Exception {
        Watch watch = Watch.start();

        /*// Tag globalTag = Tag.newTag(Const.Protocol.TAG_ENV, "a3");                                                 // 服务粒度
        // Tag globalTag = Tag.newTag(Const.Protocol.TAG_ENV, "a3").add(Const.Protocol.TAG_HOST, MUtil.getHost());   // 单机器粒度
        Tag globalTag = Tag.builder().add(Const.Protocol.TAG_ENV, "dev").add(Const.Protocol.TAG_HOST, MetricsUtil.getHostPort());  // 单机多实例粒度
        // 可选，不设置，取默认值 xsj
        String bigTime = "xsj";

        // 设置团队服务、环境
        MetricsConfig.getInstance().bigTeam(bigTime).group("video").app("test-server").globalTag(globalTag); */

        // 设置打点实现
        MetricsConfig.getInstance().prometheusImpl(null); // 实际填入 meterRegistry ( spring bean )

        MBootstrap.startup();

        System.out.println("startBootstrap cost===============" + watch.totalCost());

        /* 可选的采样配置，当打点太多影响性能，可以考虑加入采样
        Map<String, String> mName2Sample = new HashMap<>();
        mName2Sample.put("SearchServlet_search_qps", "20");
        mName2Sample.put("SearchServlet_search_wholeMethod", "50");

        MBootstrap.AutoConfig.getInstance()
                .autoSwitch(() -> true)
                .autoSample(() -> 50)
                .autoQpsSample(() -> 30)
                .autoMetricName2Sample(() -> "SearchServlet_search_qps:80,SearchServlet_search_wholeMethod:90")
                .autoMetricName2SampleMap(() -> mName2Sample)
                .debugPrintProtocolTimes(30)
                .doAuto();*/
    }



    /**
     *
     * @throws Exception
     */
    private static void testMetric() throws Exception {
        Watch watch = Watch.start();

        // 1, test simple
        Metrics.simple().qps("SearchServlet_search_queryMongo")
                .latency("SearchServlet_search_queryMongo", 200)
                .ratioYes("SearchServlet_search_exception");
        System.out.println();

        // 2, test simple and watch
        watch.reStart();
        SimpleMetrics metrics = Metrics.simple("SearchServlet_search").ratioNo("exception").qps("queryMongo");
        Thread.sleep(200L);
        metrics.latency("queryMongo", watch);
        Thread.sleep(20L);
        metrics.latency("queryRedis", watch);
        Thread.sleep(300L);
        metrics.latency("wholeMethod", watch.totalCost());

        // 4,
        Tag tag = Tag.builder().add("appId", 100).add("platform", "oppo");
        System.out.println(tag);
        Metrics.tag("SearchServlet_search", tag).qps("query").count("call_size", 10).mean("call_size_mean", 10);

        System.out.println();
    }

    /**
     * 注册测量值
     * 打点底层会启动定时任务，每秒打"一次"点
     *
     */
    private static void registerSomeGauge() {
        Metrics.registerGauge("SearchServlet_search_thread_core", () -> 10L);
        Metrics.registerGauge("SearchServlet_search_thread_queue", () -> 100L);
        Metrics.registerGaugeMany(() -> getRedisStat());
    }

    /**
     *
     * @return
     */
    private static Map<String, Long> getRedisStat() {

        Map<String, Long> metricsName2Val = new HashMap<>();
        metricsName2Val.put("redis_waiter", 10L);
        metricsName2Val.put("redis_min_idle", 15L);
        metricsName2Val.put("redis_max_active", 88L);

        return metricsName2Val;
    }


    /**
     *
     * @throws Exception
     */
    /*private static void startBootstrapByFile() throws Exception {
        // 只需要一行代码，内容会从配置文件中读取配置，并初始化metrics
        MBootstrap.startup();
        MBootstrap.AutoConfig.getInstance()
                .autoGetCleanTag(() -> {
                    Map<String, String> name2Vals = new HashMap<>();
                    name2Vals.put("appId", "oppo,xiaomi");
                    name2Vals.put("tag1", "1,2");
                    return name2Vals;
                })
                .doAuto();
    }*/
}
