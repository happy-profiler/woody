package happy2b.woody.common.constant;


import java.util.HashMap;
import java.util.Map;

public enum ProfilingResourceType {

    SPRING_WEB("spring-web") {
        @Override
        public String[] getResourceClasses() {
            return new String[]{"org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping"};
        }
    },
    DUBBO("dubbo") {
        @Override
        public String[] getResourceClasses() {
            return new String[]{"org.apache.dubbo.config.spring.ServiceBean"};
        }
    },
    GRPC("grpc") {
        @Override
        public String[] getResourceClasses() {
            return new String[]{"io.grpc.internal.ServerImpl"};
        }
    },
    ROCKETMQ("rocketmq") {
        @Override
        public String[] getResourceClasses() {
            return new String[]{"org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer", "org.apache.rocketmq.client.consumer.DefaultMQPushConsumer"};
        }
    },
    KAFKA("kafka") {
        @Override
        public String[] getResourceClasses() {
            return new String[]{"org.springframework.kafka.config.MethodKafkaListenerEndpoint"};
        }
    };

    private static final Map<String, ProfilingResourceType> VALUES = new HashMap<>();

    static {
        for (ProfilingResourceType type : values()) {
            VALUES.put(type.value, type);
        }
    }

    private String value;

    ProfilingResourceType(String segment) {
        this.value = segment;
    }

    public String getValue() {
        return value;
    }

    public static ProfilingResourceType ofType(String type) {
        return VALUES.get(type);
    }

    public abstract String[] getResourceClasses();

}
