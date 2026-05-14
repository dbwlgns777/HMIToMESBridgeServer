package com.zes.hmitomesbridgeserver.opcua;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.NodeManager;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.AnonymousIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

@Component
public class ZES_opcUaServerRunner implements ApplicationRunner
{
    private static final String APP_URI = "urn:lsexp2:test:opcua:server";
    private static final String BIND_IP = "192.168.89.2";
    private static final String BIND_ADDRESS = "0.0.0.0";
    private static final int ENDPOINT_PORT = 8624;
    private static final String ENDPOINT_PATH = "/lsexp2-test";
    private static final String ROOT_ENDPOINT_PATH = "/";

    private final ZES_opcUaWorkItemProvider ZES_gv_workItemProvider;

    public ZES_opcUaServerRunner(ZES_opcUaWorkItemProvider ZES_gv_workItemProvider)
    {
        this.ZES_gv_workItemProvider = ZES_gv_workItemProvider;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception
    {
        OpcUaServer ZES_lv_server = createServer();
        Runtime.getRuntime().addShutdownHook(new Thread(ZES_lv_server::shutdown));
        ZES_lv_server.startup().get();
        addDbBackedNodes(ZES_lv_server);

        System.out.println("OPC UA Test Server started.");
        System.out.println("Discovery Endpoint: opc.tcp://" + BIND_IP + ":" + ENDPOINT_PORT + ROOT_ENDPOINT_PATH);
        System.out.println("Service Endpoint: opc.tcp://" + BIND_IP + ":" + ENDPOINT_PORT + ENDPOINT_PATH);
    }

    private OpcUaServer createServer()
    {
        EndpointConfiguration serviceEndpoint = buildEndpoint(ENDPOINT_PATH);
        EndpointConfiguration discoveryEndpoint = buildEndpoint(ROOT_ENDPOINT_PATH);

        var configBuilder = OpcUaServerConfig.builder()
                .setEndpoints(Set.of(discoveryEndpoint, serviceEndpoint))
                .setIdentityValidator(new AnonymousIdentityValidator())
                .setBuildInfo(new BuildInfo(APP_URI, "openai", "LS eXP2 OPC UA Test Server", OpcUaServer.SDK_VERSION, "2.4.0", DateTime.now()));

        invokeIfPresent(configBuilder, "setBindPort", ENDPOINT_PORT);
        return new OpcUaServer(configBuilder.build());
    }

    private EndpointConfiguration buildEndpoint(String path)
    {
        EndpointConfiguration.Builder endpointBuilder = EndpointConfiguration.newBuilder()
                .setBindAddress(BIND_ADDRESS)
                .setHostname(BIND_IP)
                .setPath(path)
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
                .setSecurityPolicy(SecurityPolicy.None)
                .setSecurityMode(MessageSecurityMode.None);

        invokeIfPresent(endpointBuilder, "setBindPort", ENDPOINT_PORT);
        return endpointBuilder.build();
    }

    private void invokeIfPresent(Object target, String methodName, int value)
    {
        try
        {
            Method m = target.getClass().getMethod(methodName, int.class);
            m.invoke(target, value);
        }
        catch (Exception ignored)
        {
        }
    }

    private void addDbBackedNodes(OpcUaServer server)
    {
        UaNode objectsFolder = server.getAddressSpaceManager().getManagedNode(Identifiers.ObjectsFolder).orElseThrow();
        UShort nsIndex = objectsFolder.getNodeId().getNamespaceIndex();
        UaNodeContext nodeContext = objectsFolder.getNodeContext();
        @SuppressWarnings("unchecked")
        NodeManager<UaNode> nodeManager = (NodeManager<UaNode>) objectsFolder.getNodeManager();

        UaFolderNode rootFolder = new UaFolderNode(nodeContext, new NodeId(nsIndex, "LS_EXP2"), new QualifiedName(nsIndex, "LS_EXP2"), LocalizedText.english("LS_EXP2"));
        nodeManager.addNode(rootFolder);
        nodeManager.addReferences(new Reference(Identifiers.ObjectsFolder, Identifiers.Organizes, rootFolder.getNodeId().expanded(), true), server.getNamespaceTable());

        // equipmentNo를 ictNumber로 사용
        UaVariableNode ictNumberNode = createStringRwNode(nodeContext, nsIndex, "LS_EXP2/selectedIctNumber", "selectedIctNumber");
        ictNumberNode.setValue(new DataValue(new Variant("P0208258")));
        nodeManager.addNode(ictNumberNode);
        nodeManager.addReferences(new Reference(rootFolder.getNodeId(), Identifiers.Organizes, ictNumberNode.getNodeId().expanded(), true), server.getNamespaceTable());

        List<ZES_opcUaWorkItem> ZES_lv_items = ZES_gv_workItemProvider.ZES_getWorkItemsByIctNumber("P0208258");
        ZES_opcUaWorkItem ZES_lv_item = ZES_lv_items.isEmpty() ? new ZES_opcUaWorkItem("", "", "", "", (short) 0) : ZES_lv_items.get(0);

        addStringNode(nodeContext, nodeManager, server, rootFolder, nsIndex, "LS_EXP2/workReport/detail/productcodeDetail", "productcodeDetail", ZES_lv_item.productCode());
        addStringNode(nodeContext, nodeManager, server, rootFolder, nsIndex, "LS_EXP2/workReport/detail/productnameDetail", "productnameDetail", ZES_lv_item.productName());
        addStringNode(nodeContext, nodeManager, server, rootFolder, nsIndex, "LS_EXP2/workReport/detail/processDetail", "processDetail", ZES_lv_item.processName());
        addStringNode(nodeContext, nodeManager, server, rootFolder, nsIndex, "LS_EXP2/workReport/detail/workdeadlineDetail", "workdeadlineDetail", ZES_lv_item.deadline());

        addInt16RwNode(nodeContext, nodeManager, server, rootFolder, nsIndex, "LS_EXP2/workReportCurrentPage", "workReportCurrentPage", (short) 1);
        addInt16Node(nodeContext, nodeManager, server, rootFolder, nsIndex, "LS_EXP2/workReportTotalPage", "workReportTotalPage", (short) 1);
        addInt16RwNode(nodeContext, nodeManager, server, rootFolder, nsIndex, "LS_EXP2/workReportSelectedRow", "workReportSelectedRow", (short) 1);

        addStringNode(nodeContext, nodeManager, server, rootFolder, nsIndex, "LS_EXP2/workReport/row1/productcode", "productcode_row1", ZES_lv_item.productCode());
        addStringNode(nodeContext, nodeManager, server, rootFolder, nsIndex, "LS_EXP2/workReport/row1/productname", "productname_row1", ZES_lv_item.productName());
        addStringNode(nodeContext, nodeManager, server, rootFolder, nsIndex, "LS_EXP2/workReport/row1/process", "process_row1", ZES_lv_item.processName());
        addStringNode(nodeContext, nodeManager, server, rootFolder, nsIndex, "LS_EXP2/workReport/row1/workdeadline", "workdeadline_row1", ZES_lv_item.deadline());

        UaVariableNode targetNode = UaVariableNode.builder(nodeContext)
                .setNodeId(new NodeId(nsIndex, "LS_EXP2/workReport/detail/targetQuantityDetail"))
                .setBrowseName(new QualifiedName(nsIndex, "targetQuantityDetail"))
                .setDisplayName(LocalizedText.english("targetQuantityDetail"))
                .setDataType(Identifiers.Int16)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();
        targetNode.setValue(new DataValue(new Variant(ZES_lv_item.targetProduction())));
        nodeManager.addNode(targetNode);
        nodeManager.addReferences(new Reference(rootFolder.getNodeId(), Identifiers.Organizes, targetNode.getNodeId().expanded(), true), server.getNamespaceTable());
    }

    private UaVariableNode createStringRwNode(UaNodeContext nodeContext, UShort nsIndex, String id, String browseName)
    {
        UaVariableNode node = UaVariableNode.builder(nodeContext)
                .setNodeId(new NodeId(nsIndex, id))
                .setBrowseName(new QualifiedName(nsIndex, browseName))
                .setDisplayName(LocalizedText.english(browseName))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();
        node.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));
        node.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));
        return node;
    }


    private void addInt16Node(UaNodeContext nodeContext, NodeManager<UaNode> nodeManager, OpcUaServer server, UaFolderNode rootFolder,
                              UShort nsIndex, String nodeId, String browseName, short value)
    {
        UaVariableNode node = UaVariableNode.builder(nodeContext)
                .setNodeId(new NodeId(nsIndex, nodeId))
                .setBrowseName(new QualifiedName(nsIndex, browseName))
                .setDisplayName(LocalizedText.english(browseName))
                .setDataType(Identifiers.Int16)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();
        node.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY));
        node.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY));
        node.setValue(new DataValue(new Variant(value)));
        nodeManager.addNode(node);
        nodeManager.addReferences(new Reference(rootFolder.getNodeId(), Identifiers.Organizes, node.getNodeId().expanded(), true), server.getNamespaceTable());
    }

    private void addInt16RwNode(UaNodeContext nodeContext, NodeManager<UaNode> nodeManager, OpcUaServer server, UaFolderNode rootFolder,
                                UShort nsIndex, String nodeId, String browseName, short value)
    {
        UaVariableNode node = UaVariableNode.builder(nodeContext)
                .setNodeId(new NodeId(nsIndex, nodeId))
                .setBrowseName(new QualifiedName(nsIndex, browseName))
                .setDisplayName(LocalizedText.english(browseName))
                .setDataType(Identifiers.Int16)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();
        node.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));
        node.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));
        node.setValue(new DataValue(new Variant(value)));
        nodeManager.addNode(node);
        nodeManager.addReferences(new Reference(rootFolder.getNodeId(), Identifiers.Organizes, node.getNodeId().expanded(), true), server.getNamespaceTable());
    }

    private void addStringNode(UaNodeContext nodeContext, NodeManager<UaNode> nodeManager, OpcUaServer server, UaFolderNode rootFolder,
                               UShort nsIndex, String nodeId, String browseName, String value)
    {
        UaVariableNode node = UaVariableNode.builder(nodeContext)
                .setNodeId(new NodeId(nsIndex, nodeId))
                .setBrowseName(new QualifiedName(nsIndex, browseName))
                .setDisplayName(LocalizedText.english(browseName))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();
        node.setValue(new DataValue(new Variant(value)));
        nodeManager.addNode(node);
        nodeManager.addReferences(new Reference(rootFolder.getNodeId(), Identifiers.Organizes, node.getNodeId().expanded(), true), server.getNamespaceTable());
    }
}
