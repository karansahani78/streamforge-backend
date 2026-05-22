package com.streamforge.service;

import com.streamforge.config.RabbitMQConfig;
import com.streamforge.dto.VideoProcessingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoEventProducer {
    private final RabbitTemplate rabbitTemplate;

    public void sendVideoProcessingEvent(VideoProcessingMessage message){
        rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_PROCESSING_QUEUE, message);
        log.info("Video processing event sent for videos: {}", message.getVideoId());
    }
}
