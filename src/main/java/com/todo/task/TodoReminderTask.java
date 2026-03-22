package com.todo.task;

import com.todo.entity.Todo;
import com.todo.entity.TodoStatus;
import com.todo.service.ISendUtil;
import com.todo.service.TodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class TodoReminderTask {

    private static final Logger logger = LoggerFactory.getLogger(TodoReminderTask.class);

    @Autowired
    private TodoService todoService;

    @Autowired
    private ISendUtil sendUtil;

    private static final long ONE_MINUTE_MS = 60 *  1000;

    @Scheduled(fixedRate = 1000)
    public void checkDueTodos() {
        logger.debug("开始检查待办到期提醒...");
        
        List<Todo> pendingTodos = todoService.findPendingTodos();
        Date now = new Date();

        for (Todo todo : pendingTodos) {
            if (todo.getDueTime() == null) {
                continue;
            }

            long timeUntilDue = todo.getDueTime().getTime() - now.getTime();

            if (timeUntilDue <= ONE_MINUTE_MS && timeUntilDue > 0 && !Boolean.TRUE.equals(todo.getNotified())) {
                String msg = buildReminderMessage(todo);
                try {
                    sendUtil.send(msg);
                    todo.setNotified(true);
                    todoService.update(todo);
                    logger.info("已发送待办提醒: {}", msg);
                } catch (Exception e) {
                    logger.error("发送待办提醒失败: {}", e.getMessage());
                }
            }

            if (timeUntilDue <= 0 && todo.getStatus() != TodoStatus.EXPIRED) {
                todo.setStatus(TodoStatus.EXPIRED);
                todoService.update(todo);
                logger.info("待办已过期: {}", todo.getTitle());
            }
        }
    }

    private String buildReminderMessage(Todo todo) {
        StringBuilder sb = new StringBuilder();
        sb.append("【待办提醒】");
        sb.append("标题: ").append(todo.getTitle()).append("; ");
        sb.append("执行人: ").append(todo.getExecutorName()).append("; ");
        sb.append("优先级: ").append(todo.getPriority().getDesc()).append("; ");
        sb.append("到期时间: ").append(todo.getDueTime());
        return sb.toString();
    }
}
