package com.todo.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class PortConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    private static final int MIN_PORT = 11000;
    private static final int MAX_PORT = 12000;

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        int port = getRandomPort(MIN_PORT, MAX_PORT);
        factory.setPort(8083);
    }

    private int getRandomPort(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }
}
