package com.vinit.gymPartner.entity.enums;

public enum SessionState {
    PENDING_APPROVAL,  // Waiting for partner to accept the session request
    SCHEDULED,         // Both accepted, session is confirmed
    DECLINED,          // Partner declined the session request
    COMPLETED,
    CANCELLED,
    NO_SHOW
}
