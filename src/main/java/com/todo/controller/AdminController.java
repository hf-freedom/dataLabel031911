package com.todo.controller;

import com.todo.cache.UserCache;
import com.todo.common.Result;
import com.todo.dto.TodoCreateDTO;
import com.todo.entity.Priority;
import com.todo.entity.Todo;
import com.todo.entity.User;
import com.todo.service.TodoService;
import com.todo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private TodoService todoService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserCache userCache;

    private boolean checkAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return userService.isAdmin(userId);
    }

    @GetMapping("/users")
    public Result<List<User>> getAllUsers(@RequestHeader(value = "userId", required = false) Long userId) {
        if (!checkAdmin(userId)) {
            return Result.error(403, "无权限访问");
        }
        List<User> users = userService.findNonAdminUsers();
        return Result.success(users);
    }

    @PostMapping("/push-todo")
    public Result<Todo> pushTodo(@RequestBody TodoCreateDTO dto,
                                 @RequestHeader(value = "userId", required = false) Long userId) {
        if (!checkAdmin(userId)) {
            return Result.error(403, "无权限操作");
        }

        User admin = userService.findById(userId);
        User executor = userService.findById(dto.getExecutorId());

        if (executor == null) {
            return Result.error(404, "执行人不存在");
        }

        Todo todo = new Todo();
        todo.setTitle(dto.getTitle());
        todo.setContent(dto.getContent());
        todo.setCreatorId(userId);
        todo.setCreatorName(admin.getUsername());
        todo.setExecutorId(dto.getExecutorId());
        todo.setExecutorName(executor.getUsername());
        todo.setPriority(Priority.fromCode(dto.getPriorityCode() != null ? dto.getPriorityCode() : 2));
        if (dto.getDueTime() != null) {
            todo.setDueTime(new Date(dto.getDueTime()));
        }

        Todo created = todoService.create(todo);
        return Result.success("推送成功", created);
    }

    @GetMapping("/all-todos")
    public Result<List<Todo>> getAllTodos(@RequestHeader(value = "userId", required = false) Long userId) {
        if (!checkAdmin(userId)) {
            return Result.error(403, "无权限访问");
        }
        List<Todo> todos = todoService.findAll();
        return Result.success(todos);
    }
}
