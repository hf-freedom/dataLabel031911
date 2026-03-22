package com.todo.service.impl;

import com.todo.cache.UserCache;
import com.todo.entity.User;
import com.todo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserCache userCache;

    @Override
    public User findById(Long id) {
        return userCache.findById(id);
    }

    @Override
    public User findByUsername(String username) {
        return userCache.findByUsername(username);
    }

    @Override
    public List<User> findAll() {
        return userCache.findAll();
    }

    @Override
    public List<User> findNonAdminUsers() {
        return userCache.findNonAdminUsers();
    }

    @Override
    public boolean isAdmin(Long userId) {
        User user = userCache.findById(userId);
        return user != null && Boolean.TRUE.equals(user.getIsAdmin());
    }
}
