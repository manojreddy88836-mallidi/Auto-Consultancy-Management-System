package com.autoconsultancy.security;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory JWT token blacklist used to implement logout.
 *
 * <p>Tokens are stored until their natural expiry, after which they are evicted.
 * On every validation the blacklist is checked before trusting the token.
 *
 * <p>For multi-node deployments this should be backed by Redis or MongoDB.
 */
@Service
public class TokenBlacklist {

    /** token → expiry timestamp (ms since epoch). */
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Adds {@code token} to the blacklist.
     *
     * @param token  the raw JWT string
     * @param expiry the token's natural expiry time; it will be evicted after this time
     */
    public void blacklist(String token, Date expiry) {
        blacklistedTokens.put(token, expiry.getTime());
        purgeExpired();
    }

    /** Returns {@code true} if the token has been explicitly revoked. */
    public boolean isBlacklisted(String token) {
        purgeExpired();
        return blacklistedTokens.containsKey(token);
    }

    /** Removes all tokens whose natural expiry has already passed. */
    private void purgeExpired() {
        long now = System.currentTimeMillis();
        blacklistedTokens.entrySet().removeIf(e -> e.getValue() < now);
    }
}
