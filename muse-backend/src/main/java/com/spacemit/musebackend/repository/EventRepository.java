package com.spacemit.musebackend.repository;

import com.spacemit.musebackend.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {
    Optional<Event> findByEventId(String eventId);
    List<Event> findByDeviceIdAndTimestampBetween(String deviceId, LocalDateTime start, LocalDateTime end);
    List<Event> findByDeviceIdOrderByTimestampDesc(String deviceId);
    
    List<Event> findByDeviceId(String deviceId);
    
    List<Event> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    Page<Event> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<Event> findByDeviceIdAndTimestampBetween(String deviceId, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<Event> findByDeviceIdAndEventTypeAndTimestampBetween(String deviceId, Event.EventType eventType, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<Event> findByDeviceIdAndEventTypeAndSeverityAndTimestampBetween(String deviceId, Event.EventType eventType, Event.Severity severity, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    // 新增的查询方法
    Page<Event> findByDeviceIdAndSeverityAndTimestampBetween(String deviceId, Event.Severity severity, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<Event> findByEventTypeAndSeverityAndTimestampBetween(Event.EventType eventType, Event.Severity severity, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<Event> findByEventTypeAndTimestampBetween(Event.EventType eventType, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<Event> findBySeverityAndTimestampBetween(Event.Severity severity, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    // 无时间限制的查询方法
    Page<Event> findByDeviceIdAndEventTypeAndSeverity(String deviceId, Event.EventType eventType, Event.Severity severity, Pageable pageable);
    
    Page<Event> findByDeviceIdAndEventType(String deviceId, Event.EventType eventType, Pageable pageable);
    
    Page<Event> findByDeviceIdAndSeverity(String deviceId, Event.Severity severity, Pageable pageable);
    
    Page<Event> findByEventTypeAndSeverity(Event.EventType eventType, Event.Severity severity, Pageable pageable);
    
    Page<Event> findByDeviceId(String deviceId, Pageable pageable);
    
    Page<Event> findByEventType(Event.EventType eventType, Pageable pageable);
    
    Page<Event> findBySeverity(Event.Severity severity, Pageable pageable);
    
    List<Event> findByDeviceIdAndEventTypeAndTimestampAfter(String deviceId, Event.EventType eventType, LocalDateTime timestamp);
    
    List<Event> findByDeviceIdAndTimestampAfter(String deviceId, LocalDateTime timestamp);
    
    List<Event> findByTimestampAfterAndSeverityIn(LocalDateTime timestamp, List<Event.Severity> severities);
    
    List<Event> findBySeverityIn(List<Event.Severity> severities);
}