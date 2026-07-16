package com.resume.module.resume.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.resume.common.BusinessException;
import com.resume.module.resume.dto.InterviewAnswerResultVO;
import com.resume.module.resume.dto.InterviewMessageVO;
import com.resume.module.resume.dto.InterviewSessionVO;
import com.resume.module.resume.service.InterviewService;
import com.resume.user_identify.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sprint3 he：面试 WebSocket。连接 /ws/interview?token=&sessionId=
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewWebSocketHandler extends TextWebSocketHandler {

    private final InterviewService interviewService;
    private final ObjectMapper objectMapper;

    /** sessionId -> ws connections */
    private final Map<Long, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        Long interviewId = (Long) session.getAttributes().get("sessionId");
        if (userId == null || interviewId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("missing auth or sessionId"));
            return;
        }
        // 校验归属
        InterviewSessionVO detail = interviewService.getSession(userId, interviewId);
        rooms.computeIfAbsent(interviewId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
        send(session, "STATE", interviewId, Map.of(
                "state", detail.getState(),
                "questionIndex", detail.getQuestionIndex(),
                "maxQuestions", detail.getMaxQuestions(),
                "lastSeq", detail.getLastSeq()
        ));
        log.info("Interview WS connected userId={}, sessionId={}", userId, interviewId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        Long interviewId = (Long) session.getAttributes().get("sessionId");
        JsonNode root = objectMapper.readTree(message.getPayload());
        String type = text(root, "type");
        JsonNode payload = root.has("payload") ? root.get("payload") : root;

        try {
            switch (type == null ? "" : type.toUpperCase()) {
                case "PING" -> send(session, "PONG", interviewId, Map.of());
                case "ANSWER" -> {
                    String answer = text(payload, "answer");
                    String clientMsgId = text(payload, "clientMsgId");
                    InterviewAnswerResultVO result = interviewService.answer(userId, interviewId, answer, clientMsgId);
                    send(session, "ANSWER_RESULT", interviewId, result);
                    broadcast(interviewId, "STATE", Map.of(
                            "state", result.getState(),
                            "questionIndex", result.getQuestionIndex(),
                            "finished", result.isFinished()
                    ));
                }
                case "END" -> {
                    InterviewSessionVO vo = interviewService.end(userId, interviewId);
                    send(session, "ANSWER_RESULT", interviewId, Map.of(
                            "finished", true,
                            "state", vo.getState(),
                            "report", vo.getReport()
                    ));
                    broadcast(interviewId, "STATE", Map.of("state", vo.getState(), "finished", true));
                }
                case "RESUME" -> {
                    int afterSeq = payload.has("afterSeq") ? payload.get("afterSeq").asInt(0) : 0;
                    List<InterviewMessageVO> missed = interviewService.messagesAfterSeq(userId, interviewId, afterSeq);
                    InterviewSessionVO detail = interviewService.getSession(userId, interviewId);
                    send(session, "SNAPSHOT", interviewId, Map.of(
                            "messages", missed,
                            "lastSeq", detail.getLastSeq(),
                            "state", detail.getState(),
                            "questionIndex", detail.getQuestionIndex()
                    ));
                }
                default -> send(session, "ERROR", interviewId, Map.of("code", 400, "message", "未知消息类型: " + type));
            }
        } catch (BusinessException e) {
            send(session, "ERROR", interviewId, Map.of("code", e.getCode(), "message", e.getMessage()));
        } catch (Exception e) {
            log.warn("Interview WS error: {}", e.getMessage());
            send(session, "ERROR", interviewId, Map.of("code", 500, "message", e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long interviewId = (Long) session.getAttributes().get("sessionId");
        if (interviewId != null) {
            Map<String, WebSocketSession> room = rooms.get(interviewId);
            if (room != null) {
                room.remove(session.getId());
                if (room.isEmpty()) {
                    rooms.remove(interviewId);
                }
            }
        }
    }

    private void broadcast(Long interviewId, String type, Object payload) throws IOException {
        Map<String, WebSocketSession> room = rooms.get(interviewId);
        if (room == null) {
            return;
        }
        for (WebSocketSession s : room.values()) {
            if (s.isOpen()) {
                send(s, type, interviewId, payload);
            }
        }
    }

    private void send(WebSocketSession session, String type, Long interviewId, Object payload) throws IOException {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", type);
        node.put("sessionId", interviewId == null ? 0L : interviewId);
        node.set("payload", objectMapper.valueToTree(payload));
        synchronized (session) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(node)));
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    static Long parseLongQuery(URI uri, String key) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String part : uri.getQuery().split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) {
                try {
                    return Long.parseLong(kv[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    static String parseToken(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String part : uri.getQuery().split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    static Long userIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String raw = token.startsWith("Bearer ") ? token : "Bearer " + token;
        return JwtTokenUtil.getIdFromOriginalToken(raw);
    }
}
