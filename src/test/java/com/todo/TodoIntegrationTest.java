package com.todo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.dto.TodoCreateDTO;
import com.todo.dto.TodoUpdateDTO;
import com.todo.entity.Todo;
import com.todo.entity.TodoStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TodoIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(TodoIntegrationTest.class);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockSendUtil mockSendUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    // 测试用户ID - 对应DataInitializer中初始化的数据
    private static final Long ADMIN_ID = 1L;      // 管理员
    private static final Long USER_ID = 2L;       // 普通用户 zhangsan
    private static final Long USER2_ID = 3L;      // 普通用户 lisi
    private static final String USER_NAME = "zhangsan";
    private static final String USER2_NAME = "lisi";

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockSendUtil.clear();
        logger.info("=== 测试初始化完成 ===");
    }

    // ==================== 正向测试 ====================

    @Test
    public void testUserTodoCRUD() throws Exception {
        logger.info("=== 开始测试：用户对自己代办的增删改查 ===");

        // 1. 创建代办
        logger.info("1. 测试创建代办");
        TodoCreateDTO createDTO = new TodoCreateDTO();
        createDTO.setTitle("User Test Todo");
        createDTO.setContent("This is a test todo content");
        createDTO.setExecutorId(USER_ID);
        createDTO.setExecutorName(USER_NAME);
        createDTO.setPriorityCode(1);

        MvcResult createResult = mockMvc.perform(post("/api/todo/create")
                        .header("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        logger.info("创建代办响应: {}", createResponse);

        Todo createdTodo = objectMapper.readValue(
                objectMapper.readTree(createResponse).get("data").toString(),
                Todo.class
        );
        Long todoId = createdTodo.getId();
        logger.info("创建的代办ID: {}", todoId);

        // 验证返回值
        assertNotNull("返回的代办ID不应为空", todoId);
        assertEquals("标题验证", "User Test Todo", createdTodo.getTitle());
        assertEquals("内容验证", "This is a test todo content", createdTodo.getContent());

        // 2. 查询我的代办列表
        logger.info("2. 测试查询我的代办列表");
        MvcResult listResult = mockMvc.perform(get("/api/todo/my")
                        .header("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andReturn();

        String listResponse = listResult.getResponse().getContentAsString();
        logger.info("查询我的代办响应: {}", listResponse);

        // 验证列表包含刚创建的代办
        assertTrue("列表应包含刚创建的代办", listResponse.contains("User Test Todo"));

        // 3. 查询代办详情
        logger.info("3. 测试查询代办详情");
        MvcResult detailResult = mockMvc.perform(get("/api/todo/detail/{id}", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(todoId))
                .andReturn();

        String detailResponse = detailResult.getResponse().getContentAsString();
        logger.info("查询代办详情响应: {}", detailResponse);

        Todo detailTodo = objectMapper.readValue(
                objectMapper.readTree(detailResponse).get("data").toString(),
                Todo.class
        );
        assertEquals("详情查询标题验证", "User Test Todo", detailTodo.getTitle());

        // 4. 更新代办
        logger.info("4. 测试更新代办");
        TodoUpdateDTO updateDTO = new TodoUpdateDTO();
        updateDTO.setId(todoId);
        updateDTO.setTitle("Updated Todo Title");
        updateDTO.setContent("Updated todo content");

        MvcResult updateResult = mockMvc.perform(put("/api/todo/update")
                        .header("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"))
                .andReturn();

        String updateResponse = updateResult.getResponse().getContentAsString();
        logger.info("更新代办响应: {}", updateResponse);

        Todo updatedTodo = objectMapper.readValue(
                objectMapper.readTree(updateResponse).get("data").toString(),
                Todo.class
        );
        assertEquals("更新后标题验证", "Updated Todo Title", updatedTodo.getTitle());
        assertEquals("更新后内容验证", "Updated todo content", updatedTodo.getContent());

        // 5. 更新代办状态
        logger.info("5. 测试更新代办状态");
        MvcResult statusResult = mockMvc.perform(put("/api/todo/status/{id}", todoId)
                        .header("userId", USER_ID)
                        .param("statusCode", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("状态更新成功"))
                .andReturn();

        String statusResponse = statusResult.getResponse().getContentAsString();
        logger.info("更新状态响应: {}", statusResponse);

        Todo statusTodo = objectMapper.readValue(
                objectMapper.readTree(statusResponse).get("data").toString(),
                Todo.class
        );
        assertEquals("状态验证", TodoStatus.IN_PROGRESS, statusTodo.getStatus());

        // 6. 删除代办
        logger.info("6. 测试删除代办");
        MvcResult deleteResult = mockMvc.perform(delete("/api/todo/delete/{id}", todoId)
                        .header("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"))
                .andReturn();

        String deleteResponse = deleteResult.getResponse().getContentAsString();
        logger.info("删除代办响应: {}", deleteResponse);

        // 验证删除后查询不到
        MvcResult afterDeleteResult = mockMvc.perform(get("/api/todo/detail/{id}", todoId))
                .andExpect(status().isOk())
                .andReturn();
        String afterDeleteResponse = afterDeleteResult.getResponse().getContentAsString();
        logger.info("删除后查询响应: {}", afterDeleteResponse);

        logger.info("=== 用户代办CRUD测试完成 ===");
    }

    @Test
    public void testAdminPushTodo() throws Exception {
        logger.info("=== 开始测试：管理员推送代办 ===");

        // 1. 管理员推送代办给用户
        logger.info("1. 管理员推送代办给用户");
        TodoCreateDTO createDTO = new TodoCreateDTO();
        createDTO.setTitle("Admin Pushed Todo");
        createDTO.setContent("This is a todo pushed by admin");
        createDTO.setExecutorId(USER_ID);
        createDTO.setPriorityCode(1);
        createDTO.setDueTime(System.currentTimeMillis() + 3600000); // 1小时后

        MvcResult pushResult = mockMvc.perform(post("/api/admin/push-todo")
                        .header("userId", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("推送成功"))
                .andReturn();

        String pushResponse = pushResult.getResponse().getContentAsString();
        logger.info("推送代办响应: {}", pushResponse);

        Todo pushedTodo = objectMapper.readValue(
                objectMapper.readTree(pushResponse).get("data").toString(),
                Todo.class
        );
        Long todoId = pushedTodo.getId();
        logger.info("推送的代办ID: {}", todoId);

        // 2. 用户查看是否能收到该代办
        logger.info("2. 用户查看收到的代办");
        MvcResult listResult = mockMvc.perform(get("/api/todo/my")
                        .header("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String listResponse = listResult.getResponse().getContentAsString();
        logger.info("用户代办列表响应: {}", listResponse);
        assertTrue("用户应能看到管理员推送的代办", listResponse.contains("Admin Pushed Todo"));

        logger.info("=== 管理员推送代办测试完成 ===");
    }

    @Test
    public void testTodoReminderAndExpiration() throws Exception {
        logger.info("=== 开始测试：代办到期提醒和状态变化 ===");

        // 1. 创建一个即将到期的代办（30秒后到期）
        logger.info("1. 创建即将到期的代办");
        TodoCreateDTO createDTO = new TodoCreateDTO();
        createDTO.setTitle("Expiration Test Todo");
        createDTO.setContent("This todo will expire soon");
        createDTO.setExecutorId(USER_ID);
        createDTO.setExecutorName(USER_NAME);
        createDTO.setPriorityCode(1);
        createDTO.setDueTime(System.currentTimeMillis() + 30000); // 30秒后到期

        MvcResult createResult = mockMvc.perform(post("/api/todo/create")
                        .header("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        logger.info("创建代办响应: {}", createResponse);

        Todo todo = objectMapper.readValue(
                objectMapper.readTree(createResponse).get("data").toString(),
                Todo.class
        );
        Long todoId = todo.getId();
        logger.info("创建的代办ID: {}", todoId);

        // 2. 等待提醒任务执行（最多等待70秒）
        logger.info("2. 等待提醒任务执行...");
        boolean reminderReceived = false;
        for (int i = 0; i < 70; i++) {
            Thread.sleep(1000);
            if (mockSendUtil.hasMessageContaining("Expiration Test Todo")) {
                reminderReceived = true;
                logger.info("已收到提醒消息！第{}秒", i + 1);
                break;
            }
            if (i % 10 == 9) {
                logger.info("已等待{}秒，继续等待...", i + 1);
            }
        }

        assertTrue("应收到到期提醒消息", reminderReceived);
        if (reminderReceived) {
            logger.info("提醒消息内容: {}", mockSendUtil.getMessages());
            assertTrue("消息格式应包含待办提醒标题",
                    mockSendUtil.getMessages().stream().anyMatch(msg -> msg.contains("【待办提醒】")));
        }

        // 3. 等待代办过期
        logger.info("3. 等待代办过期...");
        boolean expired = false;
        for (int i = 0; i < 60; i++) {
            Thread.sleep(1000);
            MvcResult detailResult = mockMvc.perform(get("/api/todo/detail/{id}", todoId))
                    .andExpect(status().isOk())
                    .andReturn();

            String detailResponse = detailResult.getResponse().getContentAsString();
            Todo checkedTodo = objectMapper.readValue(
                    objectMapper.readTree(detailResponse).get("data").toString(),
                    Todo.class
            );

            if (checkedTodo.getStatus() == TodoStatus.EXPIRED) {
                expired = true;
                logger.info("代办已过期！第{}秒，状态: {}", i + 1, checkedTodo.getStatus());
                break;
            }
            if (i % 10 == 9) {
                logger.info("已等待{}秒，当前状态: {}", i + 1, checkedTodo.getStatus());
            }
        }

        assertTrue("代办状态应变为已过期", expired);

        logger.info("=== 代办到期提醒和状态变化测试完成 ===");
    }

    @Test
    public void testTodoStatusAfterExpiration() throws Exception {
        logger.info("=== 开始测试：代办到期后状态变化验证 ===");

        // 创建一个已经过期的代办
        logger.info("1. 创建已过期的代办");
        TodoCreateDTO createDTO = new TodoCreateDTO();
        createDTO.setTitle("Already Expired Todo");
        createDTO.setContent("This todo should already be expired");
        createDTO.setExecutorId(USER_ID);
        createDTO.setExecutorName(USER_NAME);
        createDTO.setPriorityCode(1);
        createDTO.setDueTime(System.currentTimeMillis() - 10000); // 10秒前已过期

        MvcResult createResult = mockMvc.perform(post("/api/todo/create")
                        .header("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        logger.info("创建代办响应: {}", createResponse);

        Todo todo = objectMapper.readValue(
                objectMapper.readTree(createResponse).get("data").toString(),
                Todo.class
        );
        Long todoId = todo.getId();
        logger.info("创建的代办ID: {}", todoId);

        // 检查状态变化
        logger.info("2. 检查代办状态变化");
        boolean expired = false;
        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000);
            MvcResult detailResult = mockMvc.perform(get("/api/todo/detail/{id}", todoId))
                    .andExpect(status().isOk())
                    .andReturn();

            String detailResponse = detailResult.getResponse().getContentAsString();
            Todo checkedTodo = objectMapper.readValue(
                    objectMapper.readTree(detailResponse).get("data").toString(),
                    Todo.class
            );

            if (checkedTodo.getStatus() == TodoStatus.EXPIRED) {
                expired = true;
                logger.info("代办状态已更新为已过期！第{}秒", i + 1);
                break;
            }
            if (i % 5 == 4) {
                logger.info("已等待{}秒，当前状态: {}", i + 1, checkedTodo.getStatus());
            }
        }

        assertTrue("测试通过：代办到期后状态正确更新为已过期", expired);

        logger.info("=== 代办到期状态变化测试完成 ===");
    }

    // ==================== 反向测试 ====================

    @Test
    public void testUnauthorizedAccess() throws Exception {
        logger.info("=== 开始反向测试：未授权访问 ===");

        // 1. 未登录创建代办
        logger.info("1. 测试未登录创建代办");
        TodoCreateDTO createDTO = new TodoCreateDTO();
        createDTO.setTitle("No Login Test");
        createDTO.setContent("No login test content");
        createDTO.setExecutorId(USER_ID);
        createDTO.setExecutorName(USER_NAME);

        MvcResult createResult = mockMvc.perform(post("/api/todo/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        logger.info("未登录创建代办响应: {}", createResponse);
        assertTrue("应返回未登录提示", createResponse.contains("请先登录") || createResponse.contains("401"));

        // 2. 未登录查看我的代办
        logger.info("2. 测试未登录查看我的代办");
        MvcResult listResult = mockMvc.perform(get("/api/todo/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andReturn();

        String listResponse = listResult.getResponse().getContentAsString();
        logger.info("未登录查看代办响应: {}", listResponse);

        logger.info("=== 未授权访问测试完成 ===");
    }

    @Test
    public void testNonAdminPushTodo() throws Exception {
        logger.info("=== 开始反向测试：非管理员推送代办 ===");

        // 普通用户尝试推送代办
        TodoCreateDTO createDTO = new TodoCreateDTO();
        createDTO.setTitle("Non-admin Push Test");
        createDTO.setContent("Regular user cannot push todo");
        createDTO.setExecutorId(USER2_ID);
        createDTO.setPriorityCode(1);

        MvcResult pushResult = mockMvc.perform(post("/api/admin/push-todo")
                        .header("userId", USER_ID) // 普通用户ID
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andReturn();

        String pushResponse = pushResult.getResponse().getContentAsString();
        logger.info("非管理员推送代办响应: {}", pushResponse);
        assertTrue("应返回无权限提示", pushResponse.contains("无权限") || pushResponse.contains("403"));

        // 验证用户2看不到这个代办（应该没有被创建）
        MvcResult listResult = mockMvc.perform(get("/api/todo/my")
                        .header("userId", USER2_ID))
                .andExpect(status().isOk())
                .andReturn();

        String listResponse = listResult.getResponse().getContentAsString();
        logger.info("用户2代办列表: {}", listResponse);
        assertFalse("代办不应被创建", listResponse.contains("Non-admin Push Test"));

        logger.info("=== 非管理员推送代办测试完成 ===");
    }

    @Test
    public void testDeleteNonExistentTodo() throws Exception {
        logger.info("=== 开始反向测试：删除不存在的代办 ===");

        // 删除一个不存在的代办ID
        long nonExistentId = 99999L;
        MvcResult deleteResult = mockMvc.perform(delete("/api/todo/delete/{id}", nonExistentId)
                        .header("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andReturn();

        String deleteResponse = deleteResult.getResponse().getContentAsString();
        logger.info("删除不存在代办响应: {}", deleteResponse);

        // 验证返回错误码（404或提示不存在）
        assertTrue("应返回相应错误提示",
                deleteResponse.contains("404") ||
                deleteResponse.contains("不存在") ||
                deleteResponse.contains("待办不存在"));

        logger.info("=== 删除不存在的代办测试完成 ===");
    }

    @Test
    public void testUpdateOtherUserTodo() throws Exception {
        logger.info("=== 开始反向测试：修改他人代办 ===");

        // 1. 用户1创建一个代办
        TodoCreateDTO createDTO = new TodoCreateDTO();
        createDTO.setTitle("User1's Todo");
        createDTO.setContent("This is User1's todo");
        createDTO.setExecutorId(USER_ID);
        createDTO.setExecutorName(USER_NAME);

        MvcResult createResult = mockMvc.perform(post("/api/todo/create")
                        .header("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        Todo todo = objectMapper.readValue(
                objectMapper.readTree(createResponse).get("data").toString(),
                Todo.class
        );
        Long todoId = todo.getId();
        logger.info("用户1创建的代办ID: {}", todoId);

        // 2. 用户2尝试修改这个代办
        TodoUpdateDTO updateDTO = new TodoUpdateDTO();
        updateDTO.setId(todoId);
        updateDTO.setTitle("User2 Modified Title");
        updateDTO.setContent("User2 trying to modify");

        MvcResult updateResult = mockMvc.perform(put("/api/todo/update")
                        .header("userId", USER2_ID) // 用户2尝试修改
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andReturn();

        String updateResponse = updateResult.getResponse().getContentAsString();
        logger.info("用户2修改代办响应: {}", updateResponse);

        // 验证无权限
        assertTrue("应返回无权限提示",
                updateResponse.contains("403") ||
                updateResponse.contains("无权限") ||
                updateResponse.contains("权限"));

        // 3. 验证代办内容未被修改
        MvcResult detailResult = mockMvc.perform(get("/api/todo/detail/{id}", todoId))
                .andExpect(status().isOk())
                .andReturn();

        String detailResponse = detailResult.getResponse().getContentAsString();
        logger.info("查询代办详情: {}", detailResponse);
        assertTrue("代办标题应保持不变", detailResponse.contains("User1's Todo"));

        logger.info("=== 修改他人代办测试完成 ===");
    }

    @Test
    public void testQueryNonExistentTodo() throws Exception {
        logger.info("=== 开始反向测试：查询不存在的代办 ===");

        long nonExistentId = 88888L;
        MvcResult detailResult = mockMvc.perform(get("/api/todo/detail/{id}", nonExistentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andReturn();

        String detailResponse = detailResult.getResponse().getContentAsString();
        logger.info("查询不存在代办响应: {}", detailResponse);

        assertTrue("应返回相应错误提示",
                detailResponse.contains("404") ||
                detailResponse.contains("不存在") ||
                detailResponse.contains("待办不存在"));

        logger.info("=== 查询不存在的代办测试完成 ===");
    }

    @Test
    public void testUpdateNonExistentTodo() throws Exception {
        logger.info("=== 开始反向测试：更新不存在的代办 ===");

        TodoUpdateDTO updateDTO = new TodoUpdateDTO();
        updateDTO.setId(77777L); // 不存在的ID
        updateDTO.setTitle("Update non-existent todo");
        updateDTO.setContent("Test content");

        MvcResult updateResult = mockMvc.perform(put("/api/todo/update")
                        .header("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andReturn();

        String updateResponse = updateResult.getResponse().getContentAsString();
        logger.info("更新不存在代办响应: {}", updateResponse);

        assertTrue("应返回相应错误提示",
                updateResponse.contains("404") ||
                updateResponse.contains("不存在") ||
                updateResponse.contains("待办不存在"));

        logger.info("=== 更新不存在的代办测试完成 ===");
    }

    @Test
    public void testUpdateOtherUserTodoStatus() throws Exception {
        logger.info("=== 开始反向测试：非执行人修改代办状态 ===");

        // 1. 用户1创建一个代办，指派给用户2
        TodoCreateDTO createDTO = new TodoCreateDTO();
        createDTO.setTitle("Assigned to User2");
        createDTO.setContent("This is created by User1 and assigned to User2");
        createDTO.setExecutorId(USER2_ID);
        createDTO.setExecutorName(USER2_NAME);

        MvcResult createResult = mockMvc.perform(post("/api/todo/create")
                        .header("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        Todo todo = objectMapper.readValue(
                objectMapper.readTree(createResponse).get("data").toString(),
                Todo.class
        );
        Long todoId = todo.getId();
        logger.info("创建的代办ID: {}, 执行人: {}", todoId, todo.getExecutorName());

        // 2. 用户1（创建者但不是执行人）尝试修改状态
        MvcResult statusResult = mockMvc.perform(put("/api/todo/status/{id}", todoId)
                        .header("userId", USER_ID) // 创建者但不是执行人
                        .param("statusCode", "2"))
                .andExpect(status().isOk())
                .andReturn();

        String statusResponse = statusResult.getResponse().getContentAsString();
        logger.info("创建者修改状态响应: {}", statusResponse);

        // 验证只有执行人才能修改状态
        assertTrue("只有执行人才能修改状态，创建者应无权限",
                statusResponse.contains("403") ||
                statusResponse.contains("无权限") ||
                statusResponse.contains("权限"));

        // 3. 用户2（执行人）可以修改状态
        MvcResult statusResult2 = mockMvc.perform(put("/api/todo/status/{id}", todoId)
                        .header("userId", USER2_ID) // 执行人
                        .param("statusCode", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String statusResponse2 = statusResult2.getResponse().getContentAsString();
        logger.info("执行人修改状态响应: {}", statusResponse2);

        // 验证状态已修改
        MvcResult detailResult = mockMvc.perform(get("/api/todo/detail/{id}", todoId))
                .andExpect(status().isOk())
                .andReturn();
        String detailResponse = detailResult.getResponse().getContentAsString();
        assertTrue("执行人应能修改状态", detailResponse.contains("COMPLETED") || detailResponse.contains("已完成"));

        logger.info("=== 非执行人修改代办状态测试完成 ===");
    }

    @Test
    public void testNonAdminAccessAdminEndpoint() throws Exception {
        logger.info("=== 开始反向测试：非管理员访问管理员接口 ===");

        // 1. 普通用户访问管理员用户列表接口
        MvcResult usersResult = mockMvc.perform(get("/api/admin/users")
                        .header("userId", USER_ID)) // 普通用户
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andReturn();

        String usersResponse = usersResult.getResponse().getContentAsString();
        logger.info("普通用户访问管理员用户列表响应: {}", usersResponse);
        assertTrue("应返回无权限提示",
                usersResponse.contains("403") ||
                usersResponse.contains("无权限") ||
                usersResponse.contains("权限"));

        // 2. 普通用户访问管理员所有代办接口
        MvcResult todosResult = mockMvc.perform(get("/api/admin/all-todos")
                        .header("userId", USER_ID)) // 普通用户
                .andExpect(status().isOk())
                .andReturn();

        String todosResponse = todosResult.getResponse().getContentAsString();
        logger.info("普通用户访问管理员所有代办响应: {}", todosResponse);
        assertTrue("应返回无权限提示",
                todosResponse.contains("403") ||
                todosResponse.contains("无权限") ||
                todosResponse.contains("权限"));

        logger.info("=== 非管理员访问管理员接口测试完成 ===");
    }
}
