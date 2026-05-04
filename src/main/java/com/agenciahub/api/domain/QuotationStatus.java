package com.agenciahub.api.domain;

public enum QuotationStatus {
    /** Being prepared by the agency */
    DRAFT,
    /** Sent to the client */
    SENT,
    /** Waiting for client decision */
    AWAITING_CLIENT,
    /** Client accepted */
    ACCEPTED,
    /** Client declined */
    REJECTED,
    /** Past validity without acceptance */
    EXPIRED,
    /** Cancelled before resolution */
    CANCELLED
}
