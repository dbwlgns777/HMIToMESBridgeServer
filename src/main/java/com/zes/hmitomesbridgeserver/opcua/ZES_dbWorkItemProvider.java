package com.zes.hmitomesbridgeserver.opcua;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zes.hmitomesbridgeserver.mapper.ZES_workOrderMapper;
import com.zes.hmitomesbridgeserver.service.ZES_workOrderKioskService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ZES_dbWorkItemProvider implements ZES_opcUaWorkItemProvider
{
    private final ZES_workOrderKioskService ZES_gv_workOrderKioskService;
    private final ZES_workOrderMapper ZES_gv_workOrderMapper;

    public ZES_dbWorkItemProvider(ZES_workOrderKioskService ZES_gv_workOrderKioskService, ZES_workOrderMapper ZES_gv_workOrderMapper)
    {
        this.ZES_gv_workOrderKioskService = ZES_gv_workOrderKioskService;
        this.ZES_gv_workOrderMapper = ZES_gv_workOrderMapper;
    }

    @Override
    public List<ZES_opcUaWorkItem> ZES_getWorkItemsByIctNumber(String ictNumber)
    {
        List<ZES_opcUaWorkItem> ZES_lv_result = new ArrayList<>();
        JSONObject ZES_lv_response = ZES_gv_workOrderKioskService.ZES_kioskActiveWorkOrderListByIctNumber(1, Integer.MAX_VALUE, ictNumber);
        if (!"ZES_SUCCESS".equals(String.valueOf(ZES_lv_response.get("code")))) return ZES_lv_result;

        JSONObject ZES_lv_data = ZES_lv_response.getJSONObject("data");
        if (ZES_lv_data == null) return ZES_lv_result;

        JSONArray ZES_lv_processRows = ZES_lv_data.getJSONArray("row");
        if (ZES_lv_processRows == null) return ZES_lv_result;

        for (Object ZES_lv_processRowObject : ZES_lv_processRows)
        {
            if (!(ZES_lv_processRowObject instanceof JSONObject ZES_lv_processRow)) continue;
            JSONObject ZES_lv_workOrders = ZES_lv_processRow.getJSONObject("workOrders");
            if (ZES_lv_workOrders == null) continue;
            JSONArray ZES_lv_workOrderRows = ZES_lv_workOrders.getJSONArray("row");
            if (ZES_lv_workOrderRows == null) continue;

            for (Object ZES_lv_workOrderRowObject : ZES_lv_workOrderRows)
            {
                if (!(ZES_lv_workOrderRowObject instanceof JSONObject ZES_lv_workOrderRow)) continue;
                ZES_lv_result.add(ZES_toWorkItem(ZES_lv_workOrderRow));
            }
        }

        return ZES_lv_result;
    }

    @Override
    public ZES_opcUaWorkItemPage ZES_getWorkItemsByIctNumber(String ictNumber, int page, int size)
    {
        int ZES_lv_page = Math.max(1, page);
        int ZES_lv_size = Math.max(1, size);
        int ZES_lv_offset = (ZES_lv_page - 1) * ZES_lv_size;
        String ZES_lv_today = java.time.LocalDate.now().toString();

        int ZES_lv_totalRows = ZES_gv_workOrderMapper.ZES_countOpcUaWorkItemsByIctNumber(ictNumber, ZES_lv_today);
        short ZES_lv_totalPage = (short) Math.max(1, (ZES_lv_totalRows + ZES_lv_size - 1) / ZES_lv_size);
        if (ZES_lv_totalRows == 0) return new ZES_opcUaWorkItemPage(List.of(), ZES_lv_totalPage);

        List<Map<String, Object>> ZES_lv_rows = ZES_gv_workOrderMapper.ZES_selectOpcUaWorkItemsByIctNumber(ictNumber, ZES_lv_today, ZES_lv_size, ZES_lv_offset);
        List<ZES_opcUaWorkItem> ZES_lv_items = new ArrayList<>();
        for (Map<String, Object> ZES_lv_row : ZES_lv_rows)
        {
            ZES_lv_items.add(new ZES_opcUaWorkItem(
                    ZES_toStringOrEmpty(ZES_lv_row.get("product_code")),
                    ZES_toStringOrEmpty(ZES_lv_row.get("product_name")),
                    ZES_toStringOrEmpty(ZES_lv_row.get("serial_code")),
                    ZES_toStringOrEmpty(ZES_lv_row.get("process_row")),
                    ZES_toStringOrEmpty(ZES_lv_row.get("work_order_code")),
                    ZES_toStringOrEmpty(ZES_lv_row.get("deadline")),
                    ZES_toStringOrEmpty(ZES_lv_row.get("target_production")),
                    ZES_toStringOrEmpty(ZES_lv_row.get("facility_name")),
                    ZES_toStringOrEmpty(ZES_lv_row.get("facility_code")),
                    ZES_toStringOrEmpty(ZES_lv_row.get("process_defect_code")),
                    ZES_toStringOrEmpty(ZES_lv_row.get("process_defect_name")),
                    ZES_toStringOrEmpty(ZES_lv_row.get("company_code"))
            ));
        }

        return new ZES_opcUaWorkItemPage(ZES_lv_items, ZES_lv_totalPage);
    }

    private ZES_opcUaWorkItem ZES_toWorkItem(JSONObject ZES_lv_workOrderRow)
    {
        return new ZES_opcUaWorkItem(
                ZES_toStringOrEmpty(ZES_lv_workOrderRow.get("productCode")),
                ZES_toStringOrEmpty(ZES_lv_workOrderRow.get("productName")),
                ZES_toStringOrEmpty(ZES_lv_workOrderRow.get("serialCode")),
                ZES_toStringOrEmpty(ZES_lv_workOrderRow.get("processName")),
                ZES_toStringOrEmpty(ZES_lv_workOrderRow.get("workOrderCode")),
                ZES_toStringOrEmpty(ZES_lv_workOrderRow.get("deadline")),
                ZES_toStringOrEmpty(ZES_lv_workOrderRow.get("targetProduction")),
                ZES_toStringOrEmpty(ZES_lv_workOrderRow.get("facilityName")),
                ZES_toStringOrEmpty(ZES_lv_workOrderRow.get("facilityCode")),
                ZES_toStringOrEmpty(ZES_lv_workOrderRow.get("processDefectCode")),
                ZES_toStringOrEmpty(ZES_lv_workOrderRow.get("processDefectName")),
                ZES_toStringOrEmpty(ZES_lv_workOrderRow.get("companyCode"))
        );
    }

    private String ZES_toStringOrEmpty(Object value)
    {
        return value == null ? "" : String.valueOf(value);
    }

}