package com.agentcore.cluster.loadbalance;

import com.agentcore.cluster.node.ClusterNode;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 最低负载负载均衡器
 * 
 * @author AgentCore Team
 */
public class LeastLoadLoadBalancer implements LoadBalancer {

    @Override
    public Optional<ClusterNode> select(List<ClusterNode> availableNodes, String key) {
        if (availableNodes == null || availableNodes.isEmpty()) {
            return Optional.empty();
        }

        return availableNodes.stream()
            .min(Comparator.comparingDouble(node -> node.load().getLoadScore()));
    }

    @Override
    public String getAlgorithmName() {
        return "LeastLoad";
    }
}