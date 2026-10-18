package org.aburavov.urlshortener.admin;

import org.aburavov.urlshortener.common.PageResponse;
import org.aburavov.urlshortener.link.LinkMapper;
import org.aburavov.urlshortener.link.LinkService;
import org.aburavov.urlshortener.link.LinkStatus;
import org.aburavov.urlshortener.link.dto.LinkResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/links")
public class AdminLinkController {

    private final LinkService linkService;
    private final LinkMapper linkMapper;

    public AdminLinkController(LinkService linkService, LinkMapper linkMapper) {
        this.linkService = linkService;
        this.linkMapper = linkMapper;
    }

    @GetMapping
    public PageResponse<LinkResponse> list(@RequestParam(required = false) Long ownerId,
                                           @RequestParam(required = false) LinkStatus status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(linkService.listAll(ownerId, status, page, size).map(linkMapper::toResponse));
    }

    @GetMapping("/{id}")
    public LinkResponse get(@PathVariable long id) {
        return linkMapper.toResponse(linkService.getById(id));
    }

    @PostMapping("/{id}/block")
    public LinkResponse block(@PathVariable long id) {
        return linkMapper.toResponse(linkService.block(id));
    }

    @PostMapping("/{id}/unblock")
    public LinkResponse unblock(@PathVariable long id) {
        return linkMapper.toResponse(linkService.unblock(id));
    }
}
