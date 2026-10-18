package org.aburavov.urlshortener.stats;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.aburavov.urlshortener.redirect.LinkClickedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ClickRecorder {

    private static final int REFERER_MAX_LENGTH = 2048;
    private static final int USER_AGENT_MAX_LENGTH = 512;

    private final ClickRepository clickRepository;
    private final ClickProperties properties;

    public ClickRecorder(ClickRepository clickRepository, ClickProperties properties) {
        this.clickRepository = clickRepository;
        this.properties = properties;
    }

    @Async
    @EventListener
    public void onLinkClicked(LinkClickedEvent event) {
        Click click = new Click(
                event.linkId(),
                event.clickedAt(),
                truncate(event.referer(), REFERER_MAX_LENGTH),
                truncate(event.userAgent(), USER_AGENT_MAX_LENGTH),
                hashIp(event.clientIp()));
        clickRepository.save(click);
    }

    private String hashIp(String ip) {
        if (ip == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((properties.ipSalt() + ip).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
