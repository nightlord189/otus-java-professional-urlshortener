package org.aburavov.urlshortener.redirect;

import org.aburavov.urlshortener.common.NotFoundException;
import org.aburavov.urlshortener.config.CacheConfig;
import org.aburavov.urlshortener.link.Link;
import org.aburavov.urlshortener.link.LinkRepository;
import org.aburavov.urlshortener.link.LinkStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class RedirectService {

    private final LinkRepository linkRepository;

    public RedirectService(LinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    @Cacheable(cacheNames = CacheConfig.LINKS_CACHE, key = "#code")
    public ResolvedLink resolve(String code) {
        Link link = linkRepository.findByShortCode(code)
                .filter(found -> found.getStatus() != LinkStatus.DELETED)
                .orElseThrow(() -> new NotFoundException("Link not found"));
        return new ResolvedLink(link.getId(), link.getOriginalUrl(), link.getStatus(), link.getExpiresAt());
    }
}
