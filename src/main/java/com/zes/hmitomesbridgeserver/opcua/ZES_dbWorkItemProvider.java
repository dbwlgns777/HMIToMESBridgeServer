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
        JSONObject ZES_lv_response = ZES_gv_workOrderKioskService.ZES_kioskActiveWorkOrderListByIctNumber(1, 200, ictNumber);
        if (!"ZES_SUCCESS".equals(String.valueOf(ZES_lv_response.get("code")))) return ZES_lv_result;

        JSONObject ZES_lv_data = (JSONObject) ZES_lv_response.get("data");
        JSONArray ZES_lv_rows = (JSONArray) ZES_lv_data.get("row");
        for (Object procObj : ZES_lv_rows)
        {
            JSONObject proc = (JSONObject) procObj;
            JSONObject workOrders = (JSONObject) proc.get("workOrders");
            JSONArray workRows = (JSONArray) workOrders.get("row");
            for (Object rowObj : workRows)
            {
                JSONObject row = (JSONObject) rowObj;
                String tg = String.valueOf(row.getOrDefault("targetProduction", "0"));
                short target;
                try { target = (short) Double.parseDouble(tg); } catch (Exception e) { target = 0; }
                ZES_lv_result.add(new ZES_opcUaWorkItem(
                        String.valueOf(row.getOrDefault("productCode", "")),
                        String.valueOf(row.getOrDefault("productName", "")),
                        String.valueOf(row.getOrDefault("serialCode", "")),
                        String.valueOf(row.getOrDefault("processName", "")),
                        String.valueOf(row.getOrDefault("deadline", "")),
                        target
                ));
            }
        }
        return ZES_lv_result;
    }
}
