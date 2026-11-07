package org.aburavov.urlshortener.controller;

import org.aburavov.urlshortener.dto.GlobalStatsResponse;
import org.aburavov.urlshortener.dto.LinkResponse;
import org.aburavov.urlshortener.dto.UserResponse;
import org.aburavov.urlshortener.model.LinkStatus;
import org.aburavov.urlshortener.service.LinkService;
import org.aburavov.urlshortener.service.StatsService;
import org.aburavov.urlshortener.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final LinkService linkService;
    private final UserService userService;
    private final StatsService statsService;

    @Value("${app.base-url}")
    private String baseUrl;

    public AdminController(LinkService linkService, UserService userService, StatsService statsService) {
        this.linkService = linkService;
        this.userService = userService;
        this.statsService = statsService;
    }

    @GetMapping("/links")
    public Page<LinkResponse> getLinks(
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) LinkStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return linkService.findAll(ownerId, status, pageable).map(link -> LinkResponse.from(link, baseUrl));
    }

    @GetMapping("/links/{id}")
    public LinkResponse getLink(@PathVariable long id) {
        return LinkResponse.from(linkService.getById(id), baseUrl);
    }

    @PostMapping("/links/{id}/block")
    public LinkResponse blockLink(@PathVariable long id) {
        return LinkResponse.from(linkService.block(id), baseUrl);
    }

    @PostMapping("/links/{id}/unblock")
    public LinkResponse unblockLink(@PathVariable long id) {
        return LinkResponse.from(linkService.unblock(id), baseUrl);
    }

    @GetMapping("/users")
    public Page<UserResponse> getUsers(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return userService.findAll(pageable).map(UserResponse::from);
    }

    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable long id) {
        return UserResponse.from(userService.getById(id));
    }

    @PostMapping("/users/{id}/block")
    public UserResponse blockUser(@PathVariable long id) {
        return UserResponse.from(userService.block(id));
    }

    @PostMapping("/users/{id}/unblock")
    public UserResponse unblockUser(@PathVariable long id) {
        return UserResponse.from(userService.unblock(id));
    }

    @GetMapping("/stats")
    public GlobalStatsResponse getStats() {
        return statsService.globalStats();
    }
}
