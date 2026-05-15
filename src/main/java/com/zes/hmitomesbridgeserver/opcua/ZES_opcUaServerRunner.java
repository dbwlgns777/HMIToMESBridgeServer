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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        try { Method m = target.getClass().getMethod(methodName, int.class); m.invoke(target, value);} catch (Exception ignored) {}
    }

    private void addDbBackedNodes(OpcUaServer server)
    {
        UaNode objectsFolder = server.getAddressSpaceManager().getManagedNode(Identifiers.ObjectsFolder).orElseThrow();
        UShort nsIndex = objectsFolder.getNodeId().getNamespaceIndex();
        UaNodeContext ctx = objectsFolder.getNodeContext();
        @SuppressWarnings("unchecked") NodeManager<UaNode> nm = (NodeManager<UaNode>) objectsFolder.getNodeManager();

        UaFolderNode root = new UaFolderNode(ctx, new NodeId(nsIndex, "LS_EXP2"), new QualifiedName(nsIndex, "LS_EXP2"), LocalizedText.english("LS_EXP2"));
        nm.addNode(root);
        nm.addReferences(new Reference(Identifiers.ObjectsFolder, Identifiers.Organizes, root.getNodeId().expanded(), true), server.getNamespaceTable());

        UaVariableNode ict = rwString(ctx, nsIndex, "LS_EXP2/selectedIctNumber", "selectedIctNumber", "P0208258");
        UaVariableNode page = rwInt16(ctx, nsIndex, "LS_EXP2/workReportCurrentPage", "workReportCurrentPage", (short) 1);
        UaVariableNode totalPage = roInt16(ctx, nsIndex, "LS_EXP2/workReportTotalPage", "workReportTotalPage", (short) 1);
        UaVariableNode up = rwBool(ctx, nsIndex, "LS_EXP2/workReportPagePlus", "workReportPagePlus", false);
        UaVariableNode down = rwBool(ctx, nsIndex, "LS_EXP2/workReportPageMinus", "workReportPageMinus", false);
        add(nm, server, root, ict); add(nm, server, root, page); add(nm, server, root, totalPage); add(nm, server, root, up); add(nm, server, root, down);

        UaVariableNode[] serial = new UaVariableNode[5];
        UaVariableNode[] pname = new UaVariableNode[5];
        UaVariableNode[] target = new UaVariableNode[5];
        UaVariableNode[] process = new UaVariableNode[5];
        UaVariableNode[] deadline = new UaVariableNode[5];
        for (int i=0;i<5;i++)
        {
            int r=i+1;
            serial[i]=roString(ctx, nsIndex, "LS_EXP2/row"+r+"/serial_code", "serial_code_row"+r, "");
            pname[i]=roString(ctx, nsIndex, "LS_EXP2/row"+r+"/product_name", "product_name_row"+r, "");
            target[i]=roInt16(ctx, nsIndex, "LS_EXP2/row"+r+"/target_goal", "target_goal_row"+r, (short)0);
            process[i]=roString(ctx, nsIndex, "LS_EXP2/row"+r+"/process", "process_row"+r, "");
            deadline[i]=roString(ctx, nsIndex, "LS_EXP2/row"+r+"/deadline", "deadline_row"+r, "");
            add(nm, server, root, serial[i]); add(nm, server, root, pname[i]); add(nm, server, root, target[i]); add(nm, server, root, process[i]); add(nm, server, root, deadline[i]);
        }

        ScheduledExecutorService sch= Executors.newSingleThreadScheduledExecutor();
        final short[] cur={1};
        sch.scheduleAtFixedRate(()->{
            String ictNo=String.valueOf(ict.getValue().getValue().getValue());
            List<ZES_opcUaWorkItem> items=ZES_gv_workItemProvider.ZES_getWorkItemsByIctNumber(ictNo);
            short pages=(short)Math.max(1, (items.size()+4)/5);
            totalPage.setValue(new DataValue(new Variant(pages)));

            boolean u=Boolean.TRUE.equals(up.getValue().getValue().getValue());
            boolean d=Boolean.TRUE.equals(down.getValue().getValue().getValue());
            short req=((Number)page.getValue().getValue().getValue()).shortValue();
            if(u){req=(short)Math.min(pages,cur[0]+1); up.setValue(new DataValue(new Variant(false)));}
            if(d){req=(short)Math.max(1,cur[0]-1); down.setValue(new DataValue(new Variant(false)));}
            req=(short)Math.max(1,Math.min(pages,req));
            cur[0]=req;
            page.setValue(new DataValue(new Variant(req)));

            int offset=(req-1)*5;
            for(int i=0;i<5;i++){
                int idx=offset+i;
                ZES_opcUaWorkItem w= idx<items.size()?items.get(idx):new ZES_opcUaWorkItem("","","","","",(short)0);
                serial[i].setValue(new DataValue(new Variant(w.serial_code())));
                pname[i].setValue(new DataValue(new Variant(w.product_name())));
                target[i].setValue(new DataValue(new Variant(w.target_goal())));
                process[i].setValue(new DataValue(new Variant(w.process())));
                deadline[i].setValue(new DataValue(new Variant(w.deadline())));
            }
        },0,500, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(sch::shutdownNow));
    }

    private void add(NodeManager<UaNode> nm, OpcUaServer s, UaFolderNode root, UaVariableNode n){ nm.addNode(n); nm.addReferences(new Reference(root.getNodeId(), Identifiers.Organizes, n.getNodeId().expanded(), true), s.getNamespaceTable()); }
    private UaVariableNode roString(UaNodeContext c,UShort n,String id,String b,String v){ UaVariableNode x= UaVariableNode.builder(c).setNodeId(new NodeId(n,id)).setBrowseName(new QualifiedName(n,b)).setDisplayName(LocalizedText.english(b)).setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType).build(); x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY)); x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY)); x.setValue(new DataValue(new Variant(v))); return x; }
    private UaVariableNode rwString(UaNodeContext c,UShort n,String id,String b,String v){ UaVariableNode x= roString(c,n,id,b,v); x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE)); x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE)); return x; }
    private UaVariableNode roInt16(UaNodeContext c,UShort n,String id,String b,short v){ UaVariableNode x= UaVariableNode.builder(c).setNodeId(new NodeId(n,id)).setBrowseName(new QualifiedName(n,b)).setDisplayName(LocalizedText.english(b)).setDataType(Identifiers.Int16).setTypeDefinition(Identifiers.BaseDataVariableType).build(); x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY)); x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY)); x.setValue(new DataValue(new Variant(v))); return x; }
    private UaVariableNode rwInt16(UaNodeContext c,UShort n,String id,String b,short v){ UaVariableNode x= roInt16(c,n,id,b,v); x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE)); x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE)); return x; }
    private UaVariableNode rwBool(UaNodeContext c,UShort n,String id,String b,boolean v){ UaVariableNode x= UaVariableNode.builder(c).setNodeId(new NodeId(n,id)).setBrowseName(new QualifiedName(n,b)).setDisplayName(LocalizedText.english(b)).setDataType(Identifiers.Boolean).setTypeDefinition(Identifiers.BaseDataVariableType).build(); x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE)); x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE)); x.setValue(new DataValue(new Variant(v))); return x; }
}
