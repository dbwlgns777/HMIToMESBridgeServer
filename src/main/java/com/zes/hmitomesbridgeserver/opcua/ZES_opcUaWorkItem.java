package com.zes.hmitomesbridgeserver.opcua;

public record ZES_opcUaWorkItem(
        String productCode,
        String productName,
        String customer,
        String process,
        String workDeadline,
        short targetQuantity
) {
}
