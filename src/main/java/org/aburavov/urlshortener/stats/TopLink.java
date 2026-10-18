package org.aburavov.urlshortener.stats;

public interface TopLink {

    Long getLinkId();

    String getCode();

    long getClicks();
}
