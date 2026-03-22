package com.todo.service;

import com.todo.entity.Todo;
import java.util.List;

public interface TodoService {
    Todo create(Todo todo);
    Todo update(Todo todo);
    void delete(Long id);
    Todo findById(Long id);
    List<Todo> findAll();
    List<Todo> findByExecutorId(Long executorId);
    List<Todo> findByCreatorId(Long creatorId);
    Todo updatePriority(Long id, Integer priorityCode);
    Todo updateStatus(Long id, Integer statusCode);
    List<Todo> findPendingTodos();
}
