package com.king.KingAgent.advisor;

import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;
import java.util.HashMap;
import java.util.Map;

/**
 * 重读提示词 Advisor
 * 通过重复用户输入两次增强模型理解
 * 设计原理：
 * 1. 强制模型关注原始问题
 * 2. 提供冗余信息提升推理准确性
 * 3. 保持上下文完整性
 */
public class ReReadingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    /**
     * 请求前处理：改写用户提示词
     * 1. 保存原始用户输入到参数
     * 2. 构建重复两次的提示词模板
     */
    private AdvisedRequest before(AdvisedRequest advisedRequest) {
        // 创建用户参数副本，保留原始参数
        Map<String, Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
        advisedUserParams.put("re2_input_query", advisedRequest.userText());

        // 构建增强型提示词，重复输入内容两次
        return AdvisedRequest.from(advisedRequest)
            .userText("""
                {re2_input_query}
                Read the question again: {re2_input_query}
                """)
            .userParams(advisedUserParams)
            .build();
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 同步调用处理
        return chain.nextAroundCall(this.before(advisedRequest));
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        // 流式调用处理
        return chain.nextAroundStream(this.before(advisedRequest));
    }

    @Override
    public int getOrder() {
        // 执行顺序优先级，数值越小越早执行
        return 0;
    }

    @Override
    public String getName() {
        // 返回组件名称用于标识
        return this.getClass().getSimpleName();
    }
}