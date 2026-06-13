package com.libuke.evidence.api.miniapp;

import com.libuke.evidence.api.dto.BindCommunityRequest;
import com.libuke.evidence.api.dto.CommunityBindingResponse;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.domain.service.CommunityBindingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/miniapp/v1/communities")
public class MiniCommunityController {

    private final CommunityBindingService communityBindingService;

    @PostMapping("/bind")
    public ApiResponse<CommunityBindingResponse> bindCommunity(@Valid @RequestBody BindCommunityRequest request) {
        return ApiResponse.ok(communityBindingService.bindByInvitationCode(request));
    }
}
