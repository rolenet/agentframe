package com.agentcore.container.service;

import com.agentcore.core.agent.AgentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 本地服务注册实现
 * 
 * @author AgentCore Team
 */
public class LocalServiceRegistry implements ServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(LocalServiceRegistry.class);

    // 服务存储：serviceId -> ServiceDescriptor
    private final ConcurrentHashMap<String, ServiceDescriptor> services = new ConcurrentHashMap<>();
    
    // 按服务名称索引：serviceName -> Set<serviceId>
    private final ConcurrentHashMap<String, Set<String>> serviceNameIndex = new ConcurrentHashMap<>();
    
    // 按服务类型索引：serviceType -> Set<serviceId>
    private final ConcurrentHashMap<String, Set<String>> serviceTypeIndex = new ConcurrentHashMap<>();
    
    // 按Agent ID索引：agentId -> Set<serviceId>
    private final ConcurrentHashMap<String, Set<String>> agentIndex = new ConcurrentHashMap<>();
    
    // 服务变更监听器
    private final List<ServiceChangeListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public CompletableFuture<Void> registerService(ServiceDescriptor service) {
        return CompletableFuture.runAsync(() -> {
            String serviceId = service.getServiceId();
            String agentIdStr = service.agentId().getFullId();
            
            logger.debug("Registering service: {}", serviceId);
            
            // 检查是否已存在
            ServiceDescriptor existingService = services.get(serviceId);
            if (existingService != null) {
                // 更新现有服务
                services.put(serviceId, service);
                notifyServiceUpdated(existingService, service);
                logger.debug("Updated existing service: {}", serviceId);
            } else {
                // 注册新服务
                services.put(serviceId, service);
                
                // 更新索引
                serviceNameIndex.computeIfAbsent(service.serviceName(), k -> ConcurrentHashMap.newKeySet())
                    .add(serviceId);
                serviceTypeIndex.computeIfAbsent(service.serviceType(), k -> ConcurrentHashMap.newKeySet())
                    .add(serviceId);
                agentIndex.computeIfAbsent(agentIdStr, k -> ConcurrentHashMap.newKeySet())
                    .add(serviceId);
                
                notifyServiceRegistered(service);
                logger.info("Registered new service: {}", serviceId);
            }
        });
    }

    @Override
    public CompletableFuture<Void> unregisterService(AgentId agentId, String serviceName) {
        return CompletableFuture.runAsync(() -> {
            String serviceId = agentId.getFullId() + ":" + serviceName;
            
            logger.debug("Unregistering service: {}", serviceId);
            
            ServiceDescriptor service = services.remove(serviceId);
            if (service != null) {
                // 从索引中移除
                removeFromIndex(serviceNameIndex, service.serviceName(), serviceId);
                removeFromIndex(serviceTypeIndex, service.serviceType(), serviceId);
                removeFromIndex(agentIndex, agentId.getFullId(), serviceId);
                
                notifyServiceUnregistered(service);
                logger.info("Unregistered service: {}", serviceId);
            } else {
                logger.debug("Service not found for unregistration: {}", serviceId);
            }
        });
    }

    @Override
    public CompletableFuture<Void> unregisterAllServices(AgentId agentId) {
        return CompletableFuture.runAsync(() -> {
            String agentIdStr = agentId.getFullId();
            
            logger.debug("Unregistering all services for agent: {}", agentIdStr);
            
            Set<String> serviceIds = agentIndex.remove(agentIdStr);
            if (serviceIds != null) {
                for (String serviceId : serviceIds) {
                    ServiceDescriptor service = services.remove(serviceId);
                    if (service != null) {
                        // 从其他索引中移除
                        removeFromIndex(serviceNameIndex, service.serviceName(), serviceId);
                        removeFromIndex(serviceTypeIndex, service.serviceType(), serviceId);
                        
                        notifyServiceUnregistered(service);
                    }
                }
                logger.info("Unregistered {} services for agent: {}", serviceIds.size(), agentIdStr);
            } else {
                logger.debug("No services found for agent: {}", agentIdStr);
            }
        });
    }

    @Override
    public CompletableFuture<List<ServiceDescriptor>> findServices(String serviceName) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> serviceIds = serviceNameIndex.get(serviceName);
            if (serviceIds == null || serviceIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            return serviceIds.stream()
                .map(services::get)
                .filter(Objects::nonNull)
                .filter(ServiceDescriptor::isAvailable)
                .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<List<ServiceDescriptor>> findServicesByType(String serviceType) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> serviceIds = serviceTypeIndex.get(serviceType);
            if (serviceIds == null || serviceIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            return serviceIds.stream()
                .map(services::get)
                .filter(Objects::nonNull)
                .filter(ServiceDescriptor::isAvailable)
                .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<List<ServiceDescriptor>> findServicesByAgent(AgentId agentId) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> serviceIds = agentIndex.get(agentId.getFullId());
            if (serviceIds == null || serviceIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            return serviceIds.stream()
                .map(services::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<Set<String>> getAllServiceNames() {
        return CompletableFuture.supplyAsync(() -> 
            new HashSet<>(serviceNameIndex.keySet()));
    }

    @Override
    public CompletableFuture<Set<String>> getAllServiceTypes() {
        return CompletableFuture.supplyAsync(() -> 
            new HashSet<>(serviceTypeIndex.keySet()));
    }

    @Override
    public CompletableFuture<Boolean> serviceExists(String serviceName) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> serviceIds = serviceNameIndex.get(serviceName);
            return serviceIds != null && !serviceIds.isEmpty();
        });
    }

    @Override
    public CompletableFuture<Integer> getServiceCount() {
        return CompletableFuture.supplyAsync(services::size);
    }

    @Override
    public void addServiceChangeListener(ServiceChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
            logger.debug("Added service change listener: {}", listener.getClass().getSimpleName());
        }
    }

    @Override
    public void removeServiceChangeListener(ServiceChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            logger.debug("Removed service change listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * 从索引中移除服务ID
     * 
     * @param index 索引Map
     * @param key 索引键
     * @param serviceId 服务ID
     */
    private void removeFromIndex(ConcurrentHashMap<String, Set<String>> index, String key, String serviceId) {
        Set<String> serviceIds = index.get(key);
        if (serviceIds != null) {
            serviceIds.remove(serviceId);
            if (serviceIds.isEmpty()) {
                index.remove(key);
            }
        }
    }

    /**
     * 通知服务注册事件
     */
    private void notifyServiceRegistered(ServiceDescriptor service) {
        for (ServiceChangeListener listener : listeners) {
            try {
                listener.onServiceRegistered(service);
            } catch (Exception e) {
                logger.error("Error in service registered listener", e);
            }
        }
    }

    /**
     * 通知服务注销事件
     */
    private void notifyServiceUnregistered(ServiceDescriptor service) {
        for (ServiceChangeListener listener : listeners) {
            try {
                listener.onServiceUnregistered(service);
            } catch (Exception e) {
                logger.error("Error in service unregistered listener", e);
            }
        }
    }

    /**
     * 通知服务更新事件
     */
    private void notifyServiceUpdated(ServiceDescriptor oldService, ServiceDescriptor newService) {
        for (ServiceChangeListener listener : listeners) {
            try {
                listener.onServiceUpdated(oldService, newService);
            } catch (Exception e) {
                logger.error("Error in service updated listener", e);
            }
        }
    }

    /**
     * 获取所有服务（用于测试和调试）
     * 
     * @return 所有服务的副本
     */
    public List<ServiceDescriptor> getAllServices() {
        return new ArrayList<>(services.values());
    }

    /**
     * 清空所有服务（用于测试）
     */
    public void clear() {
        services.clear();
        serviceNameIndex.clear();
        serviceTypeIndex.clear();
        agentIndex.clear();
        logger.debug("Cleared all services from registry");
    }
}