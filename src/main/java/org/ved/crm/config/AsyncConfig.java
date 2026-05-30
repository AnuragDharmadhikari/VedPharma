package org.ved.crm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// ── AsyncConfig ───────────────────────────────────────────────
// Enables Spring's @Async support
// Without this, @Async methods run synchronously — defeating the purpose
// With this, @Async methods run in a separate thread pool
// Email sending never blocks the main invoice generation thread

@Configuration
@EnableAsync
public class AsyncConfig {
}