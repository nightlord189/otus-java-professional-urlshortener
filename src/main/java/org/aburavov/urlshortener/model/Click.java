package org.aburavov.urlshortener.model;

import jakarta.persistence.*;

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

    private String ip;

    public Click() {
    }

    public Click(Long linkId, Instant clickedAt, String referer, String userAgent, String ip) {
        this.linkId = linkId;
        this.clickedAt = clickedAt;
        this.referer = referer;
        this.userAgent = userAgent;
        this.ip = ip;
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

    public String getIp() {
        return ip;
    }
}
