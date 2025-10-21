package com.agentcore.core.message;

/**
 * 消息执行类型枚举
 * 定义Agent间通信的消息类型，基于FIPA ACL标准
 * 
 * @author AgentCore Team
 */
public enum MessagePerformative {
    
    /**
     * 请求 - 请求接收者执行某个动作
     */
    REQUEST("request", "请求执行动作"),
    
    /**
     * 通知 - 告知接收者某个信息
     */
    INFORM("inform", "通知信息"),
    
    /**
     * 查询 - 通用查询消息
     */
    QUERY("query", "查询"),
    
    /**
     * 查询条件 - 查询某个条件是否为真
     */
    QUERY_IF("query-if", "查询条件"),
    
    /**
     * 查询引用 - 查询某个对象的引用
     */
    QUERY_REF("query-ref", "查询引用"),
    
    /**
     * 同意 - 同意执行请求的动作
     */
    AGREE("agree", "同意请求"),
    
    /**
     * 拒绝 - 拒绝执行请求的动作
     */
    REFUSE("refuse", "拒绝请求"),
    
    /**
     * 失败 - 通知动作执行失败
     */
    FAILURE("failure", "执行失败"),
    
    /**
     * 完成 - 通知动作执行完成
     */
    DONE("done", "执行完成"),
    
    /**
     * 提议 - 提出一个提议
     */
    PROPOSE("propose", "提出提议"),
    
    /**
     * 接受提议 - 接受一个提议
     */
    ACCEPT_PROPOSAL("accept-proposal", "接受提议"),
    
    /**
     * 拒绝提议 - 拒绝一个提议
     */
    REJECT_PROPOSAL("reject-proposal", "拒绝提议"),
    
    /**
     * 订阅 - 订阅某个主题或服务
     */
    SUBSCRIBE("subscribe", "订阅服务"),
    
    /**
     * 取消订阅 - 取消订阅某个主题或服务
     */
    UNSUBSCRIBE("unsubscribe", "取消订阅"),
    
    /**
     * 确认 - 确认收到消息
     */
    CONFIRM("confirm", "确认收到"),
    
    /**
     * 取消 - 取消之前的请求
     */
    CANCEL("cancel", "取消请求"),
    
    /**
     * 调用 - 调用远程方法
     */
    CALL_FOR_PROPOSAL("call-for-proposal", "征求提议"),
    
    /**
     * 不理解 - 表示不理解消息内容
     */
    NOT_UNDERSTOOD("not-understood", "不理解消息"),
    
    /**
     * 心跳 - 用于保持连接活跃
     */
    HEARTBEAT("heartbeat", "心跳消息"),
    
    /**
     * 注册 - 注册服务或Agent
     */
    REGISTER("register", "注册服务"),
    
    /**
     * 注销 - 注销服务或Agent
     */
    DEREGISTER("deregister", "注销服务");

    private final String code;
    private final String description;

    MessagePerformative(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 检查是否为查询类型消息
     * 
     * @return 如果是查询类型返回true
     */
    public boolean isQuery() {
        return this == QUERY || this == QUERY_IF || this == QUERY_REF;
    }

    /**
     * 检查是否为响应类型消息
     * 
     * @return 如果是响应类型返回true
     */
    public boolean isResponse() {
        return this == INFORM || this == AGREE || this == REFUSE || 
               this == FAILURE || this == DONE || this == CONFIRM ||
               this == NOT_UNDERSTOOD;
    }

    /**
     * 检查是否为提议相关消息
     * 
     * @return 如果是提议相关返回true
     */
    public boolean isProposal() {
        return this == PROPOSE || this == ACCEPT_PROPOSAL || 
               this == REJECT_PROPOSAL || this == CALL_FOR_PROPOSAL;
    }

    /**
     * 检查是否为订阅相关消息
     * 
     * @return 如果是订阅相关返回true
     */
    public boolean isSubscription() {
        return this == SUBSCRIBE || this == UNSUBSCRIBE;
    }

    /**
     * 检查是否为注册相关消息
     * 
     * @return 如果是注册相关返回true
     */
    public boolean isRegistration() {
        return this == REGISTER || this == DEREGISTER;
    }

    /**
     * 检查是否为系统消息
     * 
     * @return 如果是系统消息返回true
     */
    public boolean isSystemMessage() {
        return this == HEARTBEAT || this == REGISTER || this == DEREGISTER;
    }

    /**
     * 从代码获取消息类型
     * 
     * @param code 消息类型代码
     * @return MessagePerformative实例
     */
    public static MessagePerformative fromCode(String code) {
        for (MessagePerformative performative : values()) {
            if (performative.code.equals(code)) {
                return performative;
            }
        }
        throw new IllegalArgumentException("Unknown message performative code: " + code);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", code, description);
    }
}