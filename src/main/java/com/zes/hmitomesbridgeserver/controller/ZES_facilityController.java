package com.zes.hmitomesbridgeserver.controller;

import com.alibaba.fastjson.JSONObject;
import com.zes.hmitomesbridgeserver.common.ZES_Enum;
import com.zes.hmitomesbridgeserver.common.ZES_returnService;
import com.zes.hmitomesbridgeserver.dto.ZES_facilityIctRequest;
import com.zes.hmitomesbridgeserver.service.ZES_facilityService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ZES_facilityController
{
    private final ZES_returnService ZES_gv_returnService;
    private final ZES_facilityService ZES_gv_facilityService;

    public ZES_facilityController(ZES_returnService ZES_gv_returnService, ZES_facilityService ZES_gv_facilityService)
    {
        this.ZES_gv_returnService = ZES_gv_returnService;
        this.ZES_gv_facilityService = ZES_gv_facilityService;
    }

    @Operation(summary = "설비 상세정보 함수", description = "설비 코드로 설비정보를 조회하는 함수")
    @PostMapping("/facility/detail")
    public JSONObject ZES_facilityDetail(@RequestBody @Valid ZES_facilityIctRequest request, BindingResult bindingResult)
    {
        if(bindingResult.hasErrors())
        {
            return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_VALID_ERROR,
                    bindingResult.getAllErrors().get(0).getDefaultMessage(),"null");
        }
        return ZES_gv_facilityService.ZES_facilityDetail(request);
    }
}
