package com.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.cache.TodoCache;
import com.todo.cache.UserCache;
import com.todo.dto.TodoCreateDTO;
import com.todo.dto.TodoUpdateDTO;
import com.todo.entity.Priority;
import com.todo.entity.Todo;
import com.todo.entity.TodoStatus;
import com.todo.entity.User;
import com.todo.service.ISendUtil;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TodoIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(TodoIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserCache userCache;

    @Autowired
    private TodoCache todoCache;

    @MockBean
    private ISendUtil sendUtil;

    private static final Long ADMIN_USER_ID = 1L;
    private static final Long NORMAL_USER_ID = 2L;
    private static final Long OTHER_USER_ID = 3L;

    private static final String ADMIN_HEADER = "userId";
    
    private static AtomicReference<Long> createdTodoId = new AtomicReference<>();
    private static AtomicReference<Long> pushedTodoId = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        logger.info("========== 测试开始 ==========");
    }

    @AfterEach
    void tearDown() {
        logger.info("========== 测试结束 ==========");
    }

    @Test
    @Order(1)
    @DisplayName("1. 用户创建待办 - 正常流程")
    void testCreateTodo_Success() throws Exception {
        logger.info(">>> 测试用户创建待办功能");

        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle("Test Todo Task");
        dto.setContent("This is test todo content");
        dto.setExecutorId(NORMAL_USER_ID);
        dto.setExecutorName("zhangsan");
        dto.setPriorityCode(2);
        dto.setDueTime(System.currentTimeMillis() + 60 * 60 * 1000);

        logger.info("Request params: {}", objectMapper.writeValueAsString(dto));

        MvcResult result = mockMvc.perform(post("/api/todo/create")
                .header(ADMIN_HEADER, NORMAL_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        Todo createdTodo = objectMapper.readValue(
                objectMapper.readTree(response).get("data").toString(),
                Todo.class
        );
        createdTodoId.set(createdTodo.getId());
        logger.info("Created todo ID: {}", createdTodo.getId());
        logger.info("Created todo detail: {}", createdTodo);

        assertNotNull(createdTodo.getId());
        assertEquals("Test Todo Task", createdTodo.getTitle());
        assertEquals(TodoStatus.PENDING, createdTodo.getStatus());
        assertFalse(createdTodo.getNotified());

        logger.info("<<< User create todo test passed");
    }

    @Test
    @Order(2)
    @DisplayName("2. 用户查询自己的待办列表")
    void testGetMyTodos_Success() throws Exception {
        logger.info(">>> 测试用户查询自己的待办列表");

        MvcResult result = mockMvc.perform(get("/api/todo/my")
                .header(ADMIN_HEADER, NORMAL_USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        List<Todo> todos = objectMapper.readValue(
                objectMapper.readTree(response).get("data").toString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Todo.class)
        );

        logger.info("Found todo count: {}", todos.size());
        assertFalse(todos.isEmpty(), "Todo list should not be empty");

        logger.info("<<< User query todo list test passed");
    }

    @Test
    @Order(3)
    @DisplayName("3. 用户查询待办详情")
    void testGetTodoDetail_Success() throws Exception {
        logger.info(">>> 测试用户查询待办详情");

        Long todoId = createdTodoId.get();
        logger.info("Query todo ID: {}", todoId);

        MvcResult result = mockMvc.perform(get("/api/todo/detail/" + todoId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(todoId))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        logger.info("<<< User query todo detail test passed");
    }

    @Test
    @Order(4)
    @DisplayName("4. 用户更新待办 - 正常流程")
    void testUpdateTodo_Success() throws Exception {
        logger.info(">>> 测试用户更新待办功能");

        Long todoId = createdTodoId.get();
        logger.info("Update todo ID: {}", todoId);

        TodoUpdateDTO dto = new TodoUpdateDTO();
        dto.setId(todoId);
        dto.setTitle("Updated Todo Title");
        dto.setContent("Updated Todo Content");
        dto.setPriorityCode(3);
        dto.setDueTime(System.currentTimeMillis() + 2 * 60 * 60 * 1000);

        logger.info("Request params: {}", objectMapper.writeValueAsString(dto));

        MvcResult result = mockMvc.perform(put("/api/todo/update")
                .header(ADMIN_HEADER, NORMAL_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        Todo updatedTodo = objectMapper.readValue(
                objectMapper.readTree(response).get("data").toString(),
                Todo.class
        );
        assertEquals("Updated Todo Title", updatedTodo.getTitle());
        assertEquals("Updated Todo Content", updatedTodo.getContent());
        assertEquals(Priority.HIGH, updatedTodo.getPriority());

        logger.info("<<< User update todo test passed");
    }

    @Test
    @Order(5)
    @DisplayName("5. 用户更新待办优先级")
    void testUpdatePriority_Success() throws Exception {
        logger.info(">>> 测试用户更新待办优先级");

        Long todoId = createdTodoId.get();
        logger.info("Update priority todo ID: {}", todoId);

        MvcResult result = mockMvc.perform(put("/api/todo/priority/" + todoId)
                .param("priorityCode", "1")
                .header(ADMIN_HEADER, NORMAL_USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("优先级更新成功"))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        logger.info("<<< User update priority test passed");
    }

    @Test
    @Order(6)
    @DisplayName("6. 用户更新待办状态")
    void testUpdateStatus_Success() throws Exception {
        logger.info(">>> 测试用户更新待办状态");

        Long todoId = createdTodoId.get();
        logger.info("Update status todo ID: {}", todoId);

        MvcResult result = mockMvc.perform(put("/api/todo/status/" + todoId)
                .param("statusCode", "1")
                .header(ADMIN_HEADER, NORMAL_USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("状态更新成功"))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        logger.info("<<< User update status test passed");
    }

    @Test
    @Order(7)
    @DisplayName("7. 用户删除待办 - 正常流程")
    void testDeleteTodo_Success() throws Exception {
        logger.info(">>> 测试用户删除待办功能");

        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle("Todo To Be Deleted");
        dto.setContent("This todo will be deleted");
        dto.setExecutorId(NORMAL_USER_ID);
        dto.setExecutorName("zhangsan");

        MvcResult createResult = mockMvc.perform(post("/api/todo/create")
                .header(ADMIN_HEADER, NORMAL_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(dto)))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Todo createdTodo = objectMapper.readValue(
                objectMapper.readTree(createResponse).get("data").toString(),
                Todo.class
        );
        Long todoIdToDelete = createdTodo.getId();
        logger.info("Created todo to delete ID: {}", todoIdToDelete);

        MvcResult deleteResult = mockMvc.perform(delete("/api/todo/delete/" + todoIdToDelete)
                .header(ADMIN_HEADER, NORMAL_USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"))
                .andReturn();

        String response = deleteResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        MvcResult verifyResult = mockMvc.perform(get("/api/todo/detail/" + todoIdToDelete))
                .andReturn();
        String verifyResponse = verifyResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Verify delete result: {}", verifyResponse);
        assertEquals(404, objectMapper.readTree(verifyResponse).get("code").asInt());

        logger.info("<<< User delete todo test passed");
    }

    @Test
    @Order(10)
    @DisplayName("8. 未登录用户创建待办 - 应返回401")
    void testCreateTodo_WithoutLogin() throws Exception {
        logger.info(">>> 测试未登录用户创建待办");

        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle("Unauthorized Test");
        dto.setContent("Test content");

        mockMvc.perform(post("/api/todo/create")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"));

        logger.info("<<< Unauthorized user create todo test passed");
    }

    @Test
    @Order(11)
    @DisplayName("9. 用户无权限修改他人待办")
    void testUpdateTodo_NoPermission() throws Exception {
        logger.info(">>> 测试用户无权限修改他人待办");

        Long todoId = createdTodoId.get();
        logger.info("Attempt to modify todo ID: {}, current user ID: {}", todoId, OTHER_USER_ID);

        TodoUpdateDTO dto = new TodoUpdateDTO();
        dto.setId(todoId);
        dto.setTitle("Attempt to modify");

        mockMvc.perform(put("/api/todo/update")
                .header(ADMIN_HEADER, OTHER_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限修改此待办"));

        logger.info("<<< User no permission to modify others todo test passed");
    }

    @Test
    @Order(20)
    @DisplayName("10. 管理员推送待办给用户 - 正常流程")
    void testAdminPushTodo_Success() throws Exception {
        logger.info(">>> 测试管理员推送待办给用户");

        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle("Admin Pushed Task");
        dto.setContent("This is admin pushed todo content");
        dto.setExecutorId(OTHER_USER_ID);
        dto.setPriorityCode(3);
        dto.setDueTime(System.currentTimeMillis() + 60 * 60 * 1000);

        logger.info("Request params: {}", objectMapper.writeValueAsString(dto));
        logger.info("Admin ID: {}, Target user ID: {}", ADMIN_USER_ID, OTHER_USER_ID);

        MvcResult result = mockMvc.perform(post("/api/admin/push-todo")
                .header(ADMIN_HEADER, ADMIN_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("推送成功"))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        Todo pushedTodo = objectMapper.readValue(
                objectMapper.readTree(response).get("data").toString(),
                Todo.class
        );
        pushedTodoId.set(pushedTodo.getId());
        assertEquals(ADMIN_USER_ID, pushedTodo.getCreatorId());
        assertEquals(OTHER_USER_ID, pushedTodo.getExecutorId());

        logger.info("<<< Admin push todo test passed");
    }

    @Test
    @Order(21)
    @DisplayName("11. 用户查看管理员推送的待办")
    void testUserViewPushedTodo() throws Exception {
        logger.info(">>> 测试用户查看管理员推送的待办");

        Long expectedTodoId = pushedTodoId.get();
        logger.info("Expected pushed todo ID: {}", expectedTodoId);

        MvcResult result = mockMvc.perform(get("/api/todo/my")
                .header(ADMIN_HEADER, OTHER_USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        List<Todo> todos = objectMapper.readValue(
                objectMapper.readTree(response).get("data").toString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Todo.class)
        );

        logger.info("Found {} todos for user {}", todos.size(), OTHER_USER_ID);
        for (Todo t : todos) {
            logger.info("Todo: id={}, title={}, executorId={}", t.getId(), t.getTitle(), t.getExecutorId());
        }

        boolean hasPushedTodo = todos.stream()
                .anyMatch(t -> "Admin Pushed Task".equals(t.getTitle()) && t.getId().equals(expectedTodoId));
        assertTrue(hasPushedTodo, "User should see admin pushed todo");

        logger.info("<<< User view admin pushed todo test passed");
    }

    @Test
    @Order(22)
    @DisplayName("12. 非管理员无法推送待办")
    void testNonAdminPushTodo_Forbidden() throws Exception {
        logger.info(">>> 测试非管理员无法推送待办");

        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle("Non-admin attempt to push");
        dto.setContent("Test content");
        dto.setExecutorId(OTHER_USER_ID);

        mockMvc.perform(post("/api/admin/push-todo")
                .header(ADMIN_HEADER, NORMAL_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限操作"));

        logger.info("<<< Non-admin cannot push todo test passed");
    }

    @Test
    @Order(30)
    @DisplayName("13. 待办快到期时发送提醒消息")
    void testTodoDueReminder() throws Exception {
        logger.info(">>> 测试待办快到期时发送提醒消息");

        reset(sendUtil);

        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle("Due Soon Todo");
        dto.setContent("This todo is due soon, should trigger reminder");
        dto.setExecutorId(NORMAL_USER_ID);
        dto.setExecutorName("zhangsan");
        dto.setPriorityCode(2);
        long dueTime = System.currentTimeMillis() + 30 * 1000;
        dto.setDueTime(dueTime);

        logger.info("Request params: {}", objectMapper.writeValueAsString(dto));
        logger.info("Set due time: {} (30 seconds later)", new Date(dueTime));

        MvcResult result = mockMvc.perform(post("/api/todo/create")
                .header(ADMIN_HEADER, NORMAL_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        Todo createdTodo = objectMapper.readValue(
                objectMapper.readTree(response).get("data").toString(),
                Todo.class
        );
        Long todoId = createdTodo.getId();
        logger.info("Created todo ID: {}", todoId);

        Thread.sleep(35000);

        verify(sendUtil, atLeastOnce()).send(anyString());
        logger.info("Verified ISendUtil.send() was called");

        Todo updatedTodo = todoCache.findById(todoId);
        logger.info("Todo notified status: {}", updatedTodo.getNotified());
        assertTrue(updatedTodo.getNotified(), "Todo should be marked as notified");

        logger.info("<<< Todo due reminder test passed");
    }

    @Test
    @Order(31)
    @DisplayName("14. 待办到期后状态变为EXPIRED")
    void testTodoExpiredStatus() throws Exception {
        logger.info(">>> 测试待办到期后状态变化");

        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle("Expired Todo");
        dto.setContent("This todo has expired");
        dto.setExecutorId(NORMAL_USER_ID);
        dto.setExecutorName("zhangsan");
        dto.setPriorityCode(2);
        long dueTime = System.currentTimeMillis() + 5 * 1000;
        dto.setDueTime(dueTime);

        logger.info("Request params: {}", objectMapper.writeValueAsString(dto));
        logger.info("Set due time: {} (5 seconds later)", new Date(dueTime));

        MvcResult result = mockMvc.perform(post("/api/todo/create")
                .header(ADMIN_HEADER, NORMAL_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        Todo createdTodo = objectMapper.readValue(
                objectMapper.readTree(response).get("data").toString(),
                Todo.class
        );
        Long todoId = createdTodo.getId();
        logger.info("Created todo ID: {}, initial status: {}", todoId, createdTodo.getStatus());

        assertEquals(TodoStatus.PENDING, createdTodo.getStatus(), "Initial status should be PENDING");

        Thread.sleep(10000);

        Todo expiredTodo = todoCache.findById(todoId);
        logger.info("Todo status after waiting: {}", expiredTodo.getStatus());
        assertEquals(TodoStatus.EXPIRED, expiredTodo.getStatus(), "Todo status should be EXPIRED after due");

        logger.info("<<< Todo expired status test passed");
    }

    @Test
    @Order(40)
    @DisplayName("15. 管理员查看所有待办")
    void testAdminGetAllTodos() throws Exception {
        logger.info(">>> 测试管理员查看所有待办");

        MvcResult result = mockMvc.perform(get("/api/admin/all-todos")
                .header(ADMIN_HEADER, ADMIN_USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        List<Todo> todos = objectMapper.readValue(
                objectMapper.readTree(response).get("data").toString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Todo.class)
        );

        logger.info("Admin sees all todo count: {}", todos.size());
        assertFalse(todos.isEmpty(), "Todo list should not be empty");

        logger.info("<<< Admin view all todos test passed");
    }

    @Test
    @Order(41)
    @DisplayName("16. 管理员查看所有用户")
    void testAdminGetAllUsers() throws Exception {
        logger.info(">>> 测试管理员查看所有用户");

        MvcResult result = mockMvc.perform(get("/api/admin/users")
                .header(ADMIN_HEADER, ADMIN_USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        logger.info("<<< Admin view all users test passed");
    }

    @Test
    @Order(50)
    @DisplayName("17. 查询用户创建的待办")
    void testGetCreatedTodos() throws Exception {
        logger.info(">>> 测试查询用户创建的待办");

        MvcResult result = mockMvc.perform(get("/api/todo/created")
                .header(ADMIN_HEADER, NORMAL_USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        logger.info("Response: {}", response);

        logger.info("<<< Query user created todos test passed");
    }

    @Test
    @Order(60)
    @DisplayName("18. 查询不存在的待办详情")
    void testGetTodoDetail_NotFound() throws Exception {
        logger.info(">>> 测试查询不存在的待办详情");

        mockMvc.perform(get("/api/todo/detail/99999"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("待办不存在"));

        logger.info("<<< Query non-existent todo detail test passed");
    }

    @Test
    @Order(61)
    @DisplayName("19. 删除不存在的待办")
    void testDeleteTodo_NotFound() throws Exception {
        logger.info(">>> 测试删除不存在的待办");

        mockMvc.perform(delete("/api/todo/delete/99999")
                .header(ADMIN_HEADER, NORMAL_USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));

        logger.info("<<< Delete non-existent todo test passed");
    }
}
