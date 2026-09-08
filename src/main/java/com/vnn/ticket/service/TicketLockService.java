package com.vnn.ticket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketLockService {

    private final RedissonClient redissonClient;

    @Value("${ticket.lock.wait-time-seconds:3}")
    private long waitTimeSeconds;

    @Value("${ticket.lock.lease-time-seconds:5}")
    private long leaseTimeSeconds;

    // Bộ khóa cục bộ (Local ReentrantLock) đóng vai trò dự phòng (Fallback) nếu Redis tạm thời ngắt kết nối
    private final ConcurrentHashMap<Long, ReentrantLock> localLockMap = new ConcurrentHashMap<>();

    /**
     * Thực thi hành động trong khối Khóa phân tán Redisson (Distributed Lock).
     * Đảm bảo tính nhất quán dữ liệu khi có 50.000 requests cùng tranh mua vé tại 1 thời điểm.
     */
    public <T> T executeWithLock(Long ticketTypeId, Supplier<T> action) {
        String lockKey = "lock:ticket:type:" + ticketTypeId;
        RLock lock = null;
        boolean isLocked = false;

        try {
            lock = redissonClient.getLock(lockKey);
            // Cố gắng lấy khóa phân tán trong tối đa waitTimeSeconds, giữ khóa tối đa leaseTimeSeconds
            isLocked = lock.tryLock(waitTimeSeconds, leaseTimeSeconds, TimeUnit.SECONDS);

            if (isLocked) {
                log.info("🔒 [REDIS LOCK THÀNH CÔNG] Đã chiếm khóa phân tán '{}' cho luồng {}", lockKey, Thread.currentThread().getName());
                return action.get();
            } else {
                log.warn("⏳ [REDIS LOCK TIMEOUT] Luồng {} không thể lấy khóa '{}' sau {} giây", Thread.currentThread().getName(), lockKey, waitTimeSeconds);
                throw new RuntimeException("Hệ thống đang quá tải, lượng người săn vé quá đông. Vui lòng thử lại sau giây lát!");
            }
        } catch (Exception ex) {
            log.warn("⚠️ [REDIS FALLBACK] Gặp sự cố kết nối Redis ('{}'), chuyển sang cơ chế Khóa cục bộ ReentrantLock để hệ thống tiếp tục vận hành!", ex.getMessage());
            return executeWithLocalLock(ticketTypeId, action);
        } finally {
            if (lock != null && isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("🔓 [REDIS UNLOCK] Đã nhả khóa phân tán '{}'", lockKey);
            }
        }
    }

    private <T> T executeWithLocalLock(Long ticketTypeId, Supplier<T> action) {
        ReentrantLock localLock = localLockMap.computeIfAbsent(ticketTypeId, k -> new ReentrantLock());
        try {
            boolean acquired = localLock.tryLock(waitTimeSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RuntimeException("Lượng người đặt vé quá đông, vui lòng thử lại sau giây lát!");
            }
            try {
                return action.get();
            } finally {
                localLock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Quá trình xử lý đặt vé bị gián đoạn");
        }
    }
}
