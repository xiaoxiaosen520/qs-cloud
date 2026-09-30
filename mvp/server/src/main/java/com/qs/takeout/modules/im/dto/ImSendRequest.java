package com.qs.takeout.modules.im.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ImSendRequest {

    /** TEXT 时为正文；IMAGE 时为图片 URL（/uploads/...） */
    @NotBlank(message = "content 不能为空")
    @Size(max = 1000, message = "消息过长")
    private String content;

    /** TEXT / IMAGE，默认 TEXT */
    private String msgType;
}
