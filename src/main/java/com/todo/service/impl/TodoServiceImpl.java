package com.todo.service.impl;

import com.todo.cache.TodoCache;
import com.todo.entity.Priority;
import com.todo.entity.Todo;
import com.todo.entity.TodoStatus;
import com.todo.service.TodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TodoServiceImpl implements TodoService {

    @Autowired
    private TodoCache todoCache;

    @Override
    public Todo create(Todo todo) {
        if (todo.getStatus() == null) {
            todo.setStatus(TodoStatus.PENDING);
        }
        if (todo.getPriority() == null) {
            todo.setPriority(Priority.MEDIUM);
        }
        todo.setNotified(false);
        return todoCache.save(todo);
    }

    @Override
    public Todo update(Todo todo) {
        Todo existingTodo = todoCache.findById(todo.getId());
        if (existingTodo == null) {
            return null;
        }
        return todoCache.save(todo);
    }

    @Override
    public void delete(Long id) {
        todoCache.deleteById(id);
    }

    @Override
    public Todo findById(Long id) {
        return todoCache.findById(id);
    }

    @Override
    public List<Todo> findAll() {
        return todoCache.findAll();
    }

    @Override
    public List<Todo> findByExecutorId(Long executorId) {
        return todoCache.findByExecutorId(executorId);
    }

    @Override
    public List<Todo> findByCreatorId(Long creatorId) {
        return todoCache.findByCreatorId(creatorId);
    }

    @Override
    public Todo updatePriority(Long id, Integer priorityCode) {
        Todo todo = todoCache.findById(id);
        if (todo == null) {
            return null;
        }
        todo.setPriority(Priority.fromCode(priorityCode));
        return todoCache.save(todo);
    }

    @Override
    public Todo updateStatus(Long id, Integer statusCode) {
        Todo todo = todoCache.findById(id);
        if (todo == null) {
            return null;
        }
        todo.setStatus(TodoStatus.fromCode(statusCode));
        return todoCache.save(todo);
    }

    @Override
    public List<Todo> findPendingTodos() {
        return todoCache.findPendingTodos();
    }
}
