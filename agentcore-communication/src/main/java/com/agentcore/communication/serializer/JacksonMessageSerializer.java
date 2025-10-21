package com.agentcore.communication.serializer;

import com.agentcore.core.message.AgentMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jackson JSON消息序列化器
 * 使用Jackson进行JSON序列化和反序列化
 * 
 * @author AgentCore Team
 */
public class JacksonMessageSerializer implements MessageSerializer {

    private static final Logger logger = LoggerFactory.getLogger(JacksonMessageSerializer.class);

    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     */
    public JacksonMessageSerializer() {
        this.objectMapper = createObjectMapper();
    }

    /**
     * 构造函数
     * 
     * @param objectMapper 自定义ObjectMapper
     */
    public JacksonMessageSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : createObjectMapper();
    }

    @Override
    public byte[] serialize(AgentMessage message) throws SerializationException {
        if (message == null) {
            throw new SerializationException("Message cannot be null");
        }

        try {
            byte[] data = objectMapper.writeValueAsBytes(message);
            logger.debug("Serialized message {} to {} bytes", message.messageId(), data.length);
            return data;
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize message: " + message.messageId(), e);
        }
    }

    @Override
    public AgentMessage deserialize(byte[] data) throws SerializationException {
        if (data == null || data.length == 0) {
            throw new SerializationException("Data cannot be null or empty");
        }

        try {
            AgentMessage message = objectMapper.readValue(data, AgentMessage.class);
            logger.debug("Deserialized message {} from {} bytes", message.messageId(), data.length);
            return message;
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize message from " + data.length + " bytes", e);
        }
    }

    @Override
    public String getName() {
        return "Jackson";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public int getPriority() {
        return 10; // 中等优先级
    }

    /**
     * 创建默认的ObjectMapper
     * 
     * @return ObjectMapper实例
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册Java时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 配置序列化选项
        mapper.findAndRegisterModules();
        
        return mapper;
    }

    /**
     * 获取ObjectMapper实例
     * 
     * @return ObjectMapper实例
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}