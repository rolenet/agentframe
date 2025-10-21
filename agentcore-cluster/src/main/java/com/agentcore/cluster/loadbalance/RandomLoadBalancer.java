package com.agentcore.cluster.loadbalance;

import com.agentcore.cluster.node.ClusterNode;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 随机负载均衡器
 * 
 * @author AgentCore Team
 */
public class RandomLoadBalancer implements LoadBalancer {

    private final Random random = new Random();

    @Override
    public Optional<ClusterNode> select(List<ClusterNode> availableNodes, String key) {
        if (availableNodes == null || availableNodes.isEmpty()) {
            return Optional.empty();
        }

        int index = random.nextInt(availableNodes.size());
        return Optional.of(availableNodes.get(index));
    }

    @Override
    public String getAlgorithmName() {
        return "Random";
    }
}