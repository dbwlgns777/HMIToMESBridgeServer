package com.zes.hmitomesbridgeserver.opcua;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ZES_dbWorkItemProvider implements ZES_opcUaWorkItemProvider
{
    @Override
    public List<ZES_opcUaWorkItem> ZES_getWorkItemsByIctNumber(String ictNumber)
    {
        // TODO: ictNumber -> facility/process -> work order를 DB 조회해 변환
        return List.of(
                new ZES_opcUaWorkItem("", "해당 설비에 내려진 작업지시서가 없습니다", "", "", (short) 0)
        );
    }
}
