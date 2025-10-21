# AgentFrame - 多智能体协调框架

AgentFrame是一个基于Java 17和Spring Boot 3.5.6的现代化多智能体协调框架，提供高性能、可扩展的智能体系统开发平台。

## 🚀 核心特性

- **智能体生命周期管理** - 完整的Agent创建、启动、暂停、停止和销毁流程
- **高性能异步通信** - 基于Netty的高性能消息传递，支持ACL消息格式
- **分布式集群支持** - 内置集群管理，支持节点发现、负载均衡和故障转移
- **服务发现与注册** - 灵活的服务注册和发现机制
- **Spring Boot集成** - 无缝集成Spring Boot生态系统
- **监控和扩展** - 基于Micrometer的监控指标收集
- **模块化架构** - 清晰的分层架构，支持灵活扩展

## 📋 系统要求

- Java 17+
- Maven 3.8+
- Spring Boot 3.5.6+

## 🏗️ 架构设计

AgentCore采用分层架构设计：

```
┌─────────────────────────────────────────┐
│              应用层 (Examples)           │
├─────────────────────────────────────────┤
│           集成层 (Spring Boot)           │
├─────────────────────────────────────────┤
│            集群层 (Cluster)             │
├─────────────────────────────────────────┤
│            容器层 (Container)            │
├─────────────────────────────────────────┤
│           通信层 (Communication)         │
├─────────────────────────────────────────┤
│             核心层 (Core)               │
└─────────────────────────────────────────┘
```

### 模块说明

- **agentcore-core**: 核心Agent抽象、行为模型、消息系统
- **agentcore-communication**: 消息序列化、传输层实现
- **agentcore-container**: Agent容器、服务注册发现
- **agentcore-cluster**: 集群管理、节点协调
- **agentcore-spring-boot-starter**: Spring Boot自动配置
- **agentcore-examples**: 示例应用和使用案例

## 🚀 快速开始

### 1. 添加依赖

在你的`pom.xml`中添加AgentCore依赖：

```xml
<dependency>
    <groupId>com.agentcore</groupId>
    <artifactId>agentcore-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 创建Agent

```java
@Component
public class MyAgent extends AbstractAgent {
    
    public MyAgent() {
        super(AgentId.create("MyAgent"));
    }
    
    @Override
    protected CompletableFuture<Void> doStart() {
        // 添加行为
        addBehavior(new CyclicBehavior() {
            @Override
            public void action() {
                // 执行周期性任务
                logger.info("Agent {} is working...", getAgentId().getShortId());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    protected void handleMessage(AgentMessage message) {
        // 处理接收到的消息
        logger.info("Received message: {}", message.content());
    }
}
```

### 3. 配置应用

在`application.yml`中配置AgentCore：

```yaml
agentcore:
  container:
    name: MyAgentContainer
    max-agents: 100
  communication:
    transport: local
    serializer: jackson
  monitoring:
    enabled: true
```

### 4. 启动应用

```java
@SpringBootApplication
public class MyAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyAgentApplication.class, args);
    }
}
```

## 📚 示例应用

### 基本示例

运行基本Agent示例：

```bash
cd agentcore-examples
mvn exec:java -Dexec.mainClass="com.agentcore.examples.basic.BasicAgentExample"
```

### Spring Boot示例

启动Spring Boot集成示例：

```bash
cd agentcore-examples
mvn spring-boot:run -Dspring-boot.run.main-class="com.agentcore.examples.springboot.SpringBootAgentApplication"
```

访问 http://localhost:8080/api/agents/status 查看Agent状态。

### 集群示例

启动多个集群节点：

```bash
# 节点1
mvn exec:java -Dexec.mainClass="com.agentcore.examples.cluster.ClusterAgentExample" -Dexec.args="node1 8080"

# 节点2
mvn exec:java -Dexec.mainClass="com.agentcore.examples.cluster.ClusterAgentExample" -Dexec.args="node2 8081"

# 节点3
mvn exec:java -Dexec.mainClass="com.agentcore.examples.cluster.ClusterAgentExample" -Dexec.args="node3 8082"
```

## 🔧 核心概念

### Agent生命周期

Agent具有以下状态：
- `CREATED` - 已创建
- `STARTING` - 启动中
- `ACTIVE` - 活跃状态
- `SUSPENDED` - 暂停状态
- `STOPPING` - 停止中
- `TERMINATED` - 已终止

### 行为模型

AgentCore支持多种行为类型：

- **OneShotBehavior** -一次行为
- **CyclicBehavior** - 循环行为
- **TickerBehavior** - 定时行为

```java
// 一次行为
addBehavior(new OneShotBehavior() {
    @Override
    public void action() {
        // 执行一次任务
    }
});

// 循环行为
addBehavior(new CyclicBehavior() {
    @Override
    public void action() {
        // 循环执行的任务
    }
});

// 定时行为
addBehavior(new TickerBehavior(Duration.ofSeconds(10)) {
    @Override
    protected void onTick() {
        // 每10秒执行一次
    }
});
```

### 消息通信

Agent间通过ACL消息进行通信：

```java
AgentMessage message = AgentMessage.builder()
    .sender(getAgentId())
    .receiver(targetAgentId)
    .performative(MessagePerformative.REQUEST)
    .content("Hello, World!")
    .build();

sendMessage(message);
```

支持的消息类型：
- `INFORM` - 信息通知
- `REQUEST` - 请求
- `QUERY` - 查询
- `CONFIRM` - 确认
- `FAILURE` - 失败通知

## 🔧 配置选项

### 容器配置

```yaml
agentcore:
  container:
    name: MyContainer              # 容器名称
    max-agents: 1000              # 最大Agent数量
    auto-start-agents: true       # 自动启动Agent
    enable-monitoring: true       # 启用监控
```

### 通信配置

```yaml
agentcore:
  communication:
    transport: tcp                # 传输类型: local, tcp
    host: localhost              # 主机地址
    port: 9090                   # 端口号
    serializer: jackson          # 序列化器: jackson, kryo
```

### 集群配置

```yaml
agentcore:
  cluster:
    enabled: true                # 启用集群
    node-name: node1            # 节点名称
    bind-host: localhost        # 绑定地址
    bind-port: 8080            # 绑定端口
    discovery:
      type: multicast          # 发现类型: multicast, redis
      multicast-group: 224.0.0.1
      multicast-port: 9999
```

## 📊 监控和管理

AgentCore集成了Spring Boot Actuator，提供丰富的监控端点：

- `/actuator/health` - 健康检查
- `/actuator/metrics` - 监控指标
- `/actuator/agentcore` - Agent状态信息
- `/actuator/cluster` - 集群状态信息

### 自定义指标

```java
@Autowired
private MeterRegistry meterRegistry;

// 计数器
Counter.builder("agent.messages.processed")
    .register(meterRegistry)
    .increment();

// 计时器
Timer.Sample sample = Timer.start(meterRegistry);
// ... 执行操作
sample.stop(Timer.builder("agent.operation.duration").register(meterRegistry));
```

## 🧪 测试

运行所有测试：

```bash
mvn test
```

运行特定模块测试：

```bash
mvn test -pl agentcore-core
```

测试文件说明：

- 1、AI数据库智能体系统 (问库智能体)示例程序
- 2、AgentCore 示例程序
- 3、AgentCore框架综合系统测试
- 4、AgentCore 容器测试说明
- 5、AgentCore 扩展测试功能总结
- 6、AgentCore 容器和智能体测试总结

## 📖 API文档

### Agent接口

```java
public interface Agent {
    AgentId getAgentId();
    AgentState getState();
    CompletableFuture<Void> start();
    CompletableFuture<Void> stop();
    CompletableFuture<Void> suspend();
    CompletableFuture<Void> resume();
    void sendMessage(AgentMessage message);
    void addBehavior(Behavior behavior);
    void removeBehavior(Behavior behavior);
}
```

### 容器接口

```java
public interface AgentContainer {
    CompletableFuture<Void> start();
    CompletableFuture<Void> stop();
    CompletableFuture<Void> registerAgent(Agent agent);
    CompletableFuture<Void> unregisterAgent(AgentId agentId);
    CompletableFuture<Void> startAgent(AgentId agentId);
    CompletableFuture<Void> stopAgent(AgentId agentId);
    List<Agent> getAllAgents();
    int getAgentCount();
}
```

## 🤝 贡献指南

1. Fork项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

## 📄 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- [JADE](https://jade.tilab.com/) - 启发了Agent通信模型
- [Spring Boot](https://spring.io/projects/spring-boot) - 提供了优秀的应用框架
- [Netty](https://netty.io/) - 高性能网络通信框架
- [Micrometer](https://micrometer.io/) - 监控指标收集

## 📞 联系我们

- 项目主页: https://gitee.com/CocoLiu/agentframe
- 问题反馈: https://gitee.com/CocoLiu/agentframe/issues
- 邮箱: cocoliu2004@gmail.com

---

**AgentCore** - 让多智能体系统开发变得简单而强大！