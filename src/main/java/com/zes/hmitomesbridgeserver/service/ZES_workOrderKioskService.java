package com.zes.hmitomesbridgeserver.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zes.hmitomesbridgeserver.common.ZES_Enum;
import com.zes.hmitomesbridgeserver.common.ZES_returnService;
import com.zes.hmitomesbridgeserver.mapper.ZES_workOrderMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ZES_workOrderKioskService
{
    private final ZES_workOrderMapper ZES_gv_workOrderMapper;
    private final ZES_returnService ZES_gv_returnService;

    public ZES_workOrderKioskService(ZES_workOrderMapper ZES_gv_workOrderMapper, ZES_returnService ZES_gv_returnService)
    {
        this.ZES_gv_workOrderMapper = ZES_gv_workOrderMapper;
        this.ZES_gv_returnService = ZES_gv_returnService;
    }

    public JSONObject ZES_kioskActiveWorkOrderList(Integer page, Integer size, String processCode)
    {
        try
        {
            if (page == null || size == null || page < 1 || size < 1)
            {
                return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_VALID_ERROR, "page/size must be >= 1", null);
            }
            if (processCode == null || processCode.trim().isEmpty())
            {
                return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_VALID_ERROR, "processCode is required", null);
            }

            List<String> ZES_lv_workOrderCodeList = ZES_workingHistoryOrderFilter(processCode, LocalDate.now().toString());
            JSONObject ZES_lv_response = new JSONObject();
            JSONArray ZES_lv_array = new JSONArray();
            int ZES_lv_totalRows = ZES_lv_workOrderCodeList.size();

            int ZES_lv_start = Math.max((page - 1) * size, 0);
            int ZES_lv_end = Math.min(ZES_lv_start + size, ZES_lv_totalRows);
            if (ZES_lv_start < ZES_lv_end)
            {
                for (String workOrderCode : ZES_lv_workOrderCodeList.subList(ZES_lv_start, ZES_lv_end))
                {
                    JSONObject ZES_lv_json = ZES_buildWorkOrderRow(workOrderCode);
                    ZES_lv_array.add(ZES_lv_json);
                }
            }

            ZES_lv_response.put("total_page", (ZES_lv_totalRows / size) + (ZES_lv_totalRows % size > 0 ? 1 : 0));
            ZES_lv_response.put("row", ZES_lv_array);
            return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_SUCCESS, "success", ZES_lv_response);
        }
        catch (Exception e)
        {
            return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_SERVER_ERROR, e.getMessage(), null);
        }
    }

    private JSONObject ZES_buildWorkOrderRow(String workOrderCode)
    {
        JSONObject ZES_lv_json = new JSONObject();
        ZES_lv_json.put("workOrderCode", workOrderCode);

        List<Map<String, Object>> codes = ZES_gv_workOrderMapper.ZES_selectNotEndedWorkOrdersByProductCodes("'DUMMY'");
        // NOTE: work_order_code 단건 조회 mapper가 없어서 아래는 필요한 컬럼 보강 후 교체 권장.

        ZES_lv_json.put("uniqueWorkOrderNumber", "");
        ZES_lv_json.put("instructionDate", "");
        ZES_lv_json.put("ordersCode", "");
        ZES_lv_json.put("deadline", "");
        ZES_lv_json.put("targetProduction", "0");
        ZES_lv_json.put("workStatement", "");
        ZES_lv_json.put("facilityCode", "");
        ZES_lv_json.put("facilityName", "");
        ZES_lv_json.put("productCode", "");
        ZES_lv_json.put("productName", "");
        ZES_lv_json.put("serialCode", "");
        ZES_lv_json.put("processCode", "");
        ZES_lv_json.put("processName", "");
        ZES_lv_json.put("cavity", 1);
        ZES_lv_json.put("totalGoodQuantity", 0);
        ZES_lv_json.put("totalDefectQuantity", 0);
        ZES_lv_json.put("dataExistence", false);
        return ZES_lv_json;
    }

    public JSONObject ZES_findByDefectiveQuantityInfo(String workHistoryCode, String processCode)
    {
        try
        {
            JSONArray ZES_lv_jsonArray = new JSONArray();
            JSONObject ZES_lv_response = new JSONObject();
            ZES_lv_response.put("defectNotice", "");

            if (processCode == null || processCode.trim().isEmpty())
            {
                ZES_lv_response.put("totalDefectQuantity", "0");
                ZES_lv_response.put("row", ZES_lv_jsonArray);
                return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_SUCCESS, "success", ZES_lv_response);
            }

            List<Map<String, Object>> ZES_lv_processDefectInfo = ZES_gv_workOrderMapper.ZES_selectProcessDefects(processCode);
            double ZES_lv_totalDefectiveQuantity = 0;

            for (Map<String, Object> row : ZES_lv_processDefectInfo)
            {
                JSONObject ZES_lv_defectInfoJson = new JSONObject();
                String ZES_lv_processDefectCode = String.valueOf(row.get("process_defect_code"));
                Map<String, Object> ZES_lv_mgmt = ZES_gv_workOrderMapper.ZES_selectProcessDefectManagement(ZES_lv_processDefectCode, workHistoryCode);
                if (ZES_lv_mgmt != null)
                {
                    ZES_lv_defectInfoJson.put("processDefectManagementNo", ZES_lv_mgmt.get("process_defect_management_no"));
                    ZES_lv_defectInfoJson.put("processDefectName", ZES_lv_mgmt.get("type"));
                    ZES_lv_defectInfoJson.put("defectQuantity", ZES_lv_mgmt.get("defect_quantity"));
                    ZES_lv_defectInfoJson.put("processDefectCode", ZES_lv_mgmt.get("process_defect_code"));
                    ZES_lv_totalDefectiveQuantity += Double.parseDouble(String.valueOf(ZES_lv_mgmt.get("defect_quantity")));
                }
                else
                {
                    ZES_lv_defectInfoJson.put("processDefectName", row.get("process_defect_name"));
                    ZES_lv_defectInfoJson.put("defectQuantity", "0");
                    ZES_lv_defectInfoJson.put("processDefectCode", ZES_lv_processDefectCode);
                }
                ZES_lv_jsonArray.add(ZES_lv_defectInfoJson);
            }

            ZES_lv_response.put("totalDefectQuantity", String.valueOf(ZES_lv_totalDefectiveQuantity));
            ZES_lv_response.put("row", ZES_lv_jsonArray);
            return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_SUCCESS, "success", ZES_lv_response);
        }
        catch (Exception e)
        {
            return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_SERVER_ERROR, e.getMessage(), null);
        }
    }

    public JSONObject ZES_resolveMonitoringTypeCodesByIctNumber(String ictNumber)
    {
        try
        {
            List<String> ZES_lv_facilityCodes = ZES_gv_workOrderMapper.ZES_selectFacilityCodesByIctNumber(ictNumber);
            if (ZES_lv_facilityCodes.size() > 1) return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_VALID_ERROR, "duplication", ZES_lv_facilityCodes);
            if (ZES_lv_facilityCodes.isEmpty()) return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_NOT_FOUND, "facility not found", null);

            String ZES_lv_facilityCode = ZES_lv_facilityCodes.get(0);
            List<String> ZES_lv_monitoringTypeCodes = ZES_gv_workOrderMapper.ZES_selectMonitoringTypeCodes(ZES_lv_facilityCode);
            JSONObject ZES_lv_data = new JSONObject();
            ZES_lv_data.put("facilityCode", ZES_lv_facilityCode);
            ZES_lv_data.put("monitoringTypeCodes", ZES_lv_monitoringTypeCodes);
            return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_SUCCESS, "success", ZES_lv_data);
        }
        catch (Exception e)
        {
            Throwable ZES_lv_root = e;
            while (ZES_lv_root.getCause() != null)
            {
                ZES_lv_root = ZES_lv_root.getCause();
            }

            String ZES_lv_message;
            if (ZES_lv_root.getClass().getSimpleName().contains("ConnectException"))
            {
                ZES_lv_message = "DB server connection timeout. Check DB host/port/network.";
            }
            else
            {
                ZES_lv_message = "DB query failed: " + ZES_lv_root.getClass().getSimpleName();
            }

            return ZES_gv_returnService.ZES_returnToFormat(ZES_Enum.ZES_SERVER_ERROR, ZES_lv_message, null);
        }
    }

    public List<String> ZES_workingHistoryOrderFilter(String code, String date)
    {
        List<String> ZES_lv_processCodes = Arrays.stream(code.split(",")).map(String::trim).filter(v -> !v.isEmpty()).toList();
        LocalDate ZES_lv_today = LocalDate.parse(date);
        String ZES_lv_instructionDateBeforeDate = ZES_lv_today.plusDays(1).toString().replace("-", ".");
        String ZES_lv_deadLineAfterDate = ZES_lv_today.minusDays(1).toString().replace("-", ".");
        if (ZES_lv_processCodes.isEmpty()) return List.of();

        String ZES_lv_processCodesIn = ZES_lv_processCodes.stream().map(v -> "'" + v + "'").collect(Collectors.joining(","));
        List<Map<String, Object>> ZES_lv_products = ZES_gv_workOrderMapper.ZES_selectProductsByProcessCodes(ZES_lv_processCodesIn);
        List<String> ZES_lv_productCodes = ZES_lv_products.stream().map(v -> String.valueOf(v.get("product_code"))).toList();
        if (ZES_lv_productCodes.isEmpty()) return List.of();

        String ZES_lv_productCodesIn = ZES_lv_productCodes.stream().map(v -> "'" + v + "'").collect(Collectors.joining(","));
        List<Map<String, Object>> ZES_lv_workOrderInfoList = ZES_gv_workOrderMapper.ZES_selectActiveWorkOrdersByProductCodes(ZES_lv_productCodesIn, ZES_lv_instructionDateBeforeDate, ZES_lv_deadLineAfterDate);
        List<Map<String, Object>> ZES_lv_workingHistoryOrderList = ZES_gv_workOrderMapper.ZES_selectNotEndedWorkOrdersByProductCodes(ZES_lv_productCodesIn);

        Set<String> ZES_lv_seen = new HashSet<>();
        return java.util.stream.Stream.concat(ZES_lv_workOrderInfoList.stream(), ZES_lv_workingHistoryOrderList.stream())
                .map(v -> String.valueOf(v.get("work_order_code"))).filter(ZES_lv_seen::add).toList();
    }
}
