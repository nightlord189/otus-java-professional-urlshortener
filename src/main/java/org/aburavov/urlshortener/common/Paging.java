package org.aburavov.urlshortener.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class Paging {

    private static final int MAX_SIZE = 100;

    private Paging() {
    }

    public static PageRequest of(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new BadRequestException("Page must be >= 0 and size between 1 and " + MAX_SIZE);
        }
        return PageRequest.of(page, size, sort);
    }
}
