package com.agentcore.spring.autoconfigure;

import com.agentcore.cluster.ClusterManager;
import com.agentcore.cluster.DefaultClusterManager;
import com.agentcore.cluster.discovery.MulticastNodeDiscovery;
import com.agentcore.cluster.discovery.NodeDiscovery;
import com.agentcore.cluster.discovery.RedisNodeDiscovery;
import com.agentcore.cluster.health.ClusterHealthChecker;
import com.agentcore.communication.serializer.JacksonMessageSerializer;
import com.agentcore.communication.serializer.KryoMessageSerializer;
import com.agentcore.communication.serializer.MessageSerializer;
import com.agentcore.communication.transport.DefaultTransportConfig;
import com.agentcore.communication.transport.LocalTransport;
import com.agentcore.communication.transport.NettyTcpTransport;
import com.agentcore.communication.transport.Transport;
import com.agentcore.communication.transport.TransportConfig;
import com.agentcore.container.AgentContainer;
import com.agentcore.container.ContainerConfig;
import com.agentcore.container.DefaultAgentContainer;
import com.agentcore.container.DefaultContainerConfig;
import com.agentcore.container.ServiceRegistry;
import com.agentcore.container.registry.LocalServiceRegistry;
import com.agentcore.spring.properties.AgentCoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * AgentCore自动配置类
 * 
 * @author AgentCore Team
 */
@AutoConfiguration
@EnableConfigurationProperties(AgentCoreProperties.class)
@ConditionalOnClass(AgentContainer.class)
public class AgentCoreAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AgentCoreAutoConfiguration.class);

    /**
     * 消息序列化器配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "agentcore.communication", name = "serializer", havingValue = "jackson", matchIfMissing = true)
    static class JacksonSerializerConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public MessageSerializer messageSerializer() {
            logger.info("Configuring Jackson message serializer");
            return new JacksonMessageSerializer();
        }
    }

    @Configuration
    @ConditionalOnProperty(prefix = "agentcore.communication", name = "serializer", havingValue = "kryo")
    static class KryoSerializerConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public MessageSerializer messageSerializer() {
            logger.info("Configuring Kryo message serializer");
            return new KryoMessageSerializer();
        }
    }

    /**
     * 传输层配置
     */
    @Configuration
    static class TransportConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "agentcore.communication", name = "transport", havingValue = "tcp")
        public Transport tcpTransport(AgentCoreProperties properties, MessageSerializer messageSerializer) {
            logger.info("Configuring Netty TCP transport on port: {}", 
                properties.getCommunication().getPort());
            TransportConfig config = new DefaultTransportConfig(
                "tcp-transport",
                Transport.TransportType.TCP,
                properties.getCommunication().getHost(),
                properties.getCommunication().getPort()
            );
            return new NettyTcpTransport(config, messageSerializer);
        }
        
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "agentcore.communication", name = "transport", havingValue = "local", matchIfMissing = true)
        public Transport localTransport() {
            logger.info("Configuring local transport");
            return new LocalTransport();
        }
    }

    /**
     * 服务注册配置
     */
    @Configuration
    static class ServiceRegistryConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public ServiceRegistry serviceRegistry() {
            logger.info("Configuring local service registry");
            return new LocalServiceRegistry();
        }
    }

    /**
     * Agent容器配置
     */
    @Configuration
    static class ContainerConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public AgentContainer agentContainer(AgentCoreProperties properties) {
            logger.info("Configuring default agent container");
            ContainerConfig config = DefaultContainerConfig.create(
                properties.getContainer().getName()
            );
            return new DefaultAgentContainer(config);
        }
    }

    /**
     * 集群管理配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "agentcore.cluster", name = "enabled", havingValue = "true")
    static class ClusterConfiguration {

        /**
         * 多播节点发现配置
         */
        @Configuration
        @ConditionalOnProperty(prefix = "agentcore.cluster.discovery", name = "type", havingValue = "multicast", matchIfMissing = true)
        static class MulticastDiscoveryConfiguration {
            
            @Bean
            @ConditionalOnMissingBean
            public NodeDiscovery nodeDiscovery(AgentCoreProperties properties) {
                logger.info("Configuring multicast node discovery");
                return new MulticastNodeDiscovery(
                    properties.getCluster().getDiscovery().getMulticastGroup(),
                    properties.getCluster().getDiscovery().getMulticastPort()
                );
            }
        }

        /**
         * Redis节点发现配置
         */
        @Configuration
        @ConditionalOnProperty(prefix = "agentcore.cluster.discovery", name = "type", havingValue = "redis")
        @ConditionalOnClass({RedisTemplate.class, RedisMessageListenerContainer.class})
        static class RedisDiscoveryConfiguration {
            
            @Bean
            @ConditionalOnMissingBean
            public NodeDiscovery nodeDiscovery(RedisTemplate<String, String> redisTemplate,
                                             RedisMessageListenerContainer messageListenerContainer) {
                logger.info("Configuring Redis node discovery");
                return new RedisNodeDiscovery(redisTemplate, messageListenerContainer);
            }
        }

        /**
         * 集群管理器配置
         */
        @Bean
        @ConditionalOnMissingBean
        public ClusterManager clusterManager(AgentCoreProperties properties, NodeDiscovery nodeDiscovery) {
            logger.info("Configuring cluster manager");
            return new DefaultClusterManager(properties.getCluster().toClusterConfig(), nodeDiscovery);
        }

        /**
         * 集群健康检查配置
         */
        @Bean
        @ConditionalOnProperty(prefix = "agentcore.cluster.health", name = "enabled", havingValue = "true", matchIfMissing = true)
        public ClusterHealthChecker clusterHealthChecker(ClusterManager clusterManager,
                                                       AgentCoreProperties properties) {
            logger.info("Configuring cluster health checker");
            return new ClusterHealthChecker(
                clusterManager,
                properties.getCluster().getHealth().getCheckInterval(),
                properties.getCluster().getHealth().toHealthCheckConfig()
            );
        }
    }

    // 监控配置暂时移除，等待Micrometer依赖问题解决

    // 管理端点和健康检查配置暂时移除，等待Actuator依赖问题解决
}