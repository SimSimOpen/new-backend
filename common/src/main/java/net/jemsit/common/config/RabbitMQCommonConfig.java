package net.jemsit.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static net.jemsit.common.data.constants.RabbitMQConstants.*;


@Configuration
public class RabbitMQCommonConfig {

    @Bean
    public Queue mediaNotificationQueue() {
        return new Queue(MEDIA_NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Queue mediaProfileQueue() {
        return new Queue(MEDIA_PROFILE_QUEUE, true);
    }

    @Bean
    public Queue productNotificationQueue() {
        return new Queue(PRODUCT_NOTIFICATION_QUEUE, true);
    }
    @Bean
    public TopicExchange mediaExchange() {
        return new TopicExchange(MEDIA_EXCHANGE);
    }

    @Bean TopicExchange productExchange() {
        return new TopicExchange(PRODUCT_EXCHANGE);
    }

    @Bean
    public Binding mediaNotificationBinding() {
        return BindingBuilder.bind(mediaNotificationQueue()).to(mediaExchange()).with(MEDIA_KEY);
    }

    @Bean
    public Binding mediaProfileBinding() {
        return BindingBuilder.bind(mediaProfileQueue()).to(mediaExchange()).with(MEDIA_KEY);
    }

    @Bean
    public Binding productNotificationBinding() {
        return BindingBuilder.bind(productNotificationQueue()).to(productExchange()).with(PRODUCT_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}
