package com.zes.hmitomesbridgeserver.opcua;

import java.util.List;

public interface ZES_opcUaWorkItemProvider
{
    List<ZES_opcUaWorkItem> ZES_getWorkItemsByIctNumber(String ictNumber);
}
