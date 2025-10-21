package com.agentcore.cluster.loadbalance;

import com.agentcore.cluster.node.ClusterNode;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡器
 * 
 * @author AgentCore Team
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Optional<ClusterNode> select(List<ClusterNode> availableNodes, String key) {
        if (availableNodes == null || availableNodes.isEmpty()) {
            return Optional.empty();
        }

        int index = counter.getAndIncrement() % availableNodes.size();
        return Optional.of(availableNodes.get(index));
    }

    @Override
    public String getAlgorithmName() {
        return "RoundRobin";
    }

    @Override
    public void reset() {
        counter.set(0);
    }
}