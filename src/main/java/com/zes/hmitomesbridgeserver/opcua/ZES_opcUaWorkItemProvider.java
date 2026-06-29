package com.zes.hmitomesbridgeserver.opcua;

import java.util.List;

public interface ZES_opcUaWorkItemProvider
{
    List<ZES_opcUaWorkItem> ZES_getWorkItemsByIctNumber(String ictNumber);

    ZES_opcUaWorkItemPage ZES_getWorkItemsByIctNumber(String ictNumber, int page, int size);
}