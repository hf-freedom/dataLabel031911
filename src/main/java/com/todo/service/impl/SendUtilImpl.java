package com.todo.service.impl;

import com.todo.service.ISendUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SendUtilImpl implements ISendUtil {
    private static final Logger logger = LoggerFactory.getLogger(SendUtilImpl.class);

    @Override
    public void send(String msg) {
        logger.info("发送待办提醒消息: {}", msg);
    }
}
