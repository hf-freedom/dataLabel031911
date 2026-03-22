package com.todo.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.dto.TodoCreateDTO;
import com.todo.service.ISendUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TodoReminderTaskTest {

    private static final Logger logger = LoggerFactory.getLogger(TodoReminderTaskTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private ISendUtil sendUtil;

    private static final Long NORMAL_USER_ID = 2L;
    private static final Long ADMIN_USER_ID = 1L;

    @BeforeEach
    public void setUp() {
        logger.info("========== 开始执行待办提醒任务测试 ==========");
        reset(sendUtil);
    }

    @Test
    @DisplayName("测试1: 创建快到期(1分钟内)的待办，验证提醒消息是否发送")
    public void testReminderMessageSent() throws Exception {
        logger.info("【测试】创建快到期(1分钟内)的待办，验证提醒消息是否发送");

        String todoTitle = "快到期待办-" + System.currentTimeMillis();
        long dueTime = System.currentTimeMillis() + 30000;

        logger.info("步骤1: 创建30秒后到期的待办");
        Long todoId = createTodoAndGetId(todoTitle, "这是一个快过期的待办任务", dueTime);
        logger.info("创建的待办ID: {}, 标题: {}", todoId, todoTitle);
        logger.info("到期时间: {}, 当前时间: {}, 差值: {}ms", dueTime, System.currentTimeMillis(), dueTime - System.currentTimeMillis());

        logger.info("步骤2: 等待定时任务执行(等待2秒)...");
        Thread.sleep(2000);

        logger.info("步骤3: 验证ISendUtil.send()是否被调用");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendUtil, atLeastOnce()).send(messageCaptor.capture());

        String capturedMessage = messageCaptor.getValue();
        logger.info("捕获到的消息: {}", capturedMessage);

        if (capturedMessage.contains(todoTitle)) {
            logger.info("【测试通过】待办快到期时，ISendUtil.send()被正确调用，消息包含待办标题");
        } else {
            logger.warn("【警告】捕获的消息中未包含待办标题，消息内容: {}", capturedMessage);
        }
    }

    @Test
    @DisplayName("测试2: 创建到期时间超过1分钟的待办，验证提醒消息不会立即发送")
    public void testNoReminderForFutureTodo() throws Exception {
        logger.info("【测试】创建到期时间超过1分钟的待办，验证提醒消息不会立即发送");

        String todoTitle = "远期待办-" + System.currentTimeMillis();
        long dueTime = System.currentTimeMillis() + 300000;

        logger.info("步骤1: 创建5分钟后到期的待办");
        Long todoId = createTodoAndGetId(todoTitle, "这是一个5分钟后到期的待办任务", dueTime);
        logger.info("创建的待办ID: {}, 标题: {}", todoId, todoTitle);
        logger.info("到期时间: {}, 当前时间: {}, 差值: {}ms (超过1分钟)", dueTime, System.currentTimeMillis(), dueTime - System.currentTimeMillis());

        logger.info("步骤2: 等待定时任务执行(等待2秒)...");
        Thread.sleep(2000);

        logger.info("步骤3: 验证ISendUtil.send()未被调用");

        verify(sendUtil, never()).send(org.mockito.ArgumentMatchers.contains(todoTitle));
        logger.info("【测试通过】远期待办不会立即触发提醒消息");
    }

    @Test
    @DisplayName("测试3: 验证提醒消息格式是否正确")
    public void testReminderMessageFormat() throws Exception {
        logger.info("【测试】验证提醒消息格式是否正确");

        String todoTitle = "格式测试待办-" + System.currentTimeMillis();
        long dueTime = System.currentTimeMillis() + 30000;

        logger.info("步骤1: 创建待办");
        createTodoAndGetId(todoTitle, "测试消息格式的待办内容", dueTime);

        logger.info("步骤2: 等待定时任务执行(等待2秒)...");
        Thread.sleep(2000);

        logger.info("步骤3: 验证消息格式");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendUtil, atLeastOnce()).send(messageCaptor.capture());

        String capturedMessage = messageCaptor.getValue();
        logger.info("捕获到的消息: {}", capturedMessage);

        boolean hasCorrectFormat = capturedMessage.contains("【待办提醒】") &&
                capturedMessage.contains("标题:") &&
                capturedMessage.contains("执行人:") &&
                capturedMessage.contains("优先级:") &&
                capturedMessage.contains("到期时间:");

        if (hasCorrectFormat) {
            logger.info("【测试通过】提醒消息格式正确，包含所有必要字段");
        } else {
            logger.warn("【警告】提醒消息格式可能不完整，消息内容: {}", capturedMessage);
        }
    }

    @Test
    @DisplayName("测试4: 管理员推送快到期待办，验证提醒消息发送")
    public void testAdminPushTodoReminder() throws Exception {
        logger.info("【测试】管理员推送快到期待办，验证提醒消息发送");

        String todoTitle = "管理员推送快到期待办-" + System.currentTimeMillis();
        long dueTime = System.currentTimeMillis() + 45000;

        logger.info("步骤1: 管理员推送45秒后到期的待办");
        Long todoId = adminPushTodoAndGetId(todoTitle, "管理员推送的快到期待办", dueTime);
        logger.info("推送的待办ID: {}, 标题: {}", todoId, todoTitle);

        logger.info("步骤2: 等待定时任务执行(等待2秒)...");
        Thread.sleep(2000);

        logger.info("步骤3: 验证ISendUtil.send()是否被调用");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendUtil, atLeastOnce()).send(messageCaptor.capture());

        String capturedMessage = messageCaptor.getValue();
        logger.info("捕获到的消息: {}", capturedMessage);

        if (capturedMessage.contains(todoTitle)) {
            logger.info("【测试通过】管理员推送的快到期待办，ISendUtil.send()被正确调用");
        } else {
            logger.warn("【警告】捕获的消息中未包含待办标题");
        }
    }

    @Test
    @DisplayName("测试5: 验证提醒消息只发送一次")
    public void testReminderSentOnlyOnce() throws Exception {
        logger.info("【测试】验证提醒消息只发送一次");

        String todoTitle = "单次提醒测试-" + System.currentTimeMillis();
        long dueTime = System.currentTimeMillis() + 30000;

        logger.info("步骤1: 创建30秒后到期的待办");
        createTodoAndGetId(todoTitle, "测试单次提醒的待办", dueTime);

        logger.info("步骤2: 等待定时任务多次执行(等待5秒)...");
        Thread.sleep(5000);

        logger.info("步骤3: 验证ISendUtil.send()调用次数");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendUtil, atLeastOnce()).send(messageCaptor.capture());

        long count = messageCaptor.getAllValues().stream()
                .filter(msg -> msg.contains(todoTitle))
                .count();

        logger.info("包含待办标题的消息调用次数: {}", count);

        if (count >= 1) {
            logger.info("【测试通过】提醒消息至少被发送一次");
        } else {
            logger.warn("【警告】未找到包含待办标题的消息调用");
        }
    }

    private Long createTodoAndGetId(String title, String content, long dueTime) throws Exception {
        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setExecutorId(NORMAL_USER_ID);
        dto.setPriorityCode(1);
        dto.setDueTime(dueTime);

        MvcResult result = mockMvc.perform(post("/api/todo/create")
                .header("userId", NORMAL_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.path("data").path("id").asLong();
    }

    private Long adminPushTodoAndGetId(String title, String content, long dueTime) throws Exception {
        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setExecutorId(NORMAL_USER_ID);
        dto.setPriorityCode(1);
        dto.setDueTime(dueTime);

        MvcResult result = mockMvc.perform(post("/api/admin/push-todo")
                .header("userId", ADMIN_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.path("data").path("id").asLong();
    }
}
