package com.agenciahub.api.application.persistence.repository;
import com.agenciahub.api.application.persistence.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface TripRepository extends JpaRepository<Trip, UUID> {
    List<Trip> findByQuotation_Id(UUID quotationId);
    List<Trip> findBySale_Id(UUID saleId);
    List<Trip> findAllByAgency_IdOrderByTravelStartDateDesc(UUID agencyId);
    List<Trip> findAllByAgency_IdAndCustomer_IdOrderByTravelStartDateDesc(UUID agencyId, UUID customerId);
    Optional<Trip> findByIdAndAgency_Id(UUID id, UUID agencyId);
}
