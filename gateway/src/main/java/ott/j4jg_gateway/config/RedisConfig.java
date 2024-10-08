package ott.j4jg_gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import ott.j4jg_gateway.model.entity.RefreshToken;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, RefreshToken> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        // Jackson2JsonRedisSerializer 설정
        Jackson2JsonRedisSerializer<RefreshToken> jsonSerializer = new Jackson2JsonRedisSerializer<>(RefreshToken.class);
        ObjectMapper objectMapper = new ObjectMapper();
        jsonSerializer.setObjectMapper(objectMapper);

        // RedisSerializationContext 설정
        RedisSerializationContext<String, RefreshToken> serializationContext = RedisSerializationContext
                .<String, RefreshToken>newSerializationContext(new StringRedisSerializer())
                .value(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .hashKey(new StringRedisSerializer())
                .hashValue(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}
