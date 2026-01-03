package org.example.gyeonggi_partners.domain.proposal.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalLockService {

    private static final String LOCK_PREFIX = "proposal:lock:";
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(10);    // 10분 락

    private final StringRedisTemplate redisTemplate;

    public boolean tryLock(Long proposalId, Long userId) {
        String key = LOCK_PREFIX + proposalId;
        String value = String.valueOf(userId);

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, value, LOCK_TIMEOUT);

        if(Boolean.TRUE.equals(success)) {
            log.info("제안서 {} 락 획득 성공 - 사용자: {}", proposalId, userId);
            return true;
        }

        String currentLockOwner = redisTemplate.opsForValue().get(key);
        if(value.equals(currentLockOwner)) {
            redisTemplate.expire(key, LOCK_TIMEOUT);
            log.info("제안서 {} 락 TTL 갱신 - 사용자: {}", proposalId, userId);
            return true;
        }

        log.warn("제안서 {} 락 획득 실패 - 사용자: {}, 현재 소유자: {}", proposalId, userId, currentLockOwner);
        return false;
    }

    public void unlock(Long proposalId, Long userId) {
        String key = LOCK_PREFIX + proposalId;
        String currentLockOwner = redisTemplate.opsForValue().get(key);

        if (String.valueOf(userId).equals(currentLockOwner)) {
            redisTemplate.delete(key);
            log.info("제안서 {} 락 해제 - 사용자: {}", proposalId, userId);
        }
    }

    public Long getLockOwner(Long proposalId) {
        String key = LOCK_PREFIX + proposalId;
        String owner = redisTemplate.opsForValue().get(key);

        return owner != null ? Long.parseLong(owner) : null;
    }


    public void renewLock(Long proposalId, Long userId) {
        String key = LOCK_PREFIX + proposalId;
        String currentLockOwner = redisTemplate.opsForValue().get(key);

        if(String.valueOf(userId).equals(currentLockOwner)) {
            redisTemplate.expire(key, LOCK_TIMEOUT);
            log.info("제안서 {} 락 TTL 갱신 - 사용자: {}", proposalId, userId);
        }
    }
}
