package com.todo.controller;

import com.todo.cache.UserCache;
import com.todo.common.Result;
import com.todo.dto.TodoCreateDTO;
import com.todo.dto.TodoUpdateDTO;
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
@RequestMapping("/api/todo")
public class TodoController {

    @Autowired
    private TodoService todoService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserCache userCache;

    @PostMapping("/create")
    public Result<Todo> create(@RequestBody TodoCreateDTO dto,
                               @RequestHeader(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        User creator = userService.findById(userId);
        if (creator == null) {
            return Result.error(404, "用户不存在");
        }

        Todo todo = new Todo();
        todo.setTitle(dto.getTitle());
        todo.setContent(dto.getContent());
        todo.setCreatorId(userId);
        todo.setCreatorName(creator.getUsername());
        todo.setExecutorId(dto.getExecutorId());
        todo.setExecutorName(dto.getExecutorName());
        todo.setPriority(Priority.fromCode(dto.getPriorityCode() != null ? dto.getPriorityCode() : 2));
        if (dto.getDueTime() != null) {
            todo.setDueTime(new Date(dto.getDueTime()));
        }

        Todo created = todoService.create(todo);
        return Result.success("创建成功", created);
    }

    @PutMapping("/update")
    public Result<Todo> update(@RequestBody TodoUpdateDTO dto,
                               @RequestHeader(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        Todo existing = todoService.findById(dto.getId());
        if (existing == null) {
            return Result.error(404, "待办不存在");
        }

        if (!existing.getExecutorId().equals(userId) && !existing.getCreatorId().equals(userId)) {
            return Result.error(403, "无权限修改此待办");
        }

        if (dto.getTitle() != null) {
            existing.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            existing.setContent(dto.getContent());
        }
        if (dto.getPriorityCode() != null) {
            existing.setPriority(Priority.fromCode(dto.getPriorityCode()));
        }
        if (dto.getDueTime() != null) {
            existing.setDueTime(new Date(dto.getDueTime()));
        }

        Todo updated = todoService.update(existing);
        return Result.success("更新成功", updated);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        Todo existing = todoService.findById(id);
        if (existing == null) {
            return Result.error(404, "待办不存在");
        }

        if (!existing.getExecutorId().equals(userId) && !existing.getCreatorId().equals(userId)) {
            return Result.error(403, "无权限删除此待办");
        }

        todoService.delete(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/my")
    public Result<List<Todo>> getMyTodos(@RequestHeader(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        List<Todo> todos = todoService.findByExecutorId(userId);
        return Result.success(todos);
    }

    @GetMapping("/created")
    public Result<List<Todo>> getCreatedTodos(@RequestHeader(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        List<Todo> todos = todoService.findByCreatorId(userId);
        return Result.success(todos);
    }

    @GetMapping("/detail/{id}")
    public Result<Todo> getDetail(@PathVariable Long id) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            return Result.error(404, "待办不存在");
        }
        return Result.success(todo);
    }

    @PutMapping("/priority/{id}")
    public Result<Todo> updatePriority(@PathVariable Long id,
                                       @RequestParam Integer priorityCode,
                                       @RequestHeader(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        Todo existing = todoService.findById(id);
        if (existing == null) {
            return Result.error(404, "待办不存在");
        }

        if (!existing.getExecutorId().equals(userId) && !existing.getCreatorId().equals(userId)) {
            return Result.error(403, "无权限修改此待办");
        }

        Todo updated = todoService.updatePriority(id, priorityCode);
        return Result.success("优先级更新成功", updated);
    }

    @PutMapping("/status/{id}")
    public Result<Todo> updateStatus(@PathVariable Long id,
                                     @RequestParam Integer statusCode,
                                     @RequestHeader(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        Todo existing = todoService.findById(id);
        if (existing == null) {
            return Result.error(404, "待办不存在");
        }

        if (!existing.getExecutorId().equals(userId)) {
            return Result.error(403, "无权限修改此待办状态");
        }

        Todo updated = todoService.updateStatus(id, statusCode);
        return Result.success("状态更新成功", updated);
    }
}
