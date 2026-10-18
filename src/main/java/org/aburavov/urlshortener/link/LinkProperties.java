package org.aburavov.urlshortener.link;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.links")
public record LinkProperties(String baseUrl, int codeLength) {
}
