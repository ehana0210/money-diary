package com.moneydiary.api.security;

/**
 * Authenticated principal extracted from a verified Firebase ID token.
 */
public record FirebaseUser(String uid, String email) {
}
