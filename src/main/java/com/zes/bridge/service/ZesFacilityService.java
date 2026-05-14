package com.zes.bridge.service;

import com.alibaba.fastjson.JSONObject;
import com.zes.bridge.common.ZesEnum;
import com.zes.bridge.common.ZesReturnService;
import com.zes.bridge.dto.ZesFacilityIctRequest;
import com.zes.bridge.mapper.ZesFacilityMapper;
import com.zes.bridge.model.ZesFacilityInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZesFacilityService {

    private final ZesFacilityMapper zesFacilityMapper;
    private final ZesReturnService zesReturnService;

    public ZesFacilityService(ZesFacilityMapper zesFacilityMapper, ZesReturnService zesReturnService) {
        this.zesFacilityMapper = zesFacilityMapper;
        this.zesReturnService = zesReturnService;
    }

    public JSONObject ZES_facilityDetail(ZesFacilityIctRequest request) {
        List<ZesFacilityInfo> result = zesFacilityMapper.selectActiveFacilitiesByCompanyCode(request.getCompanyCode());

        if (result.isEmpty()) {
            return zesReturnService.ZES_returnToFormat(ZesEnum.ZES_NOT_FOUND,
                    "No active facility found", result);
        }

        return zesReturnService.ZES_returnToFormat(ZesEnum.ZES_SUCCESS,
                "Facility information fetched successfully", result);
    }
}
