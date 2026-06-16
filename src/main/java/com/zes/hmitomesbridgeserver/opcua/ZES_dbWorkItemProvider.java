package com.zes.hmitomesbridgeserver.opcua;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zes.hmitomesbridgeserver.service.ZES_workOrderKioskService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ZES_dbWorkItemProvider implements ZES_opcUaWorkItemProvider
{
    private final ZES_workOrderKioskService ZES_gv_workOrderKioskService;

    public ZES_dbWorkItemProvider(ZES_workOrderKioskService ZES_gv_workOrderKioskService)
    {
        this.ZES_gv_workOrderKioskService = ZES_gv_workOrderKioskService;
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
                ZES_lv_result.add(new ZES_opcUaWorkItem(
                        ZES_lv_workOrderRow.getString("productCode"),
                        ZES_lv_workOrderRow.getString("productName"),
                        ZES_lv_workOrderRow.getString("serialCode"),
                        ZES_lv_workOrderRow.getString("processCode"),
                        ZES_lv_workOrderRow.getString("deadline"),
                        ZES_parseTargetGoal(ZES_lv_workOrderRow.get("targetProduction"))
                ));
            }
        }

        return ZES_lv_result;
    }

    private short ZES_parseTargetGoal(Object targetProduction)
    {
        if (targetProduction == null) return 0;
        try { return (short) Double.parseDouble(String.valueOf(targetProduction)); } catch (Exception e) { return 0; }
    }
}
