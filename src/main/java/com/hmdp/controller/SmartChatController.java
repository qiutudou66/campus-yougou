package com.hmdp.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
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

    private final RestTemplate restTemplate;//发送HTTP请求的工具

    public SmartChatController() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();//连接网络
        factory.setConnectTimeout(5_000);   // AI 服务连不上时快速失败
        factory.setReadTimeout(60_000);     // 大模型生成要几十秒，读超时放宽
        this.restTemplate = new RestTemplate(factory);
    }

    @PostMapping
    public Result chat(@RequestBody(required = false) Map<String, String> body) {
        String question = body == null ? null : body.get("question");
        if (question == null || question.trim().isEmpty()) {
            return Result.fail("问题不能为空");
        }
        // 统一用 UTF-8 字节收发，避免 RestTemplate 默认 ISO-8859-1 把中文编乱
        HttpHeaders headers = new HttpHeaders();//造一个空请求头容器（一个能放多个"头名:值"的袋子）
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        byte[] reqBytes = JSONUtil.createObj().set("question", question).toString()
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
}
