package com.lu.postrobotsystem.controller;

import com.lu.postrobotsystem.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器。
 * <p>
 * 提供系统健康检查端点，用于监控服务是否正常运行。
 * 通常被负载均衡器、容器编排平台（如 Kubernetes）或外部监控系统定期调用，
 * 以判断服务实例的存活状态。该接口不依赖数据库或外部中间件，仅返回成功响应。
 * </p>
 */
@RestController
@RequestMapping("/health")
@Tag(name = "健康检查", description = "用于检查服务是否正常运行")
public class HealthController {

    /**
     * 健康检查接口。
     * <p>
     * 最简单的存活（Liveness）探测端点，直接返回成功结果。
     * 不执行任何业务逻辑，确保监控探测的低延迟和高可用。
     * 调用方只需根据 HTTP 状态码或响应结构判断服务是否存活。
     * </p>
     *
     * @return 统一成功响应，不包含具体数据
     */
    @GetMapping("/")
    @Operation(summary = "健康检查接口")
    public Result<String> healthCheck() {
        // 直接返回空数据的成功结果，表示服务正常运行
        return Result.success();
    }
}
