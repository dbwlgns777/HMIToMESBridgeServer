package com.zes.hmitomesbridgeserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ZES_workOrderMapper
{
    @Select("""
            select work_order_code, unique_work_order_number, instruction_date, orders_code, deadline, target_production,
                   work_statement, facility_code, product_code
            from ZES_Authentication.zes_work_order_info
            where work_order_code = #{workOrderCode}
              and statement = 'active'
            limit 1
            """)
    Map<String, Object> ZES_selectWorkOrderByCode(@Param("workOrderCode") String workOrderCode);

    @Select("select facility_code from ZES_Authentication.zes_facility_info where ict_number = #{ictNumber} and statement = 'active'")
    List<String> ZES_selectFacilityCodesByIctNumber(@Param("ictNumber") String ictNumber);

    @Select("""
            select facility_code, company_code
            from ZES_Authentication.zes_facility_info
            where ict_number = #{ictNumber}
              and statement = 'active'
            limit 1
            """)
    Map<String, Object> ZES_selectFacilityAndCompanyByIctNumber(@Param("ictNumber") String ictNumber);

    @Select("select monitoring_type_code from ZES_Authentication.pms_monitoring_info where FIND_IN_SET(#{facilityCode}, facility_code) > 0 and statement = 'active'")
    List<String> ZES_selectMonitoringTypeCodes(@Param("facilityCode") String facilityCode);

    @Select("""
            select work_order_code
            from ZES_Authentication.zes_work_order_info
            where company_code = #{companyCode}
              and (work_statement = 'before' or work_statement = 'working')
              and deadline >= #{today}
              and statement = 'active'
            order by deadline desc
            """)
    List<String> ZES_selectWorkOrderCodesByCompanyCodeAndToday(@Param("companyCode") String companyCode, @Param("today") String today);

    @Select("""
            select serial_code, product_name, product_code, cavity, process_code
            from ZES_Authentication.zes_product_info
            where process_code = #{processCode}
              and statement = 'active'
            """)
    List<Map<String, Object>> ZES_selectProductsByProcessCode(@Param("processCode") String processCode);

    @Select("""
            select *
            from ZES_Authentication.zes_work_order_info
            where product_code = #{productCode}
              and work_order_code = #{workOrderCode}
              and statement = 'active'
            limit 1
            """)
    Map<String, Object> ZES_selectWorkOrderByProductAndWorkOrder(@Param("productCode") String productCode, @Param("workOrderCode") String workOrderCode);

    @Select("select product_code, product_name, serial_code, process_code, cavity from ZES_Authentication.zes_product_info where process_code in (${processCodes}) and statement = 'active'")
    List<Map<String, Object>> ZES_selectProductsByProcessCodes(@Param("processCodes") String processCodes);

    @Select("select product_code, product_name, serial_code, process_code, cavity from ZES_Authentication.zes_product_info where product_code = #{productCode} and statement = 'active' limit 1")
    Map<String, Object> ZES_selectProductByProductCode(@Param("productCode") String productCode);

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

    @Select("""
            select count(*)
            from ZES_Authentication.zes_facility_info f
            join ZES_Authentication.pms_monitoring_info m
              on FIND_IN_SET(f.facility_code, m.facility_code) > 0
             and m.statement = 'active'
            join ZES_Authentication.zes_product_info p
              on p.process_code = m.monitoring_type_code
             and p.statement = 'active'
            join ZES_Authentication.zes_work_order_info w
              on w.product_code = p.product_code
             and w.company_code = f.company_code
             and (w.work_statement = 'before' or w.work_statement = 'working')
             and str_to_date(replace(w.deadline, '.', '-'), '%Y-%m-%d') >= str_to_date(#{today}, '%Y-%m-%d')
             and w.statement = 'active'
            where f.ict_number = #{ictNumber}
              and f.statement = 'active'
            """)
    int ZES_countOpcUaWorkItemsByIctNumber(@Param("ictNumber") String ictNumber,
                                           @Param("today") String today);

    @Select("""
            select p.product_code, p.product_name, p.serial_code, p.process_code,
                   coalesce(pt.process_name, '') as process_row,
                   f.facility_name, f.facility_code, f.company_code,
                   pd.process_defect_code, pd.process_defect_name,
                   w.deadline, w.target_production
            from ZES_Authentication.zes_facility_info f
            join ZES_Authentication.pms_monitoring_info m
              on FIND_IN_SET(f.facility_code, m.facility_code) > 0
             and m.statement = 'active'
            join ZES_Authentication.zes_product_info p
              on p.process_code = m.monitoring_type_code
             and p.statement = 'active'
            left join ZES_Authentication.zes_process_type_info pt
              on pt.process_code = p.process_code
             and pt.statement = 'active'
            left join ZES_Authentication.zes_process_defect_info pd
              on pd.process_code = p.process_code
             and pd.process_defect_name = '기타'
             and pd.statement = 'active'
            join ZES_Authentication.zes_work_order_info w
              on w.product_code = p.product_code
             and w.company_code = f.company_code
             and (w.work_statement = 'before' or w.work_statement = 'working')
             and str_to_date(replace(w.deadline, '.', '-'), '%Y-%m-%d') >= str_to_date(#{today}, '%Y-%m-%d')
             and w.statement = 'active'
            where f.ict_number = #{ictNumber}
              and f.statement = 'active'
            order by datediff(str_to_date(replace(w.deadline, '.', '-'), '%Y-%m-%d'), str_to_date(#{today}, '%Y-%m-%d')) asc, w.work_order_code desc, p.product_code
            limit #{size} offset #{offset}
            """)
    List<Map<String, Object>> ZES_selectOpcUaWorkItemsByIctNumber(@Param("ictNumber") String ictNumber,
                                                                  @Param("today") String today,
                                                                  @Param("size") int size,
                                                                  @Param("offset") int offset);
}