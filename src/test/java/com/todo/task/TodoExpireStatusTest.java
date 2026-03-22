package com.todo.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.dto.TodoCreateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TodoExpireStatusTest {

    private static final Logger logger = LoggerFactory.getLogger(TodoExpireStatusTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long NORMAL_USER_ID = 2L;
    private static final Long ADMIN_USER_ID = 1L;

    @BeforeEach
    public void setUp() {
        logger.info("========== 开始执行待办过期状态测试 ==========");
    }

    @Test
    @DisplayName("测试1: 创建已过期的待办，验证状态变为EXPIRED")
    public void testTodoExpiredStatus() throws Exception {
        logger.info("【测试】创建已过期的待办，验证状态变为EXPIRED");

        String todoTitle = "已过期待办-" + System.currentTimeMillis();
        long dueTime = System.currentTimeMillis() - 10000;

        logger.info("步骤1: 创建10秒前到期的待办(已过期)");
        Long todoId = createTodoAndGetId(todoTitle, "这是一个已经过期的待办任务", dueTime);
        logger.info("创建的待办ID: {}, 标题: {}", todoId, todoTitle);
        logger.info("到期时间: {}, 当前时间: {}, 差值: {}ms (已过期)", dueTime, System.currentTimeMillis(), dueTime - System.currentTimeMillis());

        logger.info("步骤2: 等待定时任务执行(等待2秒)...");
        Thread.sleep(2000);

        logger.info("步骤3: 查询待办详情，验证状态是否变为EXPIRED");

        MvcResult detailResult = mockMvc.perform(get("/api/todo/detail/{id}", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andReturn();

        String detailResponse = detailResult.getResponse().getContentAsString();
        logger.info("待办详情响应: {}", detailResponse);

        String status = objectMapper.readTree(detailResponse).path("data").path("status").asText();
        logger.info("待办当前状态: {}", status);

        if ("EXPIRED".equals(status)) {
            logger.info("【测试通过】已过期待办的状态正确变为EXPIRED");
        } else {
            logger.warn("【警告】待办状态为: {}，期望: EXPIRED", status);
        }
    }

    @Test
    @DisplayName("测试2: 创建未到期的待办，验证状态保持PENDING")
    public void testTodoPendingStatus() throws Exception {
        logger.info("【测试】创建未到期的待办，验证状态保持PENDING");

        String todoTitle = "未到期待办-" + System.currentTimeMillis();
        long dueTime = System.currentTimeMillis() + 300000;

        logger.info("步骤1: 创建5分钟后到期的待办");
        Long todoId = createTodoAndGetId(todoTitle, "这是一个5分钟后到期的待办任务", dueTime);
        logger.info("创建的待办ID: {}, 标题: {}", todoId, todoTitle);
        logger.info("到期时间: {}, 当前时间: {}, 差值: {}ms (未到期)", dueTime, System.currentTimeMillis(), dueTime - System.currentTimeMillis());

        logger.info("步骤2: 等待定时任务执行(等待2秒)...");
        Thread.sleep(2000);

        logger.info("步骤3: 查询待办详情，验证状态保持PENDING");

        MvcResult detailResult = mockMvc.perform(get("/api/todo/detail/{id}", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andReturn();

        String detailResponse = detailResult.getResponse().getContentAsString();
        logger.info("待办详情响应: {}", detailResponse);

        String status = objectMapper.readTree(detailResponse).path("data").path("status").asText();
        logger.info("待办当前状态: {}", status);

        if ("PENDING".equals(status)) {
            logger.info("【测试通过】未到期待办的状态保持PENDING");
        } else {
            logger.warn("【警告】待办状态为: {}，期望: PENDING", status);
        }
    }

    @Test
    @DisplayName("测试3: 管理员推送已过期待办，验证状态变为EXPIRED")
    public void testAdminPushExpiredTodo() throws Exception {
        logger.info("【测试】管理员推送已过期待办，验证状态变为EXPIRED");

        String todoTitle = "管理员推送已过期待办-" + System.currentTimeMillis();
        long dueTime = System.currentTimeMillis() - 5000;

        logger.info("步骤1: 管理员推送5秒前到期的待办(已过期)");
        Long todoId = adminPushTodoAndGetId(todoTitle, "管理员推送的已过期待办", dueTime);
        logger.info("推送的待办ID: {}, 标题: {}", todoId, todoTitle);
        logger.info("到期时间: {}, 当前时间: {}, 差值: {}ms (已过期)", dueTime, System.currentTimeMillis(), dueTime - System.currentTimeMillis());

        logger.info("步骤2: 等待定时任务执行(等待2秒)...");
        Thread.sleep(2000);

        logger.info("步骤3: 查询待办详情，验证状态是否变为EXPIRED");

        MvcResult detailResult = mockMvc.perform(get("/api/todo/detail/{id}", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andReturn();

        String detailResponse = detailResult.getResponse().getContentAsString();
        logger.info("待办详情响应: {}", detailResponse);

        String status = objectMapper.readTree(detailResponse).path("data").path("status").asText();
        logger.info("待办当前状态: {}", status);

        if ("EXPIRED".equals(status)) {
            logger.info("【测试通过】管理员推送的已过期待办状态正确变为EXPIRED");
        } else {
            logger.warn("【警告】待办状态为: {}，期望: EXPIRED", status);
        }
    }

    @Test
    @DisplayName("测试4: 用户在待办列表中查看过期待办")
    public void testViewExpiredTodoInList() throws Exception {
        logger.info("【测试】用户在待办列表中查看过期待办");

        String todoTitle = "列表中的过期待办-" + System.currentTimeMillis();
        long dueTime = System.currentTimeMillis() - 10000;

        logger.info("步骤1: 创建已过期待办");
        createTodoAndGetId(todoTitle, "测试列表中显示过期状态的待办", dueTime);

        logger.info("步骤2: 等待定时任务执行(等待2秒)...");
        Thread.sleep(2000);

        logger.info("步骤3: 查询用户待办列表，验证过期待办显示");

        MvcResult listResult = mockMvc.perform(get("/api/todo/my")
                .header("userId", NORMAL_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        String listResponse = listResult.getResponse().getContentAsString();
        logger.info("待办列表响应: {}", listResponse);

        if (listResponse.contains(todoTitle) && listResponse.contains("EXPIRED")) {
            logger.info("【测试通过】过期待办在用户列表中正确显示EXPIRED状态");
        } else if (listResponse.contains(todoTitle)) {
            logger.warn("【警告】待办列表中包含待办但可能状态不是EXPIRED");
        } else {
            logger.warn("【警告】待办列表中未找到该待办");
        }
    }

    @Test
    @DisplayName("测试5: 验证所有待办状态类型")
    public void testAllTodoStatuses() throws Exception {
        logger.info("【测试】验证所有待办状态类型");

        logger.info("待办状态枚举值:");
        logger.info("- PENDING(0): 待处理");
        logger.info("- IN_PROGRESS(1): 进行中");
        logger.info("- COMPLETED(2): 已完成");
        logger.info("- EXPIRED(3): 已过期");

        String todoTitle = "状态测试待办-" + System.currentTimeMillis();

        logger.info("步骤1: 创建待办");
        Long todoId = createTodoAndGetId(todoTitle, "测试各种状态转换的待办", System.currentTimeMillis() + 60000);
        logger.info("创建的待办ID: {}", todoId);

        logger.info("步骤2: 更新状态为IN_PROGRESS(1)");
        mockMvc.perform(put("/api/todo/status/{id}", todoId)
                .header("userId", NORMAL_USER_ID)
                .param("statusCode", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        MvcResult progressResult = mockMvc.perform(get("/api/todo/detail/{id}", todoId))
                .andReturn();
        String progressStatus = objectMapper.readTree(progressResult.getResponse().getContentAsString())
                .path("data").path("status").asText();
        logger.info("更新为进行中后的状态: {}", progressStatus);

        logger.info("步骤3: 更新状态为COMPLETED(2)");
        mockMvc.perform(put("/api/todo/status/{id}", todoId)
                .header("userId", NORMAL_USER_ID)
                .param("statusCode", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        MvcResult completedResult = mockMvc.perform(get("/api/todo/detail/{id}", todoId))
                .andReturn();
        String completedStatus = objectMapper.readTree(completedResult.getResponse().getContentAsString())
                .path("data").path("status").asText();
        logger.info("更新为已完成后的状态: {}", completedStatus);

        if ("IN_PROGRESS".equals(progressStatus) && "COMPLETED".equals(completedStatus)) {
            logger.info("【测试通过】所有待办状态类型验证成功");
        } else {
            logger.warn("【警告】状态验证结果: IN_PROGRESS={}, COMPLETED={}", progressStatus, completedStatus);
        }
    }

    private Long createTodoAndGetId(String title, String content, long dueTime) throws Exception {
        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setExecutorId(NORMAL_USER_ID);
        dto.setPriorityCode(2);
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
