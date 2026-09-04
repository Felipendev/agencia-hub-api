package com.agenciahub.api.application.persistence.repository;
import com.agenciahub.api.application.persistence.entity.TripSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface TripSegmentRepository extends JpaRepository<TripSegment, UUID> {
    List<TripSegment> findByTrip_IdOrderBySegmentNumberAsc(UUID tripId);
}
