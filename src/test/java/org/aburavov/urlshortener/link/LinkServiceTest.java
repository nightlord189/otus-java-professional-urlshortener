package org.aburavov.urlshortener.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.aburavov.urlshortener.common.BadRequestException;
import org.aburavov.urlshortener.common.ConflictException;
import org.aburavov.urlshortener.link.dto.CreateLinkRequest;
import org.aburavov.urlshortener.user.User;
import org.aburavov.urlshortener.user.UserRole;
import org.aburavov.urlshortener.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;
    @Mock
    private UserService userService;
    @Mock
    private ShortCodeGenerator shortCodeGenerator;
    @Mock
    private CacheManager cacheManager;

    private LinkService linkService;

    @BeforeEach
    void setUp() {
        linkService = new LinkService(linkRepository, userService, shortCodeGenerator, cacheManager,
                new LinkProperties("http://localhost:8080", 7));
    }

    @Test
    void rejectsUrlWithoutHttpScheme() {
        assertThatThrownBy(() -> linkService.create(1L, new CreateLinkRequest("ftp://example.com/file", null, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsRelativeUrl() {
        assertThatThrownBy(() -> linkService.create(1L, new CreateLinkRequest("example.com/page", null, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsUrlPointingToThisService() {
        assertThatThrownBy(() -> linkService.create(1L, new CreateLinkRequest("http://localhost:8080/abc1234", null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("this service");
    }

    @Test
    void rejectsReservedAlias() {
        assertThatThrownBy(() -> linkService.create(1L, new CreateLinkRequest("https://example.com", "admin", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("reserved");
    }

    @Test
    void rejectsAliasWithInvalidCharacters() {
        assertThatThrownBy(() -> linkService.create(1L, new CreateLinkRequest("https://example.com", "my link!", null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsTakenAlias() {
        when(linkRepository.existsByShortCode("my-link")).thenReturn(true);

        assertThatThrownBy(() -> linkService.create(1L, new CreateLinkRequest("https://example.com", "my-link", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void retriesWhenGeneratedCodeIsTaken() {
        when(shortCodeGenerator.generate()).thenReturn("AAAAAAA", "BBBBBBB");
        when(linkRepository.existsByShortCode("AAAAAAA")).thenReturn(true);
        when(linkRepository.existsByShortCode("BBBBBBB")).thenReturn(false);
        when(userService.getById(1L)).thenReturn(new User("owner@example.com", "hash", UserRole.USER));
        when(linkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Link link = linkService.create(1L, new CreateLinkRequest("https://example.com", null, null));

        assertThat(link.getShortCode()).isEqualTo("BBBBBBB");
        assertThat(link.getStatus()).isEqualTo(LinkStatus.ACTIVE);
    }
}
