package com.todo;

import com.todo.service.ISendUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Primary
public class MockSendUtil implements ISendUtil {

    private static final Logger logger = LoggerFactory.getLogger(MockSendUtil.class);

    private final List<String> messages = new ArrayList<>();

    @Override
    public void send(String msg) {
        logger.info("MockSendUtil发送消息: {}", msg);
        messages.add(msg);
    }

    public List<String> getMessages() {
        return new ArrayList<>(messages);
    }

    public void clear() {
        messages.clear();
    }

    public boolean hasMessageContaining(String content) {
        return messages.stream().anyMatch(msg -> msg.contains(content));
    }
}
