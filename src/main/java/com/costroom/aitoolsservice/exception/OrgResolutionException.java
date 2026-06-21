package com.costroom.aitoolsservice.exception;

/**
 * Thrown when the caller's organization cannot be resolved from the DB
 * (e.g. identity not provisioned yet, or not linked to any org).
 */
public class OrgResolutionException extends RuntimeException {

    public OrgResolutionException(String message) {
        super(message);
    }
}