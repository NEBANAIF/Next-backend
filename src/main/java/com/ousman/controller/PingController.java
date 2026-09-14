package com.ousman.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight keep-alive endpoint for uptime pingers (e.g. cron-job.org).
 *
 * Render's free tier spins the service down after inactivity, so an
 * external cron job pings this URL periodically to keep it warm.
 *
 * This is intentionally NOT /actuator/health — that endpoint (with
 * show-details enabled) can return a large payload describing DB,
 * disk space, etc., which exceeds cron-job.org's response size limit
 * and causes the job to fail with "output too large". This endpoint
 * returns a couple of bytes, nothing more.
 *
 * Requires a shared secret (?key=...) instead of JWT auth, since
 * cron-job.org can't hold a login session. This keeps the endpoint
 * from being freely hit by anyone who stumbles on the URL, without
 * adding real risk if guessed — the response reveals nothing beyond
 * "server is up". Configure PING_SECRET in Render's env vars and use
 * the same value in the cron-job.org job URL.
 */
@RestController
public class PingController {

    @Value("${ping.secret}")
    private String pingSecret;

    @GetMapping("/api/ping")
    public ResponseEntity<String> ping(@RequestParam(name = "key", required = false) String key) {
        if (key == null || !key.equals(pingSecret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }
        return ResponseEntity.ok("OK");
    }
}
