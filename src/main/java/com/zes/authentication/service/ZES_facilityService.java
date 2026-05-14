package com.zes.authentication.service;

import com.alibaba.fastjson.JSONObject;
import com.zes.authentication.common.ZES_Enum;
import com.zes.authentication.common.ZES_returnService;
import com.zes.authentication.dto.ZES_facilityIctRequest;
import com.zes.authentication.mapper.ZES_facilityMapper;
import com.zes.authentication.model.ZES_facilityInfo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZES_facilityService
{
    private final ZES_facilityMapper ZES_gv_facilityMapper;
    private final ZES_returnService ZES_gv_returnService;

    public ZES_facilityService(ZES_facilityMapper ZES_gv_facilityMapper, ZES_returnService ZES_gv_returnService)
    {
        this.ZES_gv_facilityMapper = ZES_gv_facilityMapper;
        this.ZES_gv_returnService = ZES_gv_returnService;
    }

    @Transactional
    public JSONObject ZES_facilityDetail(ZES_facilityIctRequest request)
    {
        List<ZES_facilityInfo> ZES_lv_result = ZES_gv_facilityMapper.ZES_selectActiveFacilitiesByCompanyCode(request.getCompanyCode());

        if (ZES_lv_result.isEmpty())
        {
            return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_NOT_FOUND, "No active facility found", ZES_lv_result);
        }

        return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_SUCCESS, "success", ZES_lv_result);
    }
}
