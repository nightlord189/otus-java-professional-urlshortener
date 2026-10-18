package org.aburavov.urlshortener.stats;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "clicks")
public class Click {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_id", nullable = false)
    private Long linkId;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    private String referer;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_hash")
    private String ipHash;

    protected Click() {
    }

    public Click(Long linkId, Instant clickedAt, String referer, String userAgent, String ipHash) {
        this.linkId = linkId;
        this.clickedAt = clickedAt;
        this.referer = referer;
        this.userAgent = userAgent;
        this.ipHash = ipHash;
    }

    public Long getId() {
        return id;
    }

    public Long getLinkId() {
        return linkId;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }

    public String getReferer() {
        return referer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIpHash() {
        return ipHash;
    }
}
