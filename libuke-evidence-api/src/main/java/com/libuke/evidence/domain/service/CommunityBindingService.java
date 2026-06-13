package com.libuke.evidence.domain.service;

import com.libuke.evidence.api.dto.BindCommunityRequest;
import com.libuke.evidence.api.dto.CommunityBindingResponse;

public interface CommunityBindingService {

    CommunityBindingResponse bindByInvitationCode(BindCommunityRequest request);
}
