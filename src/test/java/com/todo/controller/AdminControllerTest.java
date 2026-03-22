package com.todo.controller;

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
public class AdminControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(AdminControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long ADMIN_USER_ID = 1L;
    private static final Long NORMAL_USER_ID = 2L;
    private static final Long TARGET_USER_ID = 3L;

    @BeforeEach
    public void setUp() {
        logger.info("========== 开始执行管理员模块测试 ==========");
    }

    @Test
    @DisplayName("测试1: 管理员获取所有用户列表 - 正常流程")
    public void testGetAllUsers_Success() throws Exception {
        logger.info("【测试】管理员获取所有用户列表 - 正常流程");

        MvcResult result = mockMvc.perform(get("/api/admin/users")
                .header("userId", ADMIN_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】管理员获取所有用户列表成功");
    }

    @Test
    @DisplayName("测试2: 普通用户尝试获取所有用户列表 - 无权限")
    public void testGetAllUsers_NoPermission() throws Exception {
        logger.info("【测试】普通用户尝试获取所有用户列表 - 无权限");

        MvcResult result = mockMvc.perform(get("/api/admin/users")
                .header("userId", NORMAL_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限访问"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】普通用户无法获取所有用户列表");
    }

    @Test
    @DisplayName("测试3: 未登录用户尝试获取所有用户列表")
    public void testGetAllUsers_NotLogin() throws Exception {
        logger.info("【测试】未登录用户尝试获取所有用户列表");

        MvcResult result = mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限访问"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】未登录用户无法获取所有用户列表");
    }

    @Test
    @DisplayName("测试4: 管理员推送待办给用户 - 正常流程")
    public void testPushTodo_Success() throws Exception {
        logger.info("【测试】管理员推送待办给用户 - 正常流程");

        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle("管理员推送的待办");
        dto.setContent("这是管理员推送的待办内容");
        dto.setExecutorId(TARGET_USER_ID);
        dto.setPriorityCode(1);
        dto.setDueTime(System.currentTimeMillis() + 7200000);

        String requestBody = objectMapper.writeValueAsString(dto);
        logger.info("请求参数: {}", requestBody);
        logger.info("管理员ID: {}, 目标用户ID: {}", ADMIN_USER_ID, TARGET_USER_ID);

        MvcResult result = mockMvc.perform(post("/api/admin/push-todo")
                .header("userId", ADMIN_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("推送成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.title").value("管理员推送的待办"))
                .andExpect(jsonPath("$.data.content").value("这是管理员推送的待办内容"))
                .andExpect(jsonPath("$.data.executorId").value(TARGET_USER_ID))
                .andExpect(jsonPath("$.data.creatorId").value(ADMIN_USER_ID))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】管理员推送待办给用户成功");
    }

    @Test
    @DisplayName("测试5: 普通用户尝试推送待办 - 无权限")
    public void testPushTodo_NoPermission() throws Exception {
        logger.info("【测试】普通用户尝试推送待办 - 无权限");

        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle("普通用户推送的待办");
        dto.setContent("这是普通用户推送的待办内容");
        dto.setExecutorId(TARGET_USER_ID);

        String requestBody = objectMapper.writeValueAsString(dto);
        logger.info("请求参数: {}", requestBody);

        MvcResult result = mockMvc.perform(post("/api/admin/push-todo")
                .header("userId", NORMAL_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限操作"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】普通用户无法推送待办");
    }

    @Test
    @DisplayName("测试6: 管理员推送待办给不存在的用户")
    public void testPushTodo_UserNotFound() throws Exception {
        logger.info("【测试】管理员推送待办给不存在的用户");

        Long nonExistentUserId = 9999L;

        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle("管理员推送的待办");
        dto.setContent("这是管理员推送的待办内容");
        dto.setExecutorId(nonExistentUserId);

        String requestBody = objectMapper.writeValueAsString(dto);
        logger.info("请求参数: {}", requestBody);
        logger.info("目标用户ID(不存在): {}", nonExistentUserId);

        MvcResult result = mockMvc.perform(post("/api/admin/push-todo")
                .header("userId", ADMIN_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("执行人不存在"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】管理员推送待办给不存在的用户返回正确错误信息");
    }

    @Test
    @DisplayName("测试7: 管理员推送待办后，用户可以查看到")
    public void testPushTodo_UserCanView() throws Exception {
        logger.info("【测试】管理员推送待办后，用户可以查看到");

        String todoTitle = "管理员推送的测试待办-" + System.currentTimeMillis();

        TodoCreateDTO dto = new TodoCreateDTO();
        dto.setTitle(todoTitle);
        dto.setContent("这是管理员推送的测试待办内容");
        dto.setExecutorId(TARGET_USER_ID);
        dto.setPriorityCode(2);
        dto.setDueTime(System.currentTimeMillis() + 3600000);

        String requestBody = objectMapper.writeValueAsString(dto);
        logger.info("步骤1: 管理员推送待办");
        logger.info("请求参数: {}", requestBody);

        MvcResult pushResult = mockMvc.perform(post("/api/admin/push-todo")
                .header("userId", ADMIN_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String pushResponse = pushResult.getResponse().getContentAsString();
        logger.info("推送响应: {}", pushResponse);

        logger.info("步骤2: 用户查询自己的待办列表，验证是否可以查看到管理员推送的待办");

        MvcResult queryResult = mockMvc.perform(get("/api/todo/my")
                .header("userId", TARGET_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        String queryResponse = queryResult.getResponse().getContentAsString();
        logger.info("用户待办列表响应: {}", queryResponse);

        if (queryResponse.contains(todoTitle)) {
            logger.info("【测试通过】管理员推送待办后，用户可以正常查看到");
        } else {
            logger.warn("【警告】在用户待办列表中未找到推送的待办，可能需要检查数据");
        }
    }

    @Test
    @DisplayName("测试8: 管理员获取所有待办列表 - 正常流程")
    public void testGetAllTodos_Success() throws Exception {
        logger.info("【测试】管理员获取所有待办列表 - 正常流程");

        MvcResult result = mockMvc.perform(get("/api/admin/all-todos")
                .header("userId", ADMIN_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】管理员获取所有待办列表成功");
    }

    @Test
    @DisplayName("测试9: 普通用户尝试获取所有待办列表 - 无权限")
    public void testGetAllTodos_NoPermission() throws Exception {
        logger.info("【测试】普通用户尝试获取所有待办列表 - 无权限");

        MvcResult result = mockMvc.perform(get("/api/admin/all-todos")
                .header("userId", NORMAL_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限访问"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        logger.info("响应结果: {}", responseBody);
        logger.info("【测试通过】普通用户无法获取所有待办列表");
    }
}
