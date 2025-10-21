package com.agentcore.container;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 服务注册接口
 * 
 * @author AgentCore Team
 */
public interface ServiceRegistry {

    /**
     * 注册服务
     * 
     * @param serviceInfo 服务信息
     * @return 注册结果
     */
    CompletableFuture<Boolean> registerService(ServiceInfo serviceInfo);

    /**
     * 注销服务
     * 
     * @param serviceName 服务名称
     * @param serviceId 服务ID
     * @return 注销结果
     */
    CompletableFuture<Boolean> unregisterService(String serviceName, String serviceId);

    /**
     * 查找服务
     * 
     * @param serviceName 服务名称
     * @return 服务信息列表
     */
    List<ServiceInfo> findServices(String serviceName);

    /**
     * 获取服务
     * 
     * @param serviceName 服务名称
     * @param serviceId 服务ID
     * @return 服务信息
     */
    Optional<ServiceInfo> getService(String serviceName, String serviceId);

    /**
     * 获取所有服务
     * 
     * @return 所有服务信息
     */
    List<ServiceInfo> getAllServices();

    /**
     * 检查服务是否存在
     * 
     * @param serviceName 服务名称
     * @param serviceId 服务ID
     * @return 是否存在
     */
    boolean containsService(String serviceName, String serviceId);

    /**
     * 服务信息记录
     */
    record ServiceInfo(
        String serviceName,
        String serviceId,
        String host,
        int port,
        String protocol,
        String version,
        long registerTime,
        java.util.Map<String, String> metadata
    ) {}
}