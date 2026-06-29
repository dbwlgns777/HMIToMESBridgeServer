package com.zes.hmitomesbridgeserver.opcua;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ZES_opcUaService
{
    public String ZES_readNodeValue(String endpointUrl, String nodeId) throws Exception
    {
        System.out.println("endpointUrl =>" + endpointUrl);
        OpcUaClient ZES_lv_client = OpcUaClient.create(endpointUrl);
        System.out.println("ZES_readNodeValue =>");

        try
        {
            ZES_lv_client.connect().get();

            NodeId ZES_lv_nodeId = NodeId.parse(nodeId);
            CompletableFuture<DataValue> ZES_lv_readFuture = ZES_lv_client.readValue(
                    0,
                    TimestampsToReturn.Both,
                    ZES_lv_nodeId
            );

            DataValue ZES_lv_dataValue = ZES_lv_readFuture.get();
            if (ZES_lv_dataValue.getValue() == null)
            {
                return "null";
            }

            return String.valueOf(ZES_lv_dataValue.getValue().getValue());
        }
        finally
        {
            ZES_lv_client.disconnect().get();
        }
    }
}
