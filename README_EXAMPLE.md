# AgentCore 示例程序

本模块包含 AgentCore 框架的功能演示程序，展示了 Agent 的基本功能：启动、发送消息、处理消息。

## 示例程序列表

### 1. SimpleAgentExample - 简单Agent示例
- **文件**: `src/main/java/com/agentcore/examples/SimpleAgentExample.java`
- **功能**: 演示最基本的Agent创建、启动、停止流程
- **特点**: 
  - 单个Agent的完整生命周期管理
  - 简单的消息发送和处理
  - 适合初学者理解Agent核心概念

**运行方式**:
```bash
cd agentcore-examples
mvn compile exec:java -Dexec.mainClass="com.agentcore.examples.SimpleAgentExample"
```

### 2. BasicAgentDemo - 基础Agent功能演示
- **文件**: `src/main/java/com/agentcore/examples/BasicAgentDemo.java`
- **功能**: 演示多个Agent之间的完整消息交互流程
- **特点**:
  - 发送者Agent和接收者Agent的协作
  - 定时消息发送机制
  - 消息回复处理
  - 完整的消息生命周期演示

**运行方式**:
```bash
cd agentcore-examples
mvn compile exec:java -Dexec.mainClass="com.agentcore.examples.BasicAgentDemo"
```

### 3. SpringAgentDemo - Spring Boot集成演示
- **文件**: `src/main/java/com/agentcore/examples/SpringAgentDemo.java`
- **功能**: 演示在Spring Boot环境中使用AgentCore框架
- **特点**:
  - Spring Boot应用集成
  - Spring配置管理
  - 依赖注入支持
  - 生产环境就绪的Agent管理

**运行方式**:
```bash
cd agentcore-examples
mvn spring-boot:run -Dspring-boot.run.main-class=com.agentcore.examples.SpringAgentDemo
```

## 核心功能演示

### Agent启动流程
1. 创建Agent ID
2. 实例化Agent对象
3. 调用`start()`方法启动
4. Agent进入运行状态，可以接收和处理消息

### 消息发送流程
1. 创建`AgentMessage`对象，指定目标Agent ID和消息内容
2. 调用`sendMessage()`方法发送
3. 消息通过通信层路由到目标Agent
4. 目标Agent的`doHandleMessage()`方法被调用

### 消息处理流程
1. Agent接收到消息
2. 调用`doHandleMessage()`方法处理消息内容
3. 可选：发送回复消息给发送者
4. 消息处理完成

## 配置说明

### 应用配置
配置文件: `src/main/resources/application.yml`

主要配置项:
- **Redis连接**: 用于Agent之间的消息通信
- **Agent容器**: 工作线程和队列配置
- **集群配置**: 集群名称和节点发现
- **通信协议**: 消息序列化和传输协议

### 依赖关系
- `agentcore-spring-boot-starter`: AgentCore核心功能
- `spring-boot-starter-web`: Web支持
- `spring-boot-starter-data-redis`: Redis通信支持

## 运行要求

### 环境要求
- Java 8+
- Maven 3.6+
- Redis服务器（用于消息通信）

### 快速启动Redis
```bash
# 使用Docker启动Redis
docker run -d -p 6379:6379 redis:latest

# 或者使用系统包管理器安装
```

## 演示输出示例

运行BasicAgentDemo的典型输出:
```
=== 开始Agent基础功能演示 ===
创建Agent: SenderAgent 和 ReceiverAgent
所有Agent已启动
发送者 SenderAgent 发送消息: 这是第 1 条消息，来自发送者 SenderAgent
接收者 ReceiverAgent 收到消息: 这是第 1 条消息，来自发送者 SenderAgent
发送者 SenderAgent 收到回复消息: 收到你的消息，这是来自接收者 ReceiverAgent 的回复
所有Agent已停止
=== Agent基础功能演示完成 ===
```

## 扩展开发

基于这些示例，您可以:
1. 创建自定义的Agent类型
2. 实现复杂的消息处理逻辑
3. 集成到现有的Spring Boot应用中
4. 构建分布式的Agent系统

## 故障排除

### 常见问题
1. **Redis连接失败**: 确保Redis服务器正在运行且可访问
2. **Agent启动失败**: 检查Agent ID是否唯一，配置是否正确
3. **消息发送失败**: 确认目标Agent已启动且ID正确

### 调试模式
启用DEBUG级别日志查看详细运行信息:
```yaml
logging:
  level:
    com.agentcore: DEBUG