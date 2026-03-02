package com.xsj.programmerartist.metrics.constant;

/**
 * 全部常量
 *
 * @Author zyb
 * @Date 2024/11/27
 **/
public interface Const {

    /**
     * ******************************************************* 底层处理实现类型
     */
    enum ProcessType {
        PROMETHEUS
    }


    /**
     * ******************************************************* 协议
     */
    interface Protocol {
        String METRICS_SEP            = "_";
        String METRICS_SUFFIX_QPS     = METRICS_SEP + "qps";
        String METRICS_SUFFIX_LATENCY = METRICS_SEP + "latency";
        String METRICS_SUFFIX_RATIO   = METRICS_SEP + "ratio";

        String TAG_TEAM               = "team";
        String TAG_GROUP              = "group";
        String TAG_APP                = "app";
        String TAG_HOST               = "hostname";
        String TAG_ENV                = "env";
        String TAG_APP_ID             = "appId";
        String DEFAULT_BIG_TEAM       = "xsj";

        String PROTOCOL_SEP           = ".";
        String TAG_VALUE_SEP          = "-";
        String VALUE_PREFIX           = ":";
        String TYPE_PREFIX            = "|";

        String TAG_4_HOST             = "host";
    }


    /**
     * ******************************************************* 客户端API
     */
    interface API {
        int RATE_YES_VAL              = 100;
        int RATE_NO_VAL               = 0;
        int QPS_VAL                   = 1;
        String TIME_UNIT              = "1sec";
        char[] ILLEGAL_SEP            = ".:|@".toCharArray();
        char[] ILLEGAL_SEP_4_BIG_TEAM = ":|@".toCharArray();
        char LEGAL_SEP                = '_';
        String TAG_VAL_OTHERS         = "others";
    }


    /**
     * ******************************************************* 服务端
     */
    interface RemoteServer {
        String DEFAULT_HOST        = "127.0.0.1";
        int DEFAULT_PORT           = 15688;
    }


    /**
     * ******************************************************* 业务系统配置文件配置项
     */
    interface ServerProperties {
        String pre  = "metrics.";

        String ENV_XSJ_LOCAL_IP     = "XSJ_LOCAL_IP";      // for ServerProperties.HOST K8S下为虚拟ip
        String ENV_XSJ_LOCAL_PORT   = "XSJ_LOCAL_PORT";    // for ServerProperties.TAG_PORT
        String ENV_K8S_XSJ_LOCAL_IP = "K8S_XSJ_LOCAL_IP";  // for ServerProperties.HOST 物理机ip

        /** consumer */
        String HOST        = pre + "host";
        String PORT        = pre + "port";
        String THREAD_SIZE = pre + "thread.size";

        /** producer */
        String BIG_TEAM    = pre + "big.team";
        String GROUP       = pre + "group";
        String COMPONENT   = pre + "component";
        String TAG_ENV     = pre + "tag.env";
        String TAG_APP_ID  = pre + "tag.appId";
        String TAG_PORT    = pre + "tag.port";
        String USE_HOST    = pre + "use.host";

        String SAFE_USE_HOST_SECOND = pre + "safe.use.host.second";

    }


    /**
     * ******************************************************* 客户端选用的初始化配置方式
     */
    enum InitConfigType {
        FILE, CODE
    }



    /**
     * ******************************************************* 打点类型
     */
    enum MetricsType {
        COUNTER,
        LATENCY,
        MEAN,
        RATIO,
        GAUGE,
        SET
    }

}
