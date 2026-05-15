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
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
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
public class ZES_opcUaServerRunner implements ApplicationRunner {
    private static final String APP_URI = "urn:lsexp2:test:opcua:server";
    private static final String BIND_IP = "192.168.89.2";
    private static final String BIND_ADDRESS = "0.0.0.0";
    private static final int ENDPOINT_PORT = 8624;
    private static final String ENDPOINT_PATH = "/lsexp2-test";
    private static final String ROOT_ENDPOINT_PATH = "/";

    private final ZES_opcUaWorkItemProvider ZES_gv_workItemProvider;
    public ZES_opcUaServerRunner(ZES_opcUaWorkItemProvider p){this.ZES_gv_workItemProvider=p;}

    @Override
    public void run(ApplicationArguments args) throws Exception {
        OpcUaServer s=createServer(); Runtime.getRuntime().addShutdownHook(new Thread(s::shutdown)); s.startup().get(); addDbBackedNodes(s);
        System.out.println("Discovery Endpoint: opc.tcp://"+BIND_IP+":"+ENDPOINT_PORT+ROOT_ENDPOINT_PATH);
        System.out.println("Service Endpoint: opc.tcp://"+BIND_IP+":"+ENDPOINT_PORT+ENDPOINT_PATH);
    }

    private OpcUaServer createServer(){
        EndpointConfiguration se=buildEndpoint(ENDPOINT_PATH), de=buildEndpoint(ROOT_ENDPOINT_PATH);
        var b= OpcUaServerConfig.builder().setEndpoints(Set.of(de,se)).setIdentityValidator(new AnonymousIdentityValidator())
                .setBuildInfo(new BuildInfo(APP_URI,"openai","LS eXP2 OPC UA Test Server",OpcUaServer.SDK_VERSION,"2.4.0", DateTime.now()));
        invokeIfPresent(b,"setBindPort",ENDPOINT_PORT); return new OpcUaServer(b.build());
    }
    private EndpointConfiguration buildEndpoint(String path){
        EndpointConfiguration.Builder b=EndpointConfiguration.newBuilder().setBindAddress(BIND_ADDRESS).setHostname(BIND_IP).setPath(path)
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY).setSecurityPolicy(SecurityPolicy.None).setSecurityMode(MessageSecurityMode.None);
        invokeIfPresent(b,"setBindPort",ENDPOINT_PORT); return b.build();
    }
    private void invokeIfPresent(Object t,String m,int v){try{Method md=t.getClass().getMethod(m,int.class);md.invoke(t,v);}catch(Exception ignored){}}

    private void addDbBackedNodes(OpcUaServer server){
        UaNode objects=server.getAddressSpaceManager().getManagedNode(Identifiers.ObjectsFolder).orElseThrow();
        UShort ns=objects.getNodeId().getNamespaceIndex(); UaNodeContext ctx=objects.getNodeContext();
        @SuppressWarnings("unchecked") NodeManager<UaNode> nm=(NodeManager<UaNode>) objects.getNodeManager();
        UaFolderNode root=new UaFolderNode(ctx,new NodeId(ns,"LS_EXP2"),new QualifiedName(ns,"LS_EXP2"),LocalizedText.english("LS_EXP2"));
        nm.addNode(root); nm.addReferences(new Reference(Identifiers.ObjectsFolder,Identifiers.Organizes,root.getNodeId().expanded(),true),server.getNamespaceTable());

        UaVariableNode ict=rwString(ctx,ns,"LS_EXP2/selectedIctNumber","selectedIctNumber","P0208258");
        UaVariableNode enter=rwBool(ctx,ns,"LS_EXP2/workOrderPageEnter","workOrderPageEnter",false);
        UaVariableNode page=rwInt16(ctx,ns,"LS_EXP2/workReportCurrentPage","workReportCurrentPage",(short)1);
        UaVariableNode plus=rwBool(ctx,ns,"LS_EXP2/workReportPagePlus","workReportPagePlus",false);
        UaVariableNode minus=rwBool(ctx,ns,"LS_EXP2/workReportPageMinus","workReportPageMinus",false);
        UaVariableNode totalPage=roInt16(ctx,ns,"LS_EXP2/workReportTotalPage","workReportTotalPage",(short)1);
        UaVariableNode selectedRow=rwInt16(ctx,ns,"LS_EXP2/selectedWorkOrderRow","selectedWorkOrderRow",(short)1);
        UaVariableNode serialCodeDetail=roString(ctx,ns,"LS_EXP2/serialCodeDetail","serialCodeDetail","");
        UaVariableNode processDetail=roString(ctx,ns,"LS_EXP2/processDetail","processDetail","");
        UaVariableNode targetGoalDetail=roInt16(ctx,ns,"LS_EXP2/targetGoalDetail","targetGoalDetail",(short)0);
        add(nm,server,root,ict);add(nm,server,root,enter);add(nm,server,root,page);add(nm,server,root,plus);add(nm,server,root,minus);add(nm,server,root,totalPage);
        add(nm,server,root,selectedRow);add(nm,server,root,serialCodeDetail);add(nm,server,root,processDetail);add(nm,server,root,targetGoalDetail);

        UaVariableNode[] serial=new UaVariableNode[5], pname=new UaVariableNode[5], target=new UaVariableNode[5], process=new UaVariableNode[5], deadline=new UaVariableNode[5];
        for(int i=0;i<5;i++){int r=i+1; serial[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/serial_code","serial_code_row"+r,""); pname[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/product_name","product_name_row"+r,"");
            target[i]=roInt16(ctx,ns,"LS_EXP2/row"+r+"/target_goal","target_goal_row"+r,(short)0); process[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/process","process_row"+r,""); deadline[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/deadline","deadline_row"+r,"");
            add(nm,server,root,serial[i]);add(nm,server,root,pname[i]);add(nm,server,root,target[i]);add(nm,server,root,process[i]);add(nm,server,root,deadline[i]);}

        ScheduledExecutorService sch= Executors.newSingleThreadScheduledExecutor(); final short[] cur={1}; final String[] lastIct={"P0208258"}; final boolean[] lastEnter={false};
        sch.scheduleAtFixedRate(()->{
            String ictNo=String.valueOf(ict.getValue().getValue().getValue());
            boolean enterNow=Boolean.TRUE.equals(enter.getValue().getValue().getValue());
            boolean enterEdge=!lastEnter[0] && enterNow;
            lastEnter[0]=enterNow;
            boolean ictChanged=!ictNo.equals(lastIct[0]);
            if(ictChanged){ lastIct[0]=ictNo; cur[0]=1; page.setValue(new DataValue(new Variant((short)1))); }
            if(enterEdge){ cur[0]=1; page.setValue(new DataValue(new Variant((short)1))); enter.setValue(new DataValue(new Variant(false))); }

            List<ZES_opcUaWorkItem> items=ZES_gv_workItemProvider.ZES_getWorkItemsByIctNumber(ictNo);
            short pages=(short)Math.max(1,(items.size()+4)/5); totalPage.setValue(new DataValue(new Variant(pages)));

            short req=((Number)page.getValue().getValue().getValue()).shortValue();
            boolean p=Boolean.TRUE.equals(plus.getValue().getValue().getValue()), m=Boolean.TRUE.equals(minus.getValue().getValue().getValue());
            if(p){req++; plus.setValue(new DataValue(new Variant(false)));}
            if(m){req--; minus.setValue(new DataValue(new Variant(false)));}
            req=(short)Math.max(1,Math.min(pages,req)); cur[0]=req; page.setValue(new DataValue(new Variant(req)));

            int offset=(req-1)*5; short sel=((Number)selectedRow.getValue().getValue().getValue()).shortValue(); if(sel<1)sel=1; if(sel>5)sel=5; selectedRow.setValue(new DataValue(new Variant(sel)));
            for(int i=0;i<5;i++){int idx=offset+i; ZES_opcUaWorkItem w=idx<items.size()?items.get(idx):new ZES_opcUaWorkItem("","","","","",(short)0);
                serial[i].setValue(new DataValue(new Variant(w.serial_code()))); pname[i].setValue(new DataValue(new Variant(w.product_name()))); target[i].setValue(new DataValue(new Variant(w.target_goal()))); process[i].setValue(new DataValue(new Variant(w.process()))); deadline[i].setValue(new DataValue(new Variant(w.deadline())));}
            int di=offset+(sel-1); ZES_opcUaWorkItem d=di<items.size()?items.get(di):new ZES_opcUaWorkItem("","","","","",(short)0);
            serialCodeDetail.setValue(new DataValue(new Variant(d.serial_code()))); processDetail.setValue(new DataValue(new Variant(d.process()))); targetGoalDetail.setValue(new DataValue(new Variant(d.target_goal())));

            System.out.println("[OPC-UA] polling cycle running... ict="+ictNo+", page="+req+"/"+pages+", selectedRow="+sel);
        },0,500, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(sch::shutdownNow));
    }

    private void add(NodeManager<UaNode> nm, OpcUaServer s, UaFolderNode root, UaVariableNode n){nm.addNode(n);nm.addReferences(new Reference(root.getNodeId(),Identifiers.Organizes,n.getNodeId().expanded(),true),s.getNamespaceTable());}
    private UaVariableNode roString(UaNodeContext c,UShort n,String id,String b,String v){UaVariableNode x= UaVariableNode.builder(c).setNodeId(new NodeId(n,id)).setBrowseName(new QualifiedName(n,b)).setDisplayName(LocalizedText.english(b)).setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType).build();x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY));x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY));x.setValue(new DataValue(new Variant(v)));return x;}
    private UaVariableNode rwString(UaNodeContext c,UShort n,String id,String b,String v){UaVariableNode x=roString(c,n,id,b,v);x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));return x;}
    private UaVariableNode roInt16(UaNodeContext c,UShort n,String id,String b,short v){UaVariableNode x= UaVariableNode.builder(c).setNodeId(new NodeId(n,id)).setBrowseName(new QualifiedName(n,b)).setDisplayName(LocalizedText.english(b)).setDataType(Identifiers.Int16).setTypeDefinition(Identifiers.BaseDataVariableType).build();x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY));x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY));x.setValue(new DataValue(new Variant(v)));return x;}
    private UaVariableNode rwInt16(UaNodeContext c,UShort n,String id,String b,short v){UaVariableNode x=roInt16(c,n,id,b,v);x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));return x;}
    private UaVariableNode rwBool(UaNodeContext c,UShort n,String id,String b,boolean v){UaVariableNode x= UaVariableNode.builder(c).setNodeId(new NodeId(n,id)).setBrowseName(new QualifiedName(n,b)).setDisplayName(LocalizedText.english(b)).setDataType(Identifiers.Boolean).setTypeDefinition(Identifiers.BaseDataVariableType).build();x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));x.setValue(new DataValue(new Variant(v)));return x;}
}
