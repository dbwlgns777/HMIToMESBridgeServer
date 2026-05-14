package com.zes.hmitomesbridgeserver.controller;

import com.alibaba.fastjson.JSONObject;
import com.zes.hmitomesbridgeserver.common.ZES_Enum;
import com.zes.hmitomesbridgeserver.common.ZES_returnService;
import com.zes.hmitomesbridgeserver.opcua.ZES_opcUaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/opcua")
@Validated
public class ZES_opcUaController
{
    private final ZES_opcUaService ZES_gv_opcUaService;
    private final ZES_returnService ZES_gv_returnService;

    public ZES_opcUaController(ZES_opcUaService ZES_gv_opcUaService, ZES_returnService ZES_gv_returnService)
    {
        this.ZES_gv_opcUaService = ZES_gv_opcUaService;
        this.ZES_gv_returnService = ZES_gv_returnService;
    }

    @Operation(summary = "OPC-UA 노드 값 조회", description = "엔드포인트와 NodeId로 OPC-UA 노드 값을 조회")
    @GetMapping("/read")
    public JSONObject ZES_readNode(@RequestParam @NotBlank String endpointUrl,
                                   @RequestParam @NotBlank String nodeId)
    {
        try
        {
            String ZES_lv_value = ZES_gv_opcUaService.ZES_readNodeValue(endpointUrl, nodeId);
            return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_SUCCESS, "success", ZES_lv_value);
        }
        catch (Exception e)
        {
            return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_SERVER_ERROR, e.getMessage(), null);
        }
    }
}
