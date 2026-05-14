package com.zes.hmitomesbridgeserver.opcua;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ZES_dbWorkItemProvider implements ZES_opcUaWorkItemProvider
{
    @Override
    public List<ZES_opcUaWorkItem> ZES_getWorkItemsByEquipmentNo(int equipmentNo)
    {
        // TODO: mapper를 주입해서 DB 조회 결과를 WorkItem으로 변환하세요.
        return List.of(
                new ZES_opcUaWorkItem("DB-NODATA", "해당 설비에 내려진 작업지시서가 없습니다", "", "", "", (short) 0)
        );
    }
}
