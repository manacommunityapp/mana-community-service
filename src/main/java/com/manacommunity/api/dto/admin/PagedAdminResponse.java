package com.manacommunity.api.dto.admin;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Builder
public class PagedAdminResponse<T> {
    private List<T>  content;
    private int      page;
    private int      size;
    private long     totalElements;
    private int      totalPages;

    public static <S, T> PagedAdminResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return PagedAdminResponse.<T>builder()
                .content(page.getContent().stream().map(mapper).collect(Collectors.toList()))
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
