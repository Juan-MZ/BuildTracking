package com.construmedicis.buildtracking.item.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemDTO {
    private Long id;
    private String name;
    private String description;
    private Integer quantity;
    private List<Long> projectIds;
}
