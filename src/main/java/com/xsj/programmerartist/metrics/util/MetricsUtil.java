package com.xsj.programmerartist.metrics.util;

import com.xsj.programmerartist.metrics.constant.Const;
import com.xsj.programmerartist.metrics.MetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.util.concurrent.ThreadLocalRandom;


/**
 * 工具类
 *
 * @Author zyb
 * @Date 2024/11/27
 **/
public class MetricsUtil {
    private static final Logger logger = LoggerFactory.getLogger(MetricsUtil.class);

    private static String envXsjLocalIp    = "";
    private static String envK8SXsjLocalIp = null;
    private static int envXsjLocalPort     = -1;

    private static volatile boolean shutdown  = false;


    /**
     *
     * @return
     * @throws Exception
     */
    public static MetricsConfig checkOrInitMConfig(Logger logger) throws Exception {

        // 优先，取用户配置；即代码初始化
        String errorMsg = "";
        if("".equals(errorMsg = MetricsUtil.checkMConfig(Const.InitConfigType.CODE))) {
            return MetricsConfig.getInstance();
        }else {
            logger.error("[metrics][warn] MBootstrap.startup(): First check config by manual is failed, detailMsg=" + errorMsg);
            throw new RuntimeException("[metrics][warn] MBootstrap.startup(): First check config by manual is failed, detailMsg=" + errorMsg);
        }
    }

    /**
     *
     * @param initConfigType
     * @return
     */
    public static String checkMConfig(Const.InitConfigType initConfigType) {
        StringBuilder errorMsg = new StringBuilder("");

        MetricsConfig metricsConfig = MetricsConfig.getInstance();

        if(null == metricsConfig) { errorMsg.append("metricsConfig is null, "); }
        if(errorMsg.length() > 0) { return errorMsg.toString(); }

        if(null == metricsConfig.getProcessType()) { errorMsg.append("processType is null, "); }

        /*if(MetricsUtil.isBlank(metricsConfig.getGroup())) { errorMsg.append("metrics.group is blank, "); }
        if(MetricsUtil.isBlank(metricsConfig.getApp())) { errorMsg.append("metrics.component is blank, "); }
        if(null == metricsConfig.getGlobalTag()) { errorMsg.append("metrics.tag. is null, "); }*/

        String errorStr = errorMsg.toString();
        if(errorStr.length() > 0) {
            errorStr = "[InitConfigType=" + initConfigType + "] " + errorStr;
        }
        return errorStr;
    }


    /**
     * 替换
     *
     * @param source 源字符串
     * @return       替换过后的字符串
     */
    public static String replaceIllegal(String source) {
        return MetricsUtil.replaceIllegal(source, Const.API.ILLEGAL_SEP, Const.API.LEGAL_SEP);
    }

    /**
     * 替换
     *
     * @param source        源字符串
     * @param illegalSepArr 需要被替换掉的字符数组
     * @param legalSep      替换为
     * @return              替换过后的字符串
     */
    public static String replaceIllegal(String source, char[] illegalSepArr, char legalSep) {
        if(null==source || "".equals(source)) { return source; }

        // 此方法，性能比replaceAll好，无正则
        for(char i : illegalSepArr) {
            source = source.replace(i, legalSep);
        }

        return source;
    }


    /**
     *
     * @param str
     * @return
     */
    public static boolean isBlank(String str) {
        return null==str || "".equals(str.trim());
    }

    /**
     *
     * @param str
     * @return
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     *
     * @return
     * @throws Exception
     */
    public static String getHost() throws Exception {
        String host = MetricsUtil.getEnvXsjLocalIp();
        if(MetricsUtil.isBlank(host)) {
            host = Inet4Address.getLocalHost().getHostAddress();
            if(MetricsUtil.isBlank(host)) {   // 容错
                host = "1_1_1_1";
            }
        }

        return host;
    }

    /**
     *
     * @return
     * @throws Exception
     */
    public static String getHostPort() throws Exception {
        // host
        String host = getHost();

        // port
        String port = "";  // todo PropertiesConfig.getString(Const.ServerProperties.TAG_PORT);
        if(MetricsUtil.isBlank(port)) {
            int portInt = MetricsUtil.getEnvXsjLocalPort();
            if(portInt <= 0) {
                portInt = ThreadLocalRandom.current().nextInt(3000, 4000);
            }
            port = portInt + "";
        }

        // hostPort
        String hostPort = host + (MetricsUtil.isNotBlank(port) ? String.valueOf(Const.API.LEGAL_SEP) + port : "");

        return replaceIllegal(hostPort);
    }


    /**
     *
     * @return
     */
    public static String getEnvXsjLocalIp() {
        if(MetricsUtil.isNotBlank(envXsjLocalIp)) { return envXsjLocalIp; }

        envXsjLocalIp = MetricsUtil.getEnv(Const.ServerProperties.ENV_XSJ_LOCAL_IP);

        logger.info("[metrics][info] yidian_ip=" + (null!= envXsjLocalIp ? envXsjLocalIp : ""));
        return envXsjLocalIp;
    }

    /**
     *
     * @return
     */
    public static String getEnvK8SXsjLocalIp() {
        if(null != envK8SXsjLocalIp) { return envK8SXsjLocalIp; }

        envK8SXsjLocalIp = MetricsUtil.getEnv(Const.ServerProperties.ENV_K8S_XSJ_LOCAL_IP);
        if(null == envK8SXsjLocalIp) { envK8SXsjLocalIp = ""; }

        logger.info("[metrics][info] k8s_ip=" + envK8SXsjLocalIp);
        return envK8SXsjLocalIp;
    }

    /**
     * 值用作UDP发包目的IP
     * 优先取 K8S_YIDIAN_LOCAL_IP 变量，不存在，则再取 YIDIAN_LOCAL_IP
     *
     * @return
     */
    public static String getMetricsServerHost() {

        String host = MetricsUtil.getEnvK8SXsjLocalIp();
        return MetricsUtil.isNotBlank(host) ? host : MetricsUtil.getEnvXsjLocalIp();
    }

    /**
     *
     * @return
     */
    public static int getEnvXsjLocalPort() {
        if(envXsjLocalPort > 0) { return envXsjLocalPort; }

        String port = MetricsUtil.getEnv(Const.ServerProperties.ENV_XSJ_LOCAL_PORT);
        if(MetricsUtil.isNotBlank(port)) { envXsjLocalPort = Integer.parseInt(port.trim()); }

        logger.info("[metrics][info] yidian_port=" + envXsjLocalPort);
        return envXsjLocalPort;
    }


    /**
     *
     * @param name
     * @return
     */
    private static String getEnv(String name) {
        String value = "";
        try {
            value = System.getenv(name);
        } catch (Throwable e) {
            // do nothing
        }

        return value;
    }

}
