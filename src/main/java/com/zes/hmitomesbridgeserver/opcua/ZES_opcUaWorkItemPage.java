package com.zes.hmitomesbridgeserver.opcua;

import java.util.List;

public record ZES_opcUaWorkItemPage(List<ZES_opcUaWorkItem> items, short totalPage)
{
    public ZES_opcUaWorkItemPage
    {
        items = items == null ? List.of() : List.copyOf(items);
        totalPage = (short) Math.max(1, totalPage);
    }
}