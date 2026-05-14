package com.zes.bridge.controller;

import com.alibaba.fastjson.JSONObject;
import com.zes.bridge.common.ZesEnum;
import com.zes.bridge.common.ZesReturnService;
import com.zes.bridge.dto.ZesFacilityIctRequest;
import com.zes.bridge.service.ZesFacilityService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ZesFacilityController {

    private final ZesReturnService ZES_gv_returnService;
    private final ZesFacilityService ZES_gv_facilityService;

    public ZesFacilityController(ZesReturnService zesGvReturnService, ZesFacilityService zesGvFacilityService) {
        ZES_gv_returnService = zesGvReturnService;
        ZES_gv_facilityService = zesGvFacilityService;
    }

    @Operation(summary = "설비 상세정보 함수", description = "설비 코드로 설비정보를 조회하는 함수")
    @PostMapping("/facility/detail")
    public JSONObject ZES_facilityDetail(@RequestBody @Valid ZesFacilityIctRequest request,
                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ZES_gv_returnService.ZES_returnToFormat(ZesEnum.ZES_VALID_ERROR,
                    bindingResult.getAllErrors().get(0).getDefaultMessage(), "null");
        }
        return ZES_gv_facilityService.ZES_facilityDetail(request);
    }
}
