package com.agentcore.communication.serializer;

import com.agentcore.core.message.AgentMessage;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Kryo消息序列化器
 * 使用Kryo进行高性能二进制序列化
 * 
 * @author AgentCore Team
 */
public class KryoMessageSerializer implements MessageSerializer {

    private static final Logger logger = LoggerFactory.getLogger(KryoMessageSerializer.class);

    private final Pool<Kryo> kryoPool;
    private final Pool<Output> outputPool;
    private final Pool<Input> inputPool;

    /**
     * 构造函数
     */
    public KryoMessageSerializer() {
        this.kryoPool = new Pool<Kryo>(true, false, 16) {
            @Override
            protected Kryo create() {
                return createKryo();
            }
        };

        this.outputPool = new Pool<Output>(true, false, 16) {
            @Override
            protected Output create() {
                return new Output(1024, -1);
            }
        };

        this.inputPool = new Pool<Input>(true, false, 16) {
            @Override
            protected Input create() {
                return new Input();
            }
        };
    }

    @Override
    public byte[] serialize(AgentMessage message) throws SerializationException {
        if (message == null) {
            throw new SerializationException("Message cannot be null");
        }

        Kryo kryo = kryoPool.obtain();
        Output output = outputPool.obtain();
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            output.setOutputStream(baos);
            
            kryo.writeObject(output, message);
            output.flush();
            
            byte[] data = baos.toByteArray();
            logger.debug("Serialized message {} to {} bytes", message.messageId(), data.length);
            return data;
            
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize message: " + message.messageId(), e);
        } finally {
            kryoPool.free(kryo);
            output.reset();
            outputPool.free(output);
        }
    }

    @Override
    public AgentMessage deserialize(byte[] data) throws SerializationException {
        if (data == null || data.length == 0) {
            throw new SerializationException("Data cannot be null or empty");
        }

        Kryo kryo = kryoPool.obtain();
        Input input = inputPool.obtain();
        
        try {
            input.setInputStream(new ByteArrayInputStream(data));
            
            AgentMessage message = kryo.readObject(input, AgentMessage.class);
            logger.debug("Deserialized message {} from {} bytes", message.messageId(), data.length);
            return message;
            
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize message from " + data.length + " bytes", e);
        } finally {
            kryoPool.free(kryo);
            input.close();
            inputPool.free(input);
        }
    }

    @Override
    public String getName() {
        return "Kryo";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getContentType() {
        return "application/x-kryo";
    }

    @Override
    public boolean supportsCompression() {
        return true;
    }

    @Override
    public int getPriority() {
        return 20; // 高优先级，因为性能更好
    }

    /**
     * 创建Kryo实例
     * 
     * @return Kryo实例
     */
    private Kryo createKryo() {
        Kryo kryo = new Kryo();
        
        // 配置Kryo
        kryo.setReferences(true);
        kryo.setRegistrationRequired(false);
        
        // 注册常用类
        kryo.register(AgentMessage.class);
        
        return kryo;
    }
}