package org.example.civilbridge.domain.message.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageBatchWriter {

    public static final String QUEUE_KEY = "message:queue";
    public static final int BATCH_SIZE = 500;
    private static final long SCHEDULE_RATE_MS = 1000L;

    private final StringRedisTemplate stringRedisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRate = SCHEDULE_RATE_MS)
    public void scheduledFlush() {
        flush();
    }

    public synchronized void flush() {
        List<String> items = stringRedisTemplate.opsForList().rightPop(QUEUE_KEY, BATCH_SIZE);
        if (items == null || items.isEmpty()) return;

        List<Object[]> batchArgs = items.stream()
                .map(this::parseItem)
                .filter(Objects::nonNull)
                .toList();

        if (batchArgs.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                "INSERT INTO messages (user_id, room_id, content, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                batchArgs
        );
        log.debug("Batch inserted {} messages", batchArgs.size());
    }

    private Object[] parseItem(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return new Object[]{
                    node.get("userId").asLong(),
                    node.get("roomId").asLong(),
                    node.get("content").asText()
            };
        } catch (Exception e) {
            log.error("Failed to parse queued message: {}", json, e);
            return null;
        }
    }
}
