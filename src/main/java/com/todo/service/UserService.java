package com.todo.service;

import com.todo.entity.User;
import java.util.List;

public interface UserService {
    User findById(Long id);
    User findByUsername(String username);
    List<User> findAll();
    List<User> findNonAdminUsers();
    boolean isAdmin(Long userId);
}
