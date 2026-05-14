package com.zes.hmitomesbridgeserver.controller;

import com.alibaba.fastjson.JSONObject;
import com.zes.hmitomesbridgeserver.service.ZES_workOrderKioskService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kiosk")
@Validated
public class ZES_workOrderKioskController
{
    private final ZES_workOrderKioskService ZES_gv_workOrderKioskService;

    public ZES_workOrderKioskController(ZES_workOrderKioskService ZES_gv_workOrderKioskService)
    {
        this.ZES_gv_workOrderKioskService = ZES_gv_workOrderKioskService;
    }

    @Operation(summary = "Kiosk 작업지시 목록", description = "processCode가 여러 개인 경우 콤마(,)로 구분하여 조회")
    @GetMapping("/active-work-orders")
    public JSONObject ZES_kioskActiveWorkOrderList(@RequestParam @Min(1) Integer page,
                                                   @RequestParam @Min(1) Integer size,
                                                   @RequestParam @NotBlank String processCode)
    {
        return ZES_gv_workOrderKioskService.ZES_kioskActiveWorkOrderList(page, size, processCode);
    }

    @Operation(summary = "ICT 번호로 모니터링 타입 조회", description = "facility 중복 시 duplication 반환")
    @GetMapping("/monitoring-type-codes")
    public JSONObject ZES_monitoringTypeCodes(@RequestParam @NotBlank String ictNumber)
    {
        return ZES_gv_workOrderKioskService.ZES_resolveMonitoringTypeCodesByIctNumber(ictNumber);
    }

    @Operation(summary = "ICT 번호로 작업지시 목록 조회", description = "ict_number -> facility -> monitoring process code -> 작업지시목록")
    @GetMapping("/active-work-orders/by-ict-number")
    public JSONObject ZES_kioskActiveWorkOrderListByIctNumber(@RequestParam @Min(1) Integer page,
                                                              @RequestParam @Min(1) Integer size,
                                                              @RequestParam @NotBlank String ictNumber)
    {
        return ZES_gv_workOrderKioskService.ZES_kioskActiveWorkOrderListByIctNumber(page, size, ictNumber);
    }

}
