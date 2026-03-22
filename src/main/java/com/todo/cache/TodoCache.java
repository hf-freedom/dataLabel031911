package com.todo.cache;

import com.todo.entity.Todo;
import com.todo.entity.TodoStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class TodoCache {
    private final Map<Long, Todo> todoMap = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Todo save(Todo todo) {
        if (todo.getId() == null) {
            todo.setId(idGenerator.getAndIncrement());
        }
        todoMap.put(todo.getId(), todo);
        return todo;
    }

    public Todo findById(Long id) {
        return todoMap.get(id);
    }

    public List<Todo> findAll() {
        return new ArrayList<>(todoMap.values());
    }

    public void deleteById(Long id) {
        todoMap.remove(id);
    }

    public List<Todo> findByExecutorId(Long executorId) {
        return todoMap.values().stream()
                .filter(todo -> executorId.equals(todo.getExecutorId()))
                .collect(Collectors.toList());
    }

    public List<Todo> findByCreatorId(Long creatorId) {
        return todoMap.values().stream()
                .filter(todo -> creatorId.equals(todo.getCreatorId()))
                .collect(Collectors.toList());
    }

    public List<Todo> findPendingTodos() {
        return todoMap.values().stream()
                .filter(todo -> todo.getStatus() == TodoStatus.PENDING || todo.getStatus() == TodoStatus.IN_PROGRESS)
                .collect(Collectors.toList());
    }
}
