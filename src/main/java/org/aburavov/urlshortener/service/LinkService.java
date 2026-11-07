package org.aburavov.urlshortener.service;

import org.aburavov.urlshortener.dto.CreateLinkRequest;
import org.aburavov.urlshortener.dto.ResolvedLink;
import org.aburavov.urlshortener.dto.UpdateLinkRequest;
import org.aburavov.urlshortener.model.Link;
import org.aburavov.urlshortener.model.LinkStatus;
import org.aburavov.urlshortener.repository.LinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

@Service
public class LinkService {
    private static final Logger log = LoggerFactory.getLogger(LinkService.class);

    private final LinkRepository linkRepository;
    private final UserService userService;
    private final ShortCodeGenerator codeGenerator;
    private final CacheManager cacheManager;

    public LinkService(
            LinkRepository linkRepository,
            UserService userService,
            ShortCodeGenerator codeGenerator,
            CacheManager cacheManager) {
        this.linkRepository = linkRepository;
        this.userService = userService;
        this.codeGenerator = codeGenerator;
        this.cacheManager = cacheManager;
    }

    public Link create(long ownerId, CreateLinkRequest request) {
        var url = checkUrl(request.originalUrl());
        checkExpiresAt(request.expiresAt());
        var code = request.alias();
        if (code == null || code.isBlank()) {
            code = codeGenerator.generate();
            while (linkRepository.existsByShortCode(code)) {
                code = codeGenerator.generate();
            }
        } else {
            checkAlias(code);
        }
        var owner = userService.getById(ownerId);
        var link = linkRepository.save(new Link(code, url, owner, request.expiresAt()));
        log.info("created link {} -> {}", code, url);
        return link;
    }

    @Cacheable("links")
    public ResolvedLink resolve(String code) {
        var link = linkRepository.findByShortCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));
        if (link.getStatus() == LinkStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found");
        }
        return new ResolvedLink(link.getId(), link.getOriginalUrl(), link.getStatus(), link.getExpiresAt());
    }

    public Page<Link> findByOwner(long ownerId, LinkStatus status, Pageable pageable) {
        if (status == null) {
            return linkRepository.findByOwnerIdAndStatusNot(ownerId, LinkStatus.DELETED, pageable);
        }
        return linkRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
    }

    public Page<Link> findAll(Long ownerId, LinkStatus status, Pageable pageable) {
        if (ownerId != null && status != null) {
            return linkRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
        }
        if (ownerId != null) {
            return linkRepository.findByOwnerId(ownerId, pageable);
        }
        if (status != null) {
            return linkRepository.findByStatus(status, pageable);
        }
        return linkRepository.findAll(pageable);
    }

    public Link getById(long id) {
        return linkRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));
    }

    // user sees only his own links, deleted ones look like they never existed
    public Link getOwned(long ownerId, long id) {
        var link = getById(id);
        if (link.getOwner().getId() != ownerId || link.getStatus() == LinkStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found");
        }
        return link;
    }

    @Transactional
    public Link update(long ownerId, long id, UpdateLinkRequest request) {
        var link = getOwned(ownerId, id);
        if (request.status() != null) {
            if (request.status() != LinkStatus.ACTIVE && request.status() != LinkStatus.DISABLED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be ACTIVE or DISABLED");
            }
            if (link.getStatus() == LinkStatus.BLOCKED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Link is blocked by admin");
            }
            link.setStatus(request.status());
        }
        if (request.expiresAt() != null) {
            checkExpiresAt(request.expiresAt());
            link.setExpiresAt(request.expiresAt());
        }
        evict(link);
        return link;
    }

    @Transactional
    public Link clearExpiry(long ownerId, long id) {
        var link = getOwned(ownerId, id);
        link.setExpiresAt(null);
        evict(link);
        return link;
    }

    @Transactional
    public void delete(long ownerId, long id) {
        var link = getOwned(ownerId, id);
        link.setStatus(LinkStatus.DELETED);
        evict(link);
        log.info("deleted link {}", link.getShortCode());
    }

    @Transactional
    public Link block(long id) {
        var link = getById(id);
        if (link.getStatus() == LinkStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Link is deleted");
        }
        link.setStatus(LinkStatus.BLOCKED);
        evict(link);
        return link;
    }

    @Transactional
    public Link unblock(long id) {
        var link = getById(id);
        if (link.getStatus() != LinkStatus.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Link is not blocked");
        }
        link.setStatus(LinkStatus.ACTIVE);
        evict(link);
        return link;
    }

    private void checkAlias(String alias) {
        if (!alias.matches("[A-Za-z0-9_-]{4,32}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alias must be 4-32 chars: letters, digits, - or _");
        }
        if (linkRepository.existsByShortCode(alias)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alias is already taken");
        }
    }

    private String checkUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL is required");
        }
        var url = originalUrl.trim();
        if (url.length() > 2048) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL is too long");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL is not valid");
        }
        if (uri.getHost() == null || !"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL must start with http:// or https://");
        }
        return url;
    }

    private void checkExpiresAt(Instant expiresAt) {
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiry must be in the future");
        }
    }

    private void evict(Link link) {
        cacheManager.getCache("links").evict(link.getShortCode());
    }
}
