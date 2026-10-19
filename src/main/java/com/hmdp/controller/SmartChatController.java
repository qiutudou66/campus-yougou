package com.hmdp.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 点评 AI 助手入口：Java 通过 HTTP 调用 Python FastAPI 服务（RAG 问答）。
 * Python 服务地址 localhost:8000，走 Windows→WSL2 的 localhost 转发。
 */
@RestController
@RequestMapping("/chat")
@Slf4j
public class SmartChatController {

    private static final String AI_URL = "http://localhost:8000/chat";
    private static final String AI_IMAGE_URL = "http://localhost:8000/chat-image";
    /** 图片大小上限，和 application.yaml 的 multipart 配置保持一致 */
    private static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    private final RestTemplate restTemplate;//发送HTTP请求的工具

    public SmartChatController() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();//连接网络
        factory.setConnectTimeout(5_000);   // AI 服务连不上时快速失败
        factory.setReadTimeout(60_000);     // 大模型生成要几十秒，读超时放宽
        this.restTemplate = new RestTemplate(factory);
    }

    /** 多轮对话最多带几轮历史（一问一答算 2 条） */
    private static final int MAX_HISTORY = 6;
    /** 单条历史内容长度上限，防止前端塞超长文本撑爆 prompt */
    private static final int MAX_CONTENT_LENGTH = 2000;

    @PostMapping
    public Result chat(@RequestBody(required = false) Map<String, Object> body) {
        String question = body == null || body.get("question") == null ? null : body.get("question").toString();
        if (question == null || question.trim().isEmpty()) {
            return Result.fail("问题不能为空");
        }
        List<Map<String, String>> history = parseHistory(body.get("history"));
        // 统一用 UTF-8 字节收发，避免 RestTemplate 默认 ISO-8859-1 把中文编乱
        HttpHeaders headers = new HttpHeaders();//造一个空请求头容器（一个能放多个"头名:值"的袋子）
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        byte[] reqBytes = JSONUtil.createObj()
                .set("question", question)
                .set("history", history)
                .toString()
                .getBytes(StandardCharsets.UTF_8);
        try {
            byte[] respBytes = restTemplate.exchange(
                    // AI 服务地址，POST 方法，请求头和请求体，响应“原始字节”形式
                    AI_URL, HttpMethod.POST, new HttpEntity<>(reqBytes, headers), byte[].class)
                    .getBody();
            //parseObj把字符串转成可读对象
            JSONObject ai = JSONUtil.parseObj(new String(respBytes, StandardCharsets.UTF_8));//解析AI服务返回的JSON字节
            //异常处理
            if (!Boolean.TRUE.equals(ai.getBool("success"))) {
                String msg = ai.getStr("message");
                log.error("AI 服务处理失败: {}", msg);
                return Result.fail("AI 服务处理失败" + (msg == null ? "" : "：" + msg));
            }
            JSONObject data = ai.getJSONObject("data");
            return Result.ok(data == null ? null : data.getStr("answer"));
        } catch (Exception e) {
            log.error("调用 Python AI 服务异常", e);
            return Result.fail("AI 服务暂时不可用，请稍后再试");
        }
    }

    /**
     * 图片识别：前端上传照片 → 转发给 FastAPI /chat-image（视觉模型认店名 → 查实时数据 → 生成推荐）。
     * 用 multipart/form-data 转发，RestTemplate 默认的 FormHttpMessageConverter 会处理好。
     */
    @PostMapping("/image")
    public Result chatImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("图片不能为空");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            return Result.fail("图片不能超过 10MB");
        }
        try {
            // Resource 需要带 filename，FastAPI 的 UploadFile 依赖它判断文件名
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() == null ? "photo.jpg" : file.getOriginalFilename();
                }
            };
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("file", resource);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            byte[] respBytes = restTemplate.exchange(
                    AI_IMAGE_URL, HttpMethod.POST, new HttpEntity<>(form, headers), byte[].class)
                    .getBody();
            JSONObject ai = JSONUtil.parseObj(new String(respBytes, StandardCharsets.UTF_8));
            if (!Boolean.TRUE.equals(ai.getBool("success"))) {
                String msg = ai.getStr("message");
                log.error("AI 图片服务处理失败: {}", msg);
                return Result.fail("AI 服务处理失败" + (msg == null ? "" : "：" + msg));
            }
            JSONObject data = ai.getJSONObject("data");
            return Result.ok(data == null ? null : data.getStr("answer"));
        } catch (Exception e) {
            log.error("调用 Python AI 图片服务异常", e);
            return Result.fail("AI 服务暂时不可用，请稍后再试");
        }
    }

    /**
     * 把前端传来的历史记录清洗一遍再转发：只留 user/assistant 两种角色，
     * 过滤空内容，截断超长文本，并且最多只保留最近 MAX_HISTORY 条。
     */
    private List<Map<String, String>> parseHistory(Object raw) {
        List<Map<String, String>> history = new ArrayList<>();
        if (!(raw instanceof List)) {
            return history;
        }
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> m = (Map<?, ?>) item;
            String role = m.get("role") == null ? null : m.get("role").toString();
            String content = m.get("content") == null ? null : m.get("content").toString();
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            if (content == null || content.trim().isEmpty()) {
                continue;
            }
            if (content.length() > MAX_CONTENT_LENGTH) {
                content = content.substring(0, MAX_CONTENT_LENGTH);
            }
            Map<String, String> msg = new HashMap<>();
            msg.put("role", role);
            msg.put("content", content);
            history.add(msg);
        }
        if (history.size() > MAX_HISTORY) {
            history = history.subList(history.size() - MAX_HISTORY, history.size());
        }
        return history;
    }
}
