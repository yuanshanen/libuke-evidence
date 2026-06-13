package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdminMenuResponse {

    private Long id;
    private Long parentId;
    private String type;
    private String name;
    private String title;
    private String path;
    private String activePath;
    private String component;
    private String icon;
    private String activeIcon;
    private String permissionCode;
    private String authCode;
    private Integer sortOrder;
    private Boolean enabled;
    private Integer status;
    private Boolean hidden;
    private Boolean affixTab;
    private Boolean keepAlive;
    private Boolean hideChildrenInMenu;
    private Boolean hideInBreadcrumb;
    private Boolean hideInTab;
    private String badgeType;
    private String badge;
    private String badgeVariants;
    private String linkSrc;
    private Map<String, Object> meta;
    private List<AdminMenuResponse> children;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
