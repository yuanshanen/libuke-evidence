package com.libuke.evidence.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("menus")
public class Menu {

    @TableId(type = IdType.ASSIGN_ID)
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
    private Integer sortOrder;
    private Boolean enabled;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
