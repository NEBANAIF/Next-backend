package com.ousman.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Central audit hook; persists to logs today and can be extended to write to an audit table.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public void record(String action, String entityType, String entityId, String detail) {
        log.info("[AUDIT] action={} entity={} id={} detail={}", action, entityType, entityId, detail);
    }
}
