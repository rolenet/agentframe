package com.agentcore.examples.aidb;

import com.agentcore.container.AgentContainer;
import com.agentcore.container.ContainerConfig;
import com.agentcore.container.DefaultAgentContainer;
import com.agentcore.core.agent.Agent;
import com.agentcore.core.agent.AgentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * AI数据库智能体系统 (问库智能体系统)
 * 
 * 系统包含以下智能体：
 * 1. AIDBAgent - 问库智能体，负责处理自然语言查询并生成SQL
 * 2. QueryClientAgent - 查询客户端智能体，用于发送查询请求
 * 
 * 功能特点：
 * - 通过Ollama大模型将自然语言转换为SQL查询
 * - 自动获取数据库元数据信息
 * - 安全的SQL执行（只允许SELECT查询）
 * - 格式化的查询结果展示
 * 
 * @author AgentCore Team
 */
public class AIDBSystem {

    private static final Logger logger = LoggerFactory.getLogger(AIDBSystem.class);

    public static void main(String[] args) {
        logger.info("=== AI数据库智能体系统 (问库智能体) 启动 ===");

        try {
            // 1. 创建容器配置
            ContainerConfig config = ContainerConfig.builder("AIDBContainer")
                .maxAgents(10)
                .agentStartTimeout(Duration.ofSeconds(30)) // 增加超时时间，因为需要初始化数据库连接
                .agentStopTimeout(Duration.ofSeconds(10))
                .autoStartAgents(true)
                .enableMonitoring(true)
                .property("system.name", "AI数据库智能体系统")
                .property("system.version", "1.0")
                .property("startup.time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

            logger.info("创建AI数据库容器配置: {}", config.name());

            // 2. 创建容器实例
            AgentContainer container = new DefaultAgentContainer(config);
            
            // 3. 设置事件监听器
            setupEventListeners(container);

            // 4. 启动容器
            logger.info("启动AI数据库系统容器...");
            container.start().join();
            logger.info("AI数据库系统容器启动成功");

            // 5. 创建并启动智能体
            setupAIDBAgents(container);

            // 6. 等待智能体初始化完成
            Thread.sleep(5000);

            // 7. 启动交互式查询模式
            startInteractiveMode(container);

            // 8. 停止容器
            logger.info("停止AI数据库系统容器...");
            container.stop().join();
            logger.info("AI数据库系统容器停止成功");

        } catch (Exception e) {
            logger.error("AI数据库系统运行过程中发生错误", e);
        }

        logger.info("=== AI数据库智能体系统结束 ===");
    }

    /**
     * 设置事件监听器
     */
    private static void setupEventListeners(AgentContainer container) {
        container.setAgentEventListener(new AgentContainer.AgentEventListener() {
            @Override
            public void onAgentAdded(Agent agent) {
                logger.info("🤖 智能体已添加: {}", agent.getAgentId().getShortId());
            }

            @Override
            public void onAgentRemoved(Agent agent) {
                logger.info("🤖 智能体已移除: {}", agent.getAgentId().getShortId());
            }

            @Override
            public void onAgentStarted(Agent agent) {
                logger.info("✅ 智能体已启动: {}", agent.getAgentId().getShortId());
            }

            @Override
            public void onAgentStopped(Agent agent) {
                logger.info("⏹️ 智能体已停止: {}", agent.getAgentId().getShortId());
            }

            @Override
            public void onAgentError(Agent agent, Throwable error) {
                logger.error("❌ 智能体错误: {}, 错误信息: {}", 
                    agent.getAgentId().getShortId(), error.getMessage());
            }
        });
    }

    /**
     * 创建并设置AI数据库智能体
     */
    private static void setupAIDBAgents(AgentContainer container) throws Exception {
        logger.info("创建AI数据库智能体...");

        // 创建智能体ID
        AgentId aidbAgentId = AgentId.create("AIDBAgent");
        AgentId clientAgentId = AgentId.create("QueryClientAgent");

        // 创建智能体实例
        AIDBAgent aidbAgent = container.createAgent(AIDBAgent.class, aidbAgentId);
        QueryClientAgent clientAgent = container.createAgent(QueryClientAgent.class, clientAgentId);

        // 设置智能体之间的关系
        clientAgent.setAIDBAgentId(aidbAgentId);

        logger.info("AI数据库智能体创建完成");
        logger.info("🔗 智能体关系: 查询客户端 -> 问库智能体");
    }

    /**
     * 启动交互式查询模式
     */
    private static void startInteractiveMode(AgentContainer container) {
        logger.info("🚀 启动交互式查询模式...");
        
        // 获取问库智能体
        Agent aidbAgent = container.getAgent("AIDBAgent");
        Agent clientAgent = container.getAgent("QueryClientAgent");
        
        if (!(aidbAgent instanceof AIDBAgent) || !(clientAgent instanceof QueryClientAgent)) {
            logger.error("❌ 智能体未正确创建，无法启动交互模式");
            return;
        }
        
        AIDBAgent aidb = (AIDBAgent) aidbAgent;
        QueryClientAgent client = (QueryClientAgent) clientAgent;
        
        // 显示使用说明
        showInteractiveInstructions();
        
        // 启动控制台输入循环
        Scanner scanner = new Scanner(System.in);
        String input;
        
        while (true) {
            System.out.print("\n🤖 请输入查询问题 (输入 'quit' 退出, 'help' 查看帮助, 'demo' 运行演示): ");
            input = scanner.nextLine().trim();
            
            if ("quit".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                logger.info("🤖 用户选择退出交互模式");
                break;
            } else if ("help".equalsIgnoreCase(input)) {
                showInteractiveInstructions();
                continue;
            } else if ("demo".equalsIgnoreCase(input)) {
                runDemo(client);
                continue;
            } else if (input.isEmpty()) {
                continue;
            }
            
            // 发送查询请求
            logger.info("🤖 处理用户查询: {}", input);
            client.sendQuery(input);
            
            // 等待响应
            try {
                Thread.sleep(3000); // 等待3秒让响应处理完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        scanner.close();
        logger.info("🤖 交互式查询模式结束");
    }
    
    /**
     * 显示交互式使用说明
     */
    private static void showInteractiveInstructions() {
        System.out.println("\n🤖 ==================== AI数据库智能体使用说明 ====================");
        System.out.println("🤖 欢迎使用问库智能体！您可以用自然语言查询数据库。");
        System.out.println("🤖 ");
        System.out.println("🤖 查询示例:");
        System.out.println("🤖   - 查询所有表的信息");
        System.out.println("🤖   - 显示用户表的结构");
        System.out.println("🤖   - 统计数据库中有多少个表");
        System.out.println("🤖   - 系统异常日志有多少记录");
        System.out.println("🤖   - 显示所有表名");
        System.out.println("🤖 ");
        System.out.println("🤖 特殊命令:");
        System.out.println("🤖   - help: 显示此帮助信息");
        System.out.println("🤖   - demo: 运行自动演示");
        System.out.println("🤖   - quit/exit: 退出系统");
        System.out.println("🤖 ");
        System.out.println("🤖 注意: 系统只支持SELECT查询，不允许修改数据库内容");
        System.out.println("🤖 ================================================================");
    }
    
    /**
     * 运行演示
     */
    private static void runDemo(QueryClientAgent client) {
        logger.info("🤖 开始运行演示...");
        
        String[] demoQueries = {
                "显示数据库中所有表的名称",
                "查询表的结构信息",
                "统计数据库中表的数量"
        };
        
        for (String query : demoQueries) {
            logger.info("🤖 演示查询: {}", query);
            client.sendQuery(query);
            
            try {
                Thread.sleep(4000); // 等待响应
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        logger.info("🤖 演示完成");
    }
}