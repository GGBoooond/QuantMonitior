package com.example.futuresmonitor.service;

import com.example.futuresmonitor.dto.WebSocketRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AlertWebSocketHandler.class);
    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private FuturesMonitorService monitorService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        logger.info("New WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            WebSocketRequest request = objectMapper.readValue(payload, WebSocketRequest.class);

            if ("subscribe".equalsIgnoreCase(request.getType()) && request.getSymbol() != null) {
                monitorService.switchMonitoringSymbol(request.getSymbol());
            }
        } catch (Exception e) {
            logger.error("Error processing message from client {}", session.getId(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        logger.info("WebSocket connection closed: {}, status: {}", session.getId(), status);
    }

    /**
     * 向所有连接的客户端广播消息 (使用Object以支持多种消息类型)
     * @param messageObject 消息对象 (可以是 AlertMessage, LatestPriceMessage 等)
     */
    public void broadcast(Object messageObject) {
        try {
            String message = objectMapper.writeValueAsString(messageObject);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        logger.error("Failed to send message to session {}", session.getId(), e);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error during broadcast", e);
        }
    }
}