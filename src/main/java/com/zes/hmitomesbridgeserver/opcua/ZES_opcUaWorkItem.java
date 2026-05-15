package com.zes.hmitomesbridgeserver.opcua;

public record ZES_opcUaWorkItem(
        String product_code,
        String product_name,
        String serial_code,
        String process,
        String deadline,
        short target_goal
) {
}
