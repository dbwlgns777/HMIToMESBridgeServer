package com.zes.hmitomesbridgeserver.opcua;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ZES_dbWorkItemProvider implements ZES_opcUaWorkItemProvider
{
    @Override
    public List<ZES_opcUaWorkItem> ZES_getWorkItemsByIctNumber(String ictNumber)
    {
        // TODO DB 연동
        List<ZES_opcUaWorkItem> ZES_lv_items = new ArrayList<>();
        for (int i = 1; i <= 12; i++)
        {
            ZES_lv_items.add(new ZES_opcUaWorkItem(
                    "P" + i,
                    "PRODUCT_" + i,
                    "SERIAL_" + i,
                    "PROCESS_" + i,
                    "2026-05-" + String.format("%02d", i),
                    (short) (1000 + i)
            ));
        }
        return ZES_lv_items;
    }
}
