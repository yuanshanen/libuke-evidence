package com.libuke.evidence.security;

import java.util.List;

public record AdminPrincipal(
    Long id,
    String username,
    String displayName,
    boolean superAdmin,
    List<Long> communityIds,
    List<String> roleCodes,
    List<String> permissionCodes
) {
}
