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

import java.util.List;
import java.util.Set;

@Component
public class ZES_opcUaServerRunner implements ApplicationRunner
{
    private static final String APP_URI = "urn:lsexp2:test:opcua:server";
    private static final String BIND_IP = "127.0.0.1";
    private static final String BIND_ADDRESS = "0.0.0.0";
    private static final int ENDPOINT_PORT = 8624;
    private static final String ENDPOINT_PATH = "/lsexp2-test";

    private final ZES_opcUaWorkItemProvider ZES_gv_workItemProvider;

    public ZES_opcUaServerRunner(ZES_opcUaWorkItemProvider ZES_gv_workItemProvider)
    {
        this.ZES_gv_workItemProvider = ZES_gv_workItemProvider;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception
    {
        OpcUaServer ZES_lv_server = createServer();
        ZES_lv_server.startup().get();
        addDbBackedNodes(ZES_lv_server);
    }

    private OpcUaServer createServer()
    {
        EndpointConfiguration endpoint = EndpointConfiguration.newBuilder()
                .setBindAddress(BIND_ADDRESS)
                .setHostname(BIND_IP)
                .setPath(ENDPOINT_PATH)
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
                .setSecurityPolicy(SecurityPolicy.None)
                .setSecurityMode(MessageSecurityMode.None)
                .build();

        OpcUaServerConfig config = OpcUaServerConfig.builder()
                .setEndpoints(Set.of(endpoint))
                .setIdentityValidator(new AnonymousIdentityValidator())
                .setBuildInfo(new BuildInfo(APP_URI, "zestech", "ZES OPC-UA Server", OpcUaServer.SDK_VERSION, "1.0.0", DateTime.now()))
                .build();

        return new OpcUaServer(config);
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

        List<ZES_opcUaWorkItem> ZES_lv_items = ZES_gv_workItemProvider.ZES_getWorkItemsByEquipmentNo(1);
        String ZES_lv_productName = ZES_lv_items.isEmpty() ? "" : ZES_lv_items.get(0).productName();

        UaVariableNode node = UaVariableNode.builder(nodeContext)
                .setNodeId(new NodeId(nsIndex, "LS_EXP2/workReport/detail/productnameDetail"))
                .setBrowseName(new QualifiedName(nsIndex, "productnameDetail"))
                .setDisplayName(LocalizedText.english("productnameDetail"))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();
        node.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY));
        node.setValue(new DataValue(new Variant(ZES_lv_productName)));
        nodeManager.addNode(node);
        nodeManager.addReferences(new Reference(rootFolder.getNodeId(), Identifiers.Organizes, node.getNodeId().expanded(), true), server.getNamespaceTable());
    }
}
