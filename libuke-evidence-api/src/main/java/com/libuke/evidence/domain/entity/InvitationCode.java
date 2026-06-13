package com.libuke.evidence.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("invitation_codes")
public class InvitationCode {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long communityId;
    private String code;
    private Integer maxUsageCount;
    private Integer usedCount;
    private LocalDateTime expiresAt;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
