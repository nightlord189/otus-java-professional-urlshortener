package org.aburavov.urlshortener.admin;

import org.aburavov.urlshortener.common.PageResponse;
import org.aburavov.urlshortener.user.UserService;
import org.aburavov.urlshortener.user.dto.UserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(userService.listAll(page, size).map(UserResponse::from));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable long id) {
        return UserResponse.from(userService.getById(id));
    }

    @PostMapping("/{id}/block")
    public UserResponse block(@PathVariable long id) {
        return UserResponse.from(userService.block(id));
    }

    @PostMapping("/{id}/unblock")
    public UserResponse unblock(@PathVariable long id) {
        return UserResponse.from(userService.unblock(id));
    }
}
