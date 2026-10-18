package org.aburavov.urlshortener.stats;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clicks")
public record ClickProperties(String ipSalt) {
}
