package com.resume.module.resume.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class InterviewWsHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Long userId = InterviewWebSocketHandler.userIdFromToken(
                InterviewWebSocketHandler.parseToken(request.getURI()));
        Long sessionId = InterviewWebSocketHandler.parseLongQuery(request.getURI(), "sessionId");
        if (userId == null || sessionId == null) {
            return false;
        }
        attributes.put("userId", userId);
        attributes.put("sessionId", sessionId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
