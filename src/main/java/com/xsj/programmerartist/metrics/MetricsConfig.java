package com.xsj.programmerartist.metrics;


import com.xsj.programmerartist.metrics.bean.Tag;
import com.xsj.programmerartist.metrics.constant.Const;
import com.xsj.programmerartist.metrics.process.impl.PrometheusMetricsProcessor;
import com.xsj.programmerartist.metrics.util.MetricsUtil;
import io.micrometer.core.instrument.MeterRegistry;
import static com.xsj.programmerartist.metrics.constant.Const.Protocol.DEFAULT_BIG_TEAM;

/**
 * 初始化设置
 *
 * @Author zyb
 * @Date 2024/11/27
 **/
public class MetricsConfig {
    private static final MetricsConfig config = new MetricsConfig();
    private static final boolean asyncInContainer = true;            // 是否异步入容器，默认异步

    private String bigTeam = DEFAULT_BIG_TEAM;          // 团队名
    private String group;                               // 小组名
    private String app;                                 // 应用名

    private Tag globalTag = Tag.builder();
    private String globalTagStr;
    private Const.ProcessType processType;              // for 日志


    /**
     *
     */
    private MetricsConfig() {
    }

    /**
     *
     * @return
     */
    public static MetricsConfig getInstance() {
        return config;
    }

    /**
     * 可选配置
     *
     * @param bigTeam
     * @return
     */
    public MetricsConfig bigTeam(String bigTeam) {
        if(MetricsUtil.isNotBlank(bigTeam)) {
            this.bigTeam = MetricsUtil.replaceIllegal(bigTeam.trim(), Const.API.ILLEGAL_SEP_4_BIG_TEAM, Const.API.LEGAL_SEP);
        }
        globalTag.add(Const.Protocol.TAG_TEAM, this.bigTeam);
        return this;
    }

    /**
     * 可选配置
     *
     * @param group
     * @return
     */
    public MetricsConfig group(String group) {
        if(null!=group && !"".equals(group)) this.group = MetricsUtil.replaceIllegal(group.trim());
        globalTag.add(Const.Protocol.TAG_GROUP, this.group);
        return this;
    }

    /**
     * 可选配置
     *
     * @param app
     * @return
     */
    public MetricsConfig app(String app) {
        if(null!=app && !"".equals(app)) this.app = MetricsUtil.replaceIllegal(app.trim());
        globalTag.add(Const.Protocol.TAG_APP, this.app);
        return this;
    }

    /**
     * 可选配置
     *
     * @param globalTag
     * @return
     */
    public MetricsConfig globalTag(Tag globalTag) {
        if(null!=globalTag && null!=globalTag.getValue()) {
            globalTag.getValue().forEach(t -> this.globalTag.add(t.getName(), t.getValue()));
        }

        globalTagStr   = null!=this.globalTag ? this.globalTag.toString() : "";
        return this;
    }

    /**
     *
     * @param meterRegistry
     * @return
     */
    public MetricsConfig prometheusImpl(MeterRegistry meterRegistry) {
        PrometheusMetricsProcessor.getInstance()._setMeterRegistry(meterRegistry);
        this.processType = Const.ProcessType.PROMETHEUS;
        return this;
    }


    public boolean isAsyncInContainer() {
        return asyncInContainer;
    }

    /**
     *
     * @return
     */
    public String getGlobalTagStr() {
        return globalTagStr;
    }

    public Const.ProcessType getProcessType() {
        return processType;
    }

    public String getBigTeam() {
        return bigTeam;
    }

    public String getGroup() {
        return group;
    }

    public String getApp() {
        return app;
    }

    public Tag getGlobalTag() {
        return globalTag;
    }


    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "ProducerConfig{" +
                "bigTeam='" + bigTeam + '\'' +
                ", group='" + group + '\'' +
                ", app='" + app + '\'' +
                ", processType=" + processType +
                ", globalTag=" + globalTag +
                '}';
    }
}
