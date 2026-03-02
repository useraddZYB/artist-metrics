# artist-metrics 集成、使用及配置文档  

支持6种业务指标：
1. qps流量
2. latency耗时（自带均值、p50 p95 p99）
3. mean均值（如返回列表均值、结算人均、人均金额等）
4. ratio比例（如异常率 成功率等）
5. count累积求和
6. gauge测量（如当前线程池大小、连接池大小、内存大小等）

```
原理说明：以上6种业务指标底层共享3种指标，qps count使用底层counter指标，latency mean ratio使用底层summary指标，gauge使用底层gauge指标
```

## 一、集成分五步
分别是必选四步： [增加依赖](#pom-add-dependecy)、[增加配置](#properties-add-config)、[启动类注入bean及启动](#application-set-and-startup)、[运维增加job配置](#op-add-job)，和可选的一步：[非web项目增加web](#not-web-add-web)。

### <a name="pom-add-dependecy"></a>第一步：增加依赖
在业务服务的pom.xml文件中，增加 xsj-metrics sdk依赖：  

```
<!-- metrics -->
<dependency>
    <groupId>com.programmerartist.artistmetric</groupId>
    <artifactId>artist-metric</artifactId>
    <version>1.0.0</version>
</dependency>
```

### <a name="properties-add-config"></a>第二步：增加配置
在application.properties配置文件中，增加如下配置：  

```
# prometheus artist-metrics
management.endpoints.enabled-by-default=false
management.endpoint.prometheus.enabled=true
management.endpoints.web.exposure.include[0]=prometheus
management.metrics.enable.jvm=false
management.metrics.enable.process=false
management.metrics.enable.tomcat=false
management.metrics.enable.logback=false
```

### <a name="application-set-and-startup"></a>第三步：启动类注入bean及启动
在xxServerApplication.java启动类中，往sdk中注入 meterRegistry 并启动，demo如下：

#### 关键3点：
1. spring scanBasePackages 扫描包增加 "com.xsj.metrics"，添加成员变量bean：private MeterRegistry meterRegistry; 
2. 启动类实现 implements ApplicationRunner 接口，实现run方法
3. run方法内，将 meterRegistry 注入到sdk中作为打点实现类，及调用sdk的startup接口

```
import com.xsj.programmerartist.metrics.MBootstrap;
import com.xsj.programmerartist.metrics.MetricsConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 服务启动类
 */
@SpringBootApplication(
        scanBasePackages = {
            "com.xx.rec.server",
            "com.programmerartist.metrics"
        }
)
@EnableDubbo
public class RecServerApplication implements ApplicationRunner {
    @Autowired
    private MeterRegistry meterRegistry;

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        SpringApplication.run(RecServerApplication.class, args);
    }

    /**
     *
     * @param args incoming application arguments
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        this.initMetrics();
    }

    /**
     *
     */
    private void initMetrics() {
        // 设置打点实现
        MetricsConfig.getInstance().prometheusImpl(meterRegistry);
        MBootstrap.startup();
    }
}
```

### <a name="op-add-job"></a>第四步：运维增加job配置
通知运维同学此服务需要增加监控抓取，添加prometheus抓取job配置

### <a name="not-web-add-web"></a>第五步（可选）：非web项目增加web
如果是web服务，则不需要操作这个步骤。如果此业务服务不是web服务，则需要添加web依赖改造为web服务。
（说明：由于监控组件prometheus需要定时调用web http接口来抓取监控指标数据）  

在pom.xml中，添加spring web依赖：
```
<!-- spring-boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>${spring-boot.version}</version>
</dependency>
```

在application.properties配置文件中，增加如下示范配置：

```
# spring web
spring.application.name=xx-server-spring
server.port=xx
```


## 二、代码使用
xsj-metrics 打点监控sdk，目前提供三种方式打监控：方法注解、类变量、静态方法。任选一种或多种使用即可。分别示范如下：  

1. 方法注解（共提供4种注解，注解中不设置name指标名的话，默认取classname.toLower_methodname.toLower）：

```
/**
 * 用户历史过滤&查询，service接口
 */
@DubboService
public class UserHistoryServiceImpl implements UserHistoryService {
    private static final Logger log = LoggerFactory.getLogger(UserHistoryServiceImpl.class);

    /**
     *
     * @param getRequest 查询参数
     * @return
     */
    @Override
    @MetricMethod(name = "history_service_get", qps = true, latency = true)
    public UserHistory get(HistoryGetRequest getRequest) {
        Watch watch = Watch.start();
        // business code...

        // 监控指标名为：history_service_get_qps、history_service_get_latency
        
        log.info("get finish: userId={}, size={}, cost={}", userId, ParamUtil.size(historyItems), watch.totalCost());
        return userHistory;
    }

    /**
     *
     * @param request     过滤参数
     * @param userHistory 用户历史数据
     * @return
     */
    @Override
    @MetricQPS
    @MetricLatency
    @MetricException
    public HistoryFilterResponse filter(HistoryFilterRequest request, UserHistory userHistory) {
        Watch watch = Watch.start();
        // business code...
        
        // 监控指标名为：userhistoryserviceimpl_filter_qps、userhistoryserviceimpl_filter_latency、userhistoryserviceimpl_filter_error_ratio
        
        log.info("filter finish: userId={}, size={}, cost={}", request.getUserId(), ParamUtil.size(filterResponses), watch.totalCost());
        return filterResponses.get(0);
    }
}
```

2. 类变量（类中共享指标名前缀）：

```
/**
 * 用户历史过滤&查询，service接口
 *
 */
@DubboService
public class UserHistoryServiceImpl implements UserHistoryService {
    private static final SimpleMetrics metrics = Metrics.simple("history_service");
    private static final Logger log = LoggerFactory.getLogger(UserHistoryServiceImpl.class);

    /**
     *
     * @param getRequest 查询参数
     * @return
     */
    @Override
    public UserHistory get(HistoryGetRequest getRequest) {
        Watch watch = Watch.start();
        // business code...

        // 监控指标名为：history_service_get_qps、history_service_get_latency
        metrics.qps("get").latency("get", watch.totalCost());
        
        log.info("get finish: userId={}, size={}, cost={}", userId, ParamUtil.size(historyItems), watch.totalCost());
        return userHistory;
    }

    /**
     *
     * @param request     过滤参数
     * @param userHistory 用户历史数据
     * @return
     */
    @Override
    public HistoryFilterResponse filter(HistoryFilterRequest request, UserHistory userHistory) {
        Watch watch = Watch.start();
        // business code...
        
        // 监控指标名为：history_service_filter_qps、history_service_filter_latency
        metrics.qps("filter").latency("filter", watch.totalCost());
        
        log.info("filter finish: userId={}, size={}, cost={}", request.getUserId(), ParamUtil.size(filterResponses), watch.totalCost());
        return filterResponses.get(0);
    }
}
```

3. 静态方法：

```
/**
 * 用户历史过滤&查询，service接口
 */
@DubboService
public class UserHistoryServiceImpl implements UserHistoryService {
    private static final Logger log = LoggerFactory.getLogger(UserHistoryServiceImpl.class);

    /**
     *
     * @param getRequest 查询参数
     * @return
     */
    @Override
    public UserHistory get(HistoryGetRequest getRequest) {
        Watch watch = Watch.start();
        // business code...

        // 监控指标名为：history_service_get_qps、history_service_get_latency
        Metrics.simple("history_service").qps("get").latency("get", watch.totalCost());
        
        log.info("get finish: userId={}, size={}, cost={}", userId, ParamUtil.size(historyItems), watch.totalCost());
        return userHistory;
    }

    /**
     *
     * @param request     过滤参数
     * @param userHistory 用户历史数据
     * @return
     */
    @Override
    public HistoryFilterResponse filter(HistoryFilterRequest request, UserHistory userHistory) {
        Watch watch = Watch.start();
        // business code...
        
        // 监控指标名为：history_service_filter_qps、history_service_filter_latency
        Metrics.simple("history_service").metrics.qps("filter").latency("filter", watch.totalCost());
        
        log.info("filter finish: userId={}, size={}, cost={}", request.getUserId(), ParamUtil.size(filterResponses), watch.totalCost());
        return filterResponses.get(0);
    }
}
```  

## 三、Grafana配置监控面板
测试环境地址：http://grafana.xx.cn/  
grafana配置示范如下，分别配置：qps流量、latency耗时、mean均值、ratio比例、count累积求和、gauge测量。  

```
sum (increase(rec_service_recommend_qps_total[1m]))

sum (increase(rec_service_resource_load_total_latency_sum[1m])) / sum (increase(rec_service_resource_load_total_latency_count[1m]))

sum (increase(rec_service_recommend_size_home_rec_sum[1m])) / sum (increase(rec_service_recommend_size_home_rec_count[1m]))

sum (increase(profile_get_whole_error_ratio_sum[1m])) / sum (increase(profile_get_whole_error_ratio_count[1m]))

```  

