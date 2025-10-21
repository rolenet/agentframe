package com.agentcore.container.registry;

import com.agentcore.container.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 本地服务注册实现
 * 
 * @author AgentCore Team
 */
public class LocalServiceRegistry implements ServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(LocalServiceRegistry.class);

    private final Map<String, Map<String, ServiceInfo>> services = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Boolean> registerService(ServiceInfo serviceInfo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                services.computeIfAbsent(serviceInfo.serviceName(), k -> new ConcurrentHashMap<>())
                    .put(serviceInfo.serviceId(), serviceInfo);
                
                logger.info("Service registered: {} - {}", serviceInfo.serviceName(), serviceInfo.serviceId());
                return true;
                
            } catch (Exception e) {
                logger.error("Failed to register service: {} - {}", serviceInfo.serviceName(), serviceInfo.serviceId(), e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> unregisterService(String serviceName, String serviceId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, ServiceInfo> serviceMap = services.get(serviceName);
                if (serviceMap != null) {
                    ServiceInfo removed = serviceMap.remove(serviceId);
                    if (removed != null) {
                        logger.info("Service unregistered: {} - {}", serviceName, serviceId);
                        
                        // 如果服务映射为空，则移除整个服务名称
                        if (serviceMap.isEmpty()) {
                            services.remove(serviceName);
                        }
                        return true;
                    }
                }
                
                logger.warn("Service not found for unregistration: {} - {}", serviceName, serviceId);
                return false;
                
            } catch (Exception e) {
                logger.error("Failed to unregister service: {} - {}", serviceName, serviceId, e);
                return false;
            }
        });
    }

    @Override
    public List<ServiceInfo> findServices(String serviceName) {
        Map<String, ServiceInfo> serviceMap = services.get(serviceName);
        if (serviceMap != null) {
            return List.copyOf(serviceMap.values());
        }
        return List.of();
    }

    @Override
    public Optional<ServiceInfo> getService(String serviceName, String serviceId) {
        Map<String, ServiceInfo> serviceMap = services.get(serviceName);
        if (serviceMap != null) {
            return Optional.ofNullable(serviceMap.get(serviceId));
        }
        return Optional.empty();
    }

    @Override
    public List<ServiceInfo> getAllServices() {
        return services.values().stream()
            .flatMap(serviceMap -> serviceMap.values().stream())
            .collect(Collectors.toList());
    }

    @Override
    public boolean containsService(String serviceName, String serviceId) {
        Map<String, ServiceInfo> serviceMap = services.get(serviceName);
        return serviceMap != null && serviceMap.containsKey(serviceId);
    }

    /**
     * 获取服务统计信息
     */
    public Map<String, Integer> getServiceStats() {
        return services.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().size()
            ));
    }

    /**
     * 清空所有服务
     */
    public void clear() {
        services.clear();
        logger.info("All services cleared from registry");
    }

    /**
     * 获取服务数量
     */
    public int getServiceCount() {
        return services.values().stream()
            .mapToInt(Map::size)
            .sum();
    }
}