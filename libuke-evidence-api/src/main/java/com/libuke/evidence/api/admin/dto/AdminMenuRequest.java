package com.libuke.evidence.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminMenuRequest {

    private Long parentId;

    @NotBlank
    @Size(max = 32)
    private String type;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String title;

    @Size(max = 200)
    private String path;

    @Size(max = 200)
    private String activePath;

    @Size(max = 200)
    private String component;

    @Size(max = 100)
    private String icon;

    @Size(max = 100)
    private String activeIcon;

    @Size(max = 100)
    private String permissionCode;

    @Size(max = 100)
    private String authCode;

    @Size(max = 500)
    private String linkSrc;

    @Size(max = 20)
    private String badgeType;

    @Size(max = 50)
    private String badge;

    @Size(max = 30)
    private String badgeVariants;

    private Integer sortOrder;
    private Boolean enabled;
    private Boolean hidden;
    private Boolean affixTab;
    private Boolean keepAlive;
    private Boolean hideChildrenInMenu;
    private Boolean hideInBreadcrumb;
    private Boolean hideInTab;
}
