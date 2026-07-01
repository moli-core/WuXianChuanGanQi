package com.wireless.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设备控制请求
 */
@Data
public class DeviceControlRequest {
    /** 设备编码 led/fan/buzzer */
    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;
    /** 操作 0-关 1-开 */
    @NotNull(message = "操作指令不能为空")
    private Integer action;
    /** 来源 manual/auto */
    private String source;
}
