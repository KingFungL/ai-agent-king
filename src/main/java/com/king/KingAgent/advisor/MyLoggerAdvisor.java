package com.king.KingAgent.advisor;


import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.MessageAggregator;

/**
 * 自定义日志 Advisor
 * 实现同步和流式调用的日志记录功能
 * 1. 使用Slf4j注解自动生成日志记录器
 * 2. 实现CallAroundAdvisor处理同步调用
 * 3. 实现StreamAroundAdvisor处理响应式流
 */
@Slf4j
public class MyLoggerAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    @Override
    public String getName() {
        // 返回advisor名称，用于标识和排序
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        // 返回执行顺序，数值越小越早执行
        return 0;
    }

    private AdvisedRequest before(AdvisedRequest request) {
        // 在请求前记录用户输入内容
        log.info("[AI请求] 用户输入: {}", request.userText());
        return request;
    }

    private void observeAfter(AdvisedResponse advisedResponse) {
        // 记录AI响应结果
        log.info("[AI响应] 模型输出: {}", 
            advisedResponse.response().getResult().getOutput().getText());
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 同步调用的环绕处理
        advisedRequest = before(advisedRequest);
        
        // 执行后续的advisor链
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);
        
        // 响应后处理
        observeAfter(advisedResponse);
        
        return advisedResponse;
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        // 流式调用的环绕处理
        advisedRequest = before(advisedRequest);
        
        // 获取响应式流
        Flux<AdvisedResponse> advisedResponses = chain.nextAroundStream(advisedRequest);
        
        // 使用MessageAggregator聚合流式响应并记录最终结果
        return new MessageAggregator().aggregateAdvisedResponse(advisedResponses, this::observeAfter);
    }
}