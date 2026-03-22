package com.todo.cache;

import com.todo.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class UserCache {
    private final Map<Long, User> userMap = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        userMap.put(user.getId(), user);
        return user;
    }

    public User findById(Long id) {
        return userMap.get(id);
    }

    public User findByUsername(String username) {
        return userMap.values().stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    public List<User> findAll() {
        return new ArrayList<>(userMap.values());
    }

    public void deleteById(Long id) {
        userMap.remove(id);
    }

    public List<User> findNonAdminUsers() {
        List<User> users = new ArrayList<>();
        for (User user : userMap.values()) {
            if (!Boolean.TRUE.equals(user.getIsAdmin())) {
                users.add(user);
            }
        }
        return users;
    }
}
