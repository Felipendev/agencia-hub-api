package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.domain.VerificationCodeType;
import com.agenciahub.api.application.persistence.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

    Optional<VerificationCode> findFirstByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(
            String email, VerificationCodeType type);

    long countByEmailAndCreatedAtAfter(String email, Instant after);

    @Modifying
    @Query("UPDATE VerificationCode v SET v.used = true WHERE v.email = :email AND v.type = :type AND v.used = false")
    void invalidateAllByEmailAndType(@Param("email") String email, @Param("type") VerificationCodeType type);
}
