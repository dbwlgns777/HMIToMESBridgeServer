package com.zes.hmitomesbridgeserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ZES_workOrderMapper
{
    @Select("select facility_code from ZES_Authentication.zes_facility_info where ict_number = #{ictNumber} and statement = 'active'")
    List<String> ZES_selectFacilityCodesByIctNumber(@Param("ictNumber") String ictNumber);

    @Select("select monitoring_type_code from ZES_Authentication.pms_monitoring_info where FIND_IN_SET(#{facilityCode}, facility_code) > 0 and statement = 'active'")
    List<String> ZES_selectMonitoringTypeCodes(@Param("facilityCode") String facilityCode);

    @Select("select product_code, product_name, serial_code, process_code, cavity from ZES_Authentication.zes_product_info where process_code in (${processCodes}) and statement = 'active'")
    List<Map<String, Object>> ZES_selectProductsByProcessCodes(@Param("processCodes") String processCodes);

    @Select("""
            select work_order_code, unique_work_order_number, instruction_date, orders_code, deadline, target_production,
                   work_statement, facility_code, product_code
            from ZES_Authentication.zes_work_order_info
            where product_code in (${productCodes})
              and instruction_date < #{instructionDateBefore}
              and deadline > #{deadlineAfter}
              and work_statement != 'end'
              and statement = 'active'
            order by instruction_date
            """)
    List<Map<String, Object>> ZES_selectActiveWorkOrdersByProductCodes(@Param("productCodes") String productCodes,
                                                                        @Param("instructionDateBefore") String instructionDateBefore,
                                                                        @Param("deadlineAfter") String deadlineAfter);

    @Select("""
            select work_order_code, unique_work_order_number, instruction_date, orders_code, deadline, target_production,
                   work_statement, facility_code, product_code
            from ZES_Authentication.zes_work_order_info
            where product_code in (${productCodes}) and work_statement != 'end' and statement = 'active'
            """)
    List<Map<String, Object>> ZES_selectNotEndedWorkOrdersByProductCodes(@Param("productCodes") String productCodes);

    @Select("select facility_code, facility_name from ZES_Authentication.zes_facility_info where facility_code = #{facilityCode} and statement = 'active' limit 1")
    Map<String, Object> ZES_selectFacilityByFacilityCode(@Param("facilityCode") String facilityCode);

    @Select("select process_code, process_name from ZES_Authentication.zes_process_type_info where process_code = #{processCode} and statement = 'active' limit 1")
    Map<String, Object> ZES_selectProcessTypeByProcessCode(@Param("processCode") String processCode);

    @Select("select work_history_code, good_quantity, defect_notice from ZES_Authentication.zes_work_history_info where work_order_code = #{workOrderCode} and statement = 'active'")
    List<Map<String, Object>> ZES_selectWorkHistoryByWorkOrderCode(@Param("workOrderCode") String workOrderCode);

    @Select("select process_defect_code, process_defect_name from ZES_Authentication.zes_process_defect_info where process_code = #{processCode} and statement = 'active' order by process_index asc, process_defect_name")
    List<Map<String, Object>> ZES_selectProcessDefects(@Param("processCode") String processCode);

    @Select("""
            select process_defect_management_no, type, defect_quantity, process_defect_code
            from ZES_Authentication.zes_process_defect_management
            where process_defect_code = #{processDefectCode}
              and work_history_code = #{workHistoryCode}
              and statement = 'active'
            limit 1
            """)
    Map<String, Object> ZES_selectProcessDefectManagement(@Param("processDefectCode") String processDefectCode,
                                                           @Param("workHistoryCode") String workHistoryCode);
}
