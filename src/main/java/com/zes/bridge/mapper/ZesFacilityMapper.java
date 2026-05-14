package com.zes.bridge.mapper;

import com.zes.bridge.model.ZesFacilityInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZesFacilityMapper {
    List<ZesFacilityInfo> selectActiveFacilitiesByCompanyCode(@Param("companyCode") String companyCode);
}
