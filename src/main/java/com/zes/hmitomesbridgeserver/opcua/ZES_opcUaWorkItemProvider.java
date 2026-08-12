package com.zes.hmitomesbridgeserver.opcua;

import java.util.List;

public interface ZES_opcUaWorkItemProvider
{
    List<ZES_opcUaWorkItem> ZES_getWorkItemsByIctNumber(String ictNumber);

    ZES_opcUaWorkItemPage ZES_getWorkItemsByIctNumber(String ictNumber, int page, int size);

    ZES_workHistoryState ZES_getLatestActiveWorkHistory(String workOrderCode);

    ZES_workHistoryState ZES_getActiveWorkHistory(String workHistoryCode);

    boolean ZES_hasOtherWorkingHistory(String companyCode, String workHistoryCode);
}
