package com.zes.hmitomesbridgeserver.mapper;

import com.zes.hmitomesbridgeserver.model.ZES_facilityInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ZES_facilityMapper
{
    @Select("""
            select *
            from ZES_Authentication.zes_facility_info
            where company_code = #{companyCode}
              and statement = 'active'
            """)
    @Results(id = "facilityInfoResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "companyCode", column = "company_code"),
            @Result(property = "statement", column = "statement"),
            @Result(property = "facilityCode", column = "facility_code"),
            @Result(property = "facilityName", column = "facility_name")
    })
    List<ZES_facilityInfo> ZES_selectActiveFacilitiesByCompanyCode(@Param("companyCode") String companyCode);
}
