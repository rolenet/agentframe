# AgentCore 容器测试说明

本文档说明如何在 agentcore-examples 模块中运行容器测试，展示 AgentContainer 的所有功能。

## 测试文件说明

### 1. ContainerTestDemo.java
**基础容器功能测试**
- 展示容器的基本生命周期管理
- Agent的创建、添加、移除操作
- 消息发送和接收功能
- 事件监听器设置
- 容器统计信息获取

### 2. AdvancedContainerTest.java
**高级容器功能测试**
- 错误处理和恢复机制
- 性能监控和统计
- 配置管理功能
- 大规模Agent管理
- 多种测试场景组合

### 3. ComprehensiveContainerTest.java
**综合性容器测试**
- 完整的容器功能覆盖
- 详细的事件监听和状态监控
- 消息路由和通信测试
- 错误处理和稳定性验证
- 大规模Agent管理测试

## 运行测试

### 方法1: 使用 Maven 运行单个测试
```bash
# 运行基础容器测试
mvn exec:java -Dexec.mainClass="com.agentcore.examples.ContainerTestDemo"

# 运行高级容器测试
mvn exec:java -Dexec.mainClass="com.agentcore.examples.AdvancedContainerTest"

# 运行综合性容器测试
mvn exec:java -Dexec.mainClass="com.agentcore.examples.ComprehensiveContainerTest"
```

### 方法2: 在 IDE 中运行
1. 打开相应的测试类文件
2. 右键点击 `main` 方法
3. 选择 "Run" 或 "Debug"

### 方法3: 编译后运行
```bash
# 编译项目
mvn clean compile

# 运行测试（在项目根目录）
java -cp "agentcore-examples/target/classes:agentcore-container/target/classes:agentcore-core/target/classes" com.agentcore.examples.ContainerTestDemo
```

## 测试功能覆盖

### 容器生命周期管理
- ✅ 容器启动和停止
- ✅ 自动启动配置
- ✅ 超时设置验证

### Agent 管理功能
- ✅ Agent 创建（createAgent）
- ✅ Agent 添加（addAgent）
- ✅ Agent 移除（removeAgent）
- ✅ Agent 查找（getAgent）
- ✅ Agent 存在性检查（containsAgent）
- ✅ 批量 Agent 获取（getAllAgents）
- ✅ 按类型分组（getAgentsByType）

### 消息通信功能
- ✅ 消息发送（sendMessage）
- ✅ 消息接收和处理
- ✅ 消息路由验证
- ✅ 异步消息处理

### 状态监控和统计
- ✅ 容器统计信息获取
- ✅ Agent 状态监控
- ✅ 性能指标收集
- ✅ 错误状态跟踪

### 事件监听机制
- ✅ Agent 添加/移除事件
- ✅ Agent 启动/停止事件
- ✅ 错误事件处理
- ✅ 自定义事件监听器

### 错误处理和恢复
- ✅ 异常 Agent 处理
- ✅ 容器稳定性验证
- ✅ 错误状态恢复
- ✅ 故障隔离机制

## 预期输出

运行测试后，您将看到类似以下的输出：

```
=== 开始容器功能测试演示 ===
创建容器配置: TestContainer
启动容器...
容器启动成功，运行状态: true
=== 开始演示容器功能 ===
1. 创建不同类型的Agent...
事件监听器 - Agent添加: WorkerAgent-1 (类型: worker)
事件监听器 - Agent添加: WorkerAgent-2 (类型: worker)
事件监听器 - Agent启动: WorkerAgent-1 → ACTIVE
...
容器统计信息: ContainerStats{name=TestContainer, running=true, total=4, active=4, suspended=0, errors=0, messages=15, avgTime=12.34ms}
=== 容器功能测试演示完成 ===
```

## 注意事项

1. **依赖要求**: 确保所有相关模块已正确编译
2. **日志配置**: 测试使用 SLF4J 日志框架，确保有合适的日志实现
3. **资源清理**: 测试会自动清理资源，但建议在 IDE 中运行时观察资源使用情况
4. **性能考虑**: 大规模测试可能消耗较多内存，根据系统配置调整测试规模

## 扩展测试

要添加新的容器测试，可以参考现有测试类的结构：

1. 创建新的测试类继承自合适的基类
2. 实现必要的 Agent 类型
3. 添加特定的测试场景
4. 更新此文档说明新的测试功能

## 故障排除

如果测试运行失败，请检查：

1. 类路径是否正确包含所有依赖模块
2. Maven 依赖是否已正确解析
3. 日志配置是否正确
4. 系统资源是否充足

如需进一步帮助，请参考项目的主 README 文档或联系开发团队。