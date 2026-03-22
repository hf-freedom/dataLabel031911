package com.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.todo.dto.TodoCreateDTO;
import com.todo.dto.TodoUpdateDTO;
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
public class TodoControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(TodoControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long NORMAL_USER_ID = 2L;
    private static final Long ADMIN_USER_ID = 1L;

    @BeforeEach
    public void setUp() {
        logger.info("========== 开始执行测试 ==========");
    }

    @Test
    @DisplayName("测试1: 用户创建待办 - 正常流程")
    public void testCreateTodo_Success() throws Exception {
        logger.info("【测试】用户创建待办 - 正常流程");

        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle("测试待办标题");
        dto.setContent("测试待办内容");
        dto.setExecutorId(NORMAL_USER_ID);
        dto.setPriorityCode(2);
        dto.setDueTime(System.currentTimeMillis() + 3600000);

        String requestBody = objectMapper.writeValueAsString(dto);
        logger.info("请求参数: {}", requestBody);

        MvcResult result = mockMvc.perform(post("/api/todo/create")
                .header("userId", NORMAL_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.title").value("测试待办标题"))
                .andExpect(jsonPath("$.data.content").value("测试待办内容"))
                .andExpect(jsonPath("$.data.executorId").value(NORMAL_USER_ID))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】用户创建待办成功");
    }

    @Test
    @DisplayName("测试2: 用户创建待办 - 未登录")
    public void testCreateTodo_NotLogin() throws Exception {
        logger.info("【测试】用户创建待办 - 未登录");

        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle("测试待办标题");
        dto.setContent("测试待办内容");

        String requestBody = objectMapper.writeValueAsString(dto);
        logger.info("请求参数: {}", requestBody);

        MvcResult result = mockMvc.perform(post("/api/todo/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】未登录用户无法创建待办");
    }

    @Test
    @DisplayName("测试3: 用户查询自己的待办列表")
    public void testGetMyTodos() throws Exception {
        logger.info("【测试】用户查询自己的待办列表");

        MvcResult result = mockMvc.perform(get("/api/todo/my")
                .header("userId", NORMAL_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】用户查询自己的待办列表成功");
    }

    @Test
    @DisplayName("测试4: 用户查询创建的待办列表")
    public void testGetCreatedTodos() throws Exception {
        logger.info("【测试】用户查询创建的待办列表");

        MvcResult result = mockMvc.perform(get("/api/todo/created")
                .header("userId", NORMAL_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】用户查询创建的待办列表成功");
    }

    @Test
    @DisplayName("测试5: 用户查询待办详情")
    public void testGetTodoDetail() throws Exception {
        logger.info("【测试】用户查询待办详情");

        Long todoId = createTodoAndGetId("查询详情测试待办");
        logger.info("查询待办ID: {}", todoId);

        MvcResult result = mockMvc.perform(get("/api/todo/detail/{id}", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】用户查询待办详情成功");
    }

    @Test
    @DisplayName("测试6: 用户更新待办 - 正常流程")
    public void testUpdateTodo_Success() throws Exception {
        logger.info("【测试】用户更新待办 - 正常流程");

        Long todoId = createTodoAndGetId("更新测试待办");
        logger.info("待办ID: {}", todoId);

        TodoUpdateDTO dto = new TodoUpdateDTO();
        dto.setId(todoId);
        dto.setTitle("更新后的标题");
        dto.setContent("更新后的内容");
        dto.setPriorityCode(1);

        String requestBody = objectMapper.writeValueAsString(dto);
        logger.info("请求参数: {}", requestBody);

        MvcResult result = mockMvc.perform(put("/api/todo/update")
                .header("userId", NORMAL_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】用户更新待办成功");
    }

    @Test
    @DisplayName("测试7: 用户更新待办 - 未登录")
    public void testUpdateTodo_NotLogin() throws Exception {
        logger.info("【测试】用户更新待办 - 未登录");

        TodoUpdateDTO dto = new TodoUpdateDTO();
        dto.setId(1L);
        dto.setTitle("更新后的标题");

        String requestBody = objectMapper.writeValueAsString(dto);
        logger.info("请求参数: {}", requestBody);

        MvcResult result = mockMvc.perform(put("/api/todo/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】未登录用户无法更新待办");
    }

    @Test
    @DisplayName("测试8: 用户更新待办优先级")
    public void testUpdateTodoPriority() throws Exception {
        logger.info("【测试】用户更新待办优先级");

        Long todoId = createTodoAndGetId("优先级测试待办");
        Integer priorityCode = 3;
        logger.info("待办ID: {}, 新优先级: {}", todoId, priorityCode);

        MvcResult result = mockMvc.perform(put("/api/todo/priority/{id}", todoId)
                .header("userId", NORMAL_USER_ID)
                .param("priorityCode", String.valueOf(priorityCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("优先级更新成功"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】用户更新待办优先级成功");
    }

    @Test
    @DisplayName("测试9: 用户更新待办状态")
    public void testUpdateTodoStatus() throws Exception {
        logger.info("【测试】用户更新待办状态");

        Long todoId = createTodoAndGetId("状态测试待办");
        Integer statusCode = 2;
        logger.info("待办ID: {}, 新状态: {}", todoId, statusCode);

        MvcResult result = mockMvc.perform(put("/api/todo/status/{id}", todoId)
                .header("userId", NORMAL_USER_ID)
                .param("statusCode", String.valueOf(statusCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("状态更新成功"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】用户更新待办状态成功");
    }

    @Test
    @DisplayName("测试10: 用户删除待办 - 正常流程")
    public void testDeleteTodo_Success() throws Exception {
        logger.info("【测试】用户删除待办 - 正常流程");

        Long todoId = createTodoAndGetId("删除测试待办");
        logger.info("删除待办ID: {}", todoId);

        MvcResult result = mockMvc.perform(delete("/api/todo/delete/{id}", todoId)
                .header("userId", NORMAL_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】用户删除待办成功");
    }

    @Test
    @DisplayName("测试11: 用户删除待办 - 未登录")
    public void testDeleteTodo_NotLogin() throws Exception {
        logger.info("【测试】用户删除待办 - 未登录");

        Long todoId = 1L;
        logger.info("删除待办ID: {}", todoId);

        MvcResult result = mockMvc.perform(delete("/api/todo/delete/{id}", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】未登录用户无法删除待办");
    }

    @Test
    @DisplayName("测试12: 用户删除不存在的待办")
    public void testDeleteTodo_NotFound() throws Exception {
        logger.info("【测试】用户删除不存在的待办");

        Long nonExistentTodoId = 9999L;
        logger.info("删除不存在的待办ID: {}", nonExistentTodoId);

        MvcResult result = mockMvc.perform(delete("/api/todo/delete/{id}", nonExistentTodoId)
                .header("userId", NORMAL_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("待办不存在"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】删除不存在待办返回正确错误信息");
    }

    private Long createTodoAndGetId(String title) throws Exception {
        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle(title);
        dto.setContent("测试内容");
        dto.setExecutorId(NORMAL_USER_ID);
        dto.setPriorityCode(2);
        dto.setDueTime(System.currentTimeMillis() + 3600000);

        MvcResult result = mockMvc.perform(post("/api/todo/create")
                .header("userId", NORMAL_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.path("data").path("id").asLong();
    }
}
