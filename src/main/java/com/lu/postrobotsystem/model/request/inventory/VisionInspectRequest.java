package com.lu.postrobotsystem.model.request.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 视觉巡检结果回写请求 DTO。
 * <p>
 * 用于接收机器人视觉巡检系统回传的库存校验结果，
 * 包括样品状态、账实一致性标记等信息。
 * </p>
 */
@Data
@Schema(description = "视觉巡检结果回写")
public class VisionInspectRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "样品状态不能为空")
    @Schema(description = "样品状态（NORMAL-正常 MISSING-缺失 DISPLACED-错位）")
    private String sampleStatus;

    @Schema(description = "账实不一致标记（true-异常 false-一致）")
    private Boolean mismatchFlag;
}
