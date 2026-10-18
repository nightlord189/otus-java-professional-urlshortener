package org.aburavov.urlshortener.link;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.aburavov.urlshortener.common.BadRequestException;
import org.aburavov.urlshortener.common.ConflictException;
import org.aburavov.urlshortener.common.NotFoundException;
import org.aburavov.urlshortener.config.CacheConfig;
import org.aburavov.urlshortener.link.dto.CreateLinkRequest;
import org.aburavov.urlshortener.link.dto.UpdateLinkRequest;
import org.aburavov.urlshortener.user.User;
import org.aburavov.urlshortener.user.UserService;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkService {

    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{4,32}$");
    private static final Set<String> RESERVED_ALIASES = Set.of(
            "api", "actuator", "admin", "swagger-ui", "v3", "login", "register", "health", "static", "error");
    private static final int URL_MAX_LENGTH = 2048;
    private static final int CODE_GENERATION_ATTEMPTS = 3;
    private static final int PAGE_SIZE_MAX = 100;

    private final LinkRepository linkRepository;
    private final UserService userService;
    private final ShortCodeGenerator shortCodeGenerator;
    private final CacheManager cacheManager;
    private final URI baseUri;

    public LinkService(LinkRepository linkRepository, UserService userService, ShortCodeGenerator shortCodeGenerator,
                       CacheManager cacheManager, LinkProperties properties) {
        this.linkRepository = linkRepository;
        this.userService = userService;
        this.shortCodeGenerator = shortCodeGenerator;
        this.cacheManager = cacheManager;
        this.baseUri = URI.create(properties.baseUrl());
    }

    @Transactional
    public Link create(long ownerId, CreateLinkRequest request) {
        String originalUrl = validateUrl(request.originalUrl());
        validateExpiresAt(request.expiresAt());
        String code = request.alias() == null || request.alias().isBlank()
                ? generateFreeCode()
                : validateAlias(request.alias().trim());
        User owner = userService.getById(ownerId);
        return linkRepository.save(new Link(code, originalUrl, owner, request.expiresAt()));
    }

    @Transactional(readOnly = true)
    public Page<Link> listByOwner(long ownerId, LinkStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > PAGE_SIZE_MAX) {
            throw new BadRequestException("Page must be >= 0 and size between 1 and " + PAGE_SIZE_MAX);
        }
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status == null) {
            return linkRepository.findByOwnerIdAndStatusNot(ownerId, LinkStatus.DELETED, pageRequest);
        }
        return linkRepository.findByOwnerIdAndStatus(ownerId, status, pageRequest);
    }

    @Transactional(readOnly = true)
    public Link getOwned(long ownerId, long linkId) {
        return linkRepository.findById(linkId)
                .filter(link -> link.getOwner().getId() == ownerId && link.getStatus() != LinkStatus.DELETED)
                .orElseThrow(() -> new NotFoundException("Link not found"));
    }

    @Transactional
    public Link update(long ownerId, long linkId, UpdateLinkRequest request) {
        Link link = getOwned(ownerId, linkId);
        if (request.status() != null) {
            if (request.status() != LinkStatus.ACTIVE && request.status() != LinkStatus.DISABLED) {
                throw new BadRequestException("Status must be ACTIVE or DISABLED");
            }
            if (link.getStatus() == LinkStatus.BLOCKED) {
                throw new ConflictException("Link is blocked by administrator");
            }
            link.setStatus(request.status());
        }
        if (request.expiresAt() != null) {
            validateExpiresAt(request.expiresAt());
            link.setExpiresAt(request.expiresAt());
        }
        evictFromCache(link);
        return link;
    }

    @Transactional
    public Link clearExpiry(long ownerId, long linkId) {
        Link link = getOwned(ownerId, linkId);
        link.setExpiresAt(null);
        evictFromCache(link);
        return link;
    }

    @Transactional
    public void delete(long ownerId, long linkId) {
        Link link = getOwned(ownerId, linkId);
        link.setStatus(LinkStatus.DELETED);
        evictFromCache(link);
    }

    private String generateFreeCode() {
        for (int attempt = 0; attempt < CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = shortCodeGenerator.generate();
            if (!linkRepository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique short code");
    }

    private String validateAlias(String alias) {
        if (!ALIAS_PATTERN.matcher(alias).matches()) {
            throw new BadRequestException("Alias must be 4-32 characters: letters, digits, '-' or '_'");
        }
        if (RESERVED_ALIASES.contains(alias.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Alias is reserved");
        }
        if (linkRepository.existsByShortCode(alias)) {
            throw new ConflictException("Alias is already taken");
        }
        return alias;
    }

    private String validateUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new BadRequestException("URL is required");
        }
        String url = originalUrl.trim();
        if (url.length() > URL_MAX_LENGTH) {
            throw new BadRequestException("URL is too long");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new BadRequestException("URL is not valid");
        }
        String scheme = uri.getScheme();
        if (uri.getHost() == null || scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new BadRequestException("URL must be absolute and start with http:// or https://");
        }
        if (uri.getHost().equalsIgnoreCase(baseUri.getHost())) {
            throw new BadRequestException("URL must not point to this service");
        }
        return url;
    }

    private static void validateExpiresAt(Instant expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new BadRequestException("Expiry must be in the future");
        }
    }

    private void evictFromCache(Link link) {
        cacheManager.getCache(CacheConfig.LINKS_CACHE).evict(link.getShortCode());
    }
}
