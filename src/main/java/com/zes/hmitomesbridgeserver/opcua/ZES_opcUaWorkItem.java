package com.zes.hmitomesbridgeserver.opcua;

public record ZES_opcUaWorkItem(
        String product_code,
        String product_name,
        String serial_code,
        String process_row,
        String work_order_code,
        String deadline,
        String target_goal,
        String facility_name,
        String facility_code,
        String process_defect_code,
        String process_defect_name,
        String company_code
) {
}
