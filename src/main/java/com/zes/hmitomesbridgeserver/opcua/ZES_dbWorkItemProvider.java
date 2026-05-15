package com.zes.hmitomesbridgeserver.opcua;

import com.zes.hmitomesbridgeserver.mapper.ZES_workOrderMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ZES_dbWorkItemProvider implements ZES_opcUaWorkItemProvider
{
    private final ZES_workOrderMapper ZES_gv_workOrderMapper;

    public ZES_dbWorkItemProvider(ZES_workOrderMapper ZES_gv_workOrderMapper)
    {
        this.ZES_gv_workOrderMapper = ZES_gv_workOrderMapper;
    }

    @Override
    public List<ZES_opcUaWorkItem> ZES_getWorkItemsByIctNumber(String ictNumber)
    {
        List<ZES_opcUaWorkItem> ZES_lv_result = new ArrayList<>();

        Map<String, Object> ZES_lv_facilityInfo = ZES_gv_workOrderMapper.ZES_selectFacilityAndCompanyByIctNumber(ictNumber);
        if (ZES_lv_facilityInfo == null) return ZES_lv_result;
        String ZES_lv_facilityCode = String.valueOf(ZES_lv_facilityInfo.getOrDefault("facility_code", ""));
        String ZES_lv_companyCode = String.valueOf(ZES_lv_facilityInfo.getOrDefault("company_code", ""));
        if (ZES_lv_facilityCode.isBlank() || ZES_lv_companyCode.isBlank()) return ZES_lv_result;

        List<String> ZES_lv_workOrderCodeList = ZES_gv_workOrderMapper.ZES_selectWorkOrderCodesByCompanyCodeAndToday(
                ZES_lv_companyCode, LocalDate.now().toString()
        );
        if (ZES_lv_workOrderCodeList.isEmpty()) return ZES_lv_result;

        List<String> ZES_lv_monitoringTypeCodeList = ZES_gv_workOrderMapper.ZES_selectMonitoringTypeCodes(ZES_lv_facilityCode);
        if (ZES_lv_monitoringTypeCodeList.isEmpty()) return ZES_lv_result;

        for (String ZES_lv_monitoringTypeCode : ZES_lv_monitoringTypeCodeList)
        {
            List<Map<String, Object>> ZES_lv_products = ZES_gv_workOrderMapper.ZES_selectProductsByProcessCode(ZES_lv_monitoringTypeCode);
            for (Map<String, Object> ZES_lv_product : ZES_lv_products)
            {
                String ZES_lv_productCode = String.valueOf(ZES_lv_product.getOrDefault("product_code", ""));
                String ZES_lv_productName = String.valueOf(ZES_lv_product.getOrDefault("product_name", ""));
                String ZES_lv_serialCode = String.valueOf(ZES_lv_product.getOrDefault("serial_code", ""));
                if (ZES_lv_productCode.isBlank()) continue;

                for (String ZES_lv_workOrderCode : ZES_lv_workOrderCodeList)
                {
                    Map<String, Object> ZES_lv_workOrder = ZES_gv_workOrderMapper.ZES_selectWorkOrderByProductAndWorkOrder(ZES_lv_productCode, ZES_lv_workOrderCode);
                    if (ZES_lv_workOrder == null || ZES_lv_workOrder.isEmpty()) continue;

                    String ZES_lv_target = String.valueOf(ZES_lv_workOrder.getOrDefault("target_production", "0"));
                    short ZES_lv_targetGoal;
                    try { ZES_lv_targetGoal = (short) Double.parseDouble(ZES_lv_target); } catch (Exception e) { ZES_lv_targetGoal = 0; }
                    ZES_lv_result.add(new ZES_opcUaWorkItem(
                            ZES_lv_productCode,
                            ZES_lv_productName,
                            ZES_lv_serialCode,
                            ZES_lv_monitoringTypeCode,
                            String.valueOf(ZES_lv_workOrder.getOrDefault("deadline", "")),
                            ZES_lv_targetGoal
                    ));
                }
            }
        }

        return ZES_lv_result;
    }
}
