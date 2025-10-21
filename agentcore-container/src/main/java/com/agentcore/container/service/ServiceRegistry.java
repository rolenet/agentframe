package com.agentcore.container.service;

import com.agentcore.core.agent.AgentId;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 服务注册接口
 * 负责Agent服务的注册、发现和管理
 * 
 * @author AgentCore Team
 */
public interface ServiceRegistry {

    /**
     * 注册服务
     * 
     * @param service 服务描述
     * @return CompletableFuture，完成时表示注册成功
     */
    CompletableFuture<Void> registerService(ServiceDescriptor service);

    /**
     * 注销服务
     * 
     * @param agentId Agent ID
     * @param serviceName 服务名称
     * @return CompletableFuture，完成时表示注销成功
     */
    CompletableFuture<Void> unregisterService(AgentId agentId, String serviceName);

    /**
     * 注销Agent的所有服务
     * 
     * @param agentId Agent ID
     * @return CompletableFuture，完成时表示注销成功
     */
    CompletableFuture<Void> unregisterAllServices(AgentId agentId);

    /**
     * 根据服务名称查找服务
     * 
     * @param serviceName 服务名称
     * @return 服务描述列表
     */
    CompletableFuture<List<ServiceDescriptor>> findServices(String serviceName);

    /**
     * 根据服务类型查找服务
     * 
     * @param serviceType 服务类型
     * @return 服务描述列表
     */
    CompletableFuture<List<ServiceDescriptor>> findServicesByType(String serviceType);

    /**
     * 根据Agent ID查找服务
     * 
     * @param agentId Agent ID
     * @return 服务描述列表
     */
    CompletableFuture<List<ServiceDescriptor>> findServicesByAgent(AgentId agentId);

    /**
     * 获取所有服务名称
     * 
     * @return 服务名称集合
     */
    CompletableFuture<Set<String>> getAllServiceNames();

    /**
     * 获取所有服务类型
     * 
     * @return 服务类型集合
     */
    CompletableFuture<Set<String>> getAllServiceTypes();

    /**
     * 检查服务是否存在
     * 
     * @param serviceName 服务名称
     * @return 如果存在返回true
     */
    CompletableFuture<Boolean> serviceExists(String serviceName);

    /**
     * 获取服务数量
     * 
     * @return 服务数量
     */
    CompletableFuture<Integer> getServiceCount();

    /**
     * 添加服务变更监听器
     * 
     * @param listener 监听器
     */
    void addServiceChangeListener(ServiceChangeListener listener);

    /**
     * 移除服务变更监听器
     * 
     * @param listener 监听器
     */
    void removeServiceChangeListener(ServiceChangeListener listener);

    /**
     * 服务变更监听器接口
     */
    interface ServiceChangeListener {
        /**
         * 服务注册事件
         * 
         * @param service 注册的服务
         */
        default void onServiceRegistered(ServiceDescriptor service) {}

        /**
         * 服务注销事件
         * 
         * @param service 注销的服务
         */
        default void onServiceUnregistered(ServiceDescriptor service) {}

        /**
         * 服务更新事件
         * 
         * @param oldService 旧服务描述
         * @param newService 新服务描述
         */
        default void onServiceUpdated(ServiceDescriptor oldService, ServiceDescriptor newService) {}
    }
}