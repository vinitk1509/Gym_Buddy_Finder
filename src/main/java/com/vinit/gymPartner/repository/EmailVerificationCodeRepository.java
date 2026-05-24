package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);
}
