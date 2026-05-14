package com.zes.hmitomesbridgeserver.opcua;

public record ZES_opcUaWorkItem(
        String productCode,
        String productName,
        String processName,
        String deadline,
        short targetProduction
) {
}
