package com.zes.authentication.mapper;

import com.zes.authentication.model.ZES_facilityInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ZES_facilityMapper
{
    List<ZES_facilityInfo> ZES_selectActiveFacilitiesByCompanyCode(@Param("companyCode") String companyCode);
}
