package com.todo.config;

import com.todo.cache.UserCache;
import com.todo.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserCache userCache;

    @Override
    public void run(String... args) throws Exception {
        logger.info("开始初始化用户数据...");

        User admin = new User(1L, "admin", "admin123", true);
        userCache.save(admin);
        logger.info("创建管理员: {}", admin.getUsername());

        User user1 = new User(2L, "zhangsan", "123456", false);
        userCache.save(user1);
        logger.info("创建用户: {}", user1.getUsername());

        User user2 = new User(3L, "lisi", "123456", false);
        userCache.save(user2);
        logger.info("创建用户: {}", user2.getUsername());

        User user3 = new User(4L, "wangwu", "123456", false);
        userCache.save(user3);
        logger.info("创建用户: {}", user3.getUsername());

        logger.info("用户数据初始化完成！共创建 {} 个用户", userCache.findAll().size());
    }
}
