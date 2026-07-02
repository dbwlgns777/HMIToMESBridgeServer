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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ZES_opcUaServerRunner implements ApplicationRunner {
    private static final boolean USE_PAGED_DB_FETCH = true;
    private static final short WORK_ITEMS_PAGE_SIZE = 5;
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

        UaVariableNode ict=rwInt32(ctx,ns,"LS_EXP2/selectedIctNumber","selectedIctNumber",0);
        UaVariableNode requestManage=rwInt16(ctx,ns,"LS_EXP2/request_manage","request_manage",(short)0);
        UaVariableNode enter=rwBool(ctx,ns,"LS_EXP2/workOrderPageEnter","workOrderPageEnter",false);
        UaVariableNode workStatus=rwInt16(ctx,ns,"LS_EXP2/workStatus","workStatus",(short)0);
        UaVariableNode workTime=roString(ctx,ns,"LS_EXP2/workTime","workTime","00:00:00");
        UaVariableNode pauseTime=roString(ctx,ns,"LS_EXP2/pauseTime","pauseTime","00:00:00");
        UaVariableNode page=rwInt16(ctx,ns,"LS_EXP2/workReportCurrentPage","workReportCurrentPage",(short)1);
        UaVariableNode plus=rwBool(ctx,ns,"LS_EXP2/workReportPagePlus","workReportPagePlus",false);
        UaVariableNode minus=rwBool(ctx,ns,"LS_EXP2/workReportPageMinus","workReportPageMinus",false);
        UaVariableNode totalPage=roInt16(ctx,ns,"LS_EXP2/workReportTotalPage","workReportTotalPage",(short)1);
        UaVariableNode selectedRow=rwInt16(ctx,ns,"LS_EXP2/selectedWorkOrderRow","selectedWorkOrderRow",(short)1);
        UaVariableNode serialCodeDetail=roString(ctx,ns,"LS_EXP2/serialCodeDetail","serialCodeDetail","");
        UaVariableNode productNameDetail=roString(ctx,ns,"LS_EXP2/productNameDetail","productNameDetail","");
        UaVariableNode workOrderCodeDetail=roString(ctx,ns,"LS_EXP2/workOrderCodeDetail","workOrderCodeDetail","");
        UaVariableNode processDetail=roString(ctx,ns,"LS_EXP2/processDetail","processDetail","");
        UaVariableNode processCodeDetail=roString(ctx,ns,"LS_EXP2/processCodeDetail","processCodeDetail","");
        UaVariableNode facilityName=roString(ctx,ns,"LS_EXP2/facility_name","facility_name","");
        UaVariableNode facilityCode=roString(ctx,ns,"LS_EXP2/facility_code","facility_code","");
        UaVariableNode processDefectCode=roString(ctx,ns,"LS_EXP2/process_defect_code","process_defect_code","");
        UaVariableNode processDefectName=roString(ctx,ns,"LS_EXP2/process_defect_name","process_defect_name","");
        UaVariableNode companyCode=roString(ctx,ns,"LS_EXP2/company_code","company_code","");
        UaVariableNode targetGoalDetail=roInt16(ctx,ns,"LS_EXP2/targetGoalDetail","targetGoalDetail",(short)0);
        add(nm,server,root,ict);add(nm,server,root,requestManage);add(nm,server,root,enter);add(nm,server,root,workStatus);add(nm,server,root,workTime);add(nm,server,root,pauseTime);add(nm,server,root,page);add(nm,server,root,plus);add(nm,server,root,minus);add(nm,server,root,totalPage);
        add(nm,server,root,selectedRow);add(nm,server,root,serialCodeDetail);add(nm,server,root,productNameDetail);add(nm,server,root,workOrderCodeDetail);add(nm,server,root,processDetail);add(nm,server,root,processCodeDetail);add(nm,server,root,facilityName);add(nm,server,root,facilityCode);add(nm,server,root,processDefectCode);add(nm,server,root,processDefectName);add(nm,server,root,companyCode);add(nm,server,root,targetGoalDetail);

        UaVariableNode[] serial=new UaVariableNode[5], pname=new UaVariableNode[5], target=new UaVariableNode[5], process=new UaVariableNode[5], deadline=new UaVariableNode[5], processCode=new UaVariableNode[5], workOrderCode=new UaVariableNode[5];
        for(int i=0;i<5;i++){int r=i+1; serial[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/serial_code","serial_code_row"+r,""); pname[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/product_name","product_name_row"+r,"");
            target[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/target_goal","target_goal_row"+r,""); process[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/process","process_row"+r,""); deadline[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/deadline","deadline_row"+r,"");
            processCode[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/process_code","process_code_row"+r,""); workOrderCode[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/workOrderCode","workOrderCode_row"+r,"");
            add(nm,server,root,serial[i]);add(nm,server,root,pname[i]);add(nm,server,root,target[i]);add(nm,server,root,process[i]);add(nm,server,root,deadline[i]);add(nm,server,root,processCode[i]);add(nm,server,root,workOrderCode[i]);}

        ScheduledExecutorService sch= Executors.newSingleThreadScheduledExecutor(); final short[] cur={1}; final short[] totalPages={1}; final String[] lastIct={""}; final String[] lastValidIct={""}; final boolean[] lastEnter={false}; final List<ZES_opcUaWorkItem>[] cachedItems=new List[]{List.of()}; final Map<Short, List<ZES_opcUaWorkItem>> pageCache=new HashMap<>(); final long[] workSeconds={0L}; final long[] pauseSeconds={0L}; final long[] lastTimerMillis={System.currentTimeMillis()}; final short[] activeWorkStatus={(short)0}; final boolean[] workStartCaptured={false}; final String[] workStartTime={"00:00:00"}; final String[] workEndTime={"00:00:00"}; final ZES_opcUaWorkItem[] selectedWorkItem={ZES_emptyWorkItem()};
        sch.scheduleAtFixedRate(()->{
            long timerNow=System.currentTimeMillis();
            short workStatusNow=ZES_readInt16Safe(workStatus);
            long elapsedSeconds=(timerNow-lastTimerMillis[0])/1000L;
            if(elapsedSeconds > 0){
                if(activeWorkStatus[0] == 1) workSeconds[0]+=elapsedSeconds;
                if(activeWorkStatus[0] == 2) pauseSeconds[0]+=elapsedSeconds;
                lastTimerMillis[0]+=elapsedSeconds*1000L;
            }
            if(workStatusNow != activeWorkStatus[0]){
                if(workStatusNow == 1 && !workStartCaptured[0]){
                    workStartTime[0]=ZES_formatCurrentTime();
                    workStartCaptured[0]=true;
                    System.out.println("[OPC-UA][WORK-START] workStatus=1, workStartTime="+workStartTime[0]);
                }
                if(workStatusNow == 3){
                    workEndTime[0]=ZES_formatCurrentTime();
                    ZES_opcUaWorkItem workEndItem=selectedWorkItem[0];
                    System.out.println("[OPC-UA][WORK-END-DEBUG] workStatus=3, workStartTime="+workStartTime[0]+", workEndTime="+workEndTime[0]
                            +", productCode="+workEndItem.product_code()
                            +", productName="+workEndItem.product_name()
                            +", serialCode="+workEndItem.serial_code()
                            +", processRow="+workEndItem.process_row()
                            +", workOrderCode="+workEndItem.work_order_code()
                            +", deadline="+workEndItem.deadline()
                            +", targetGoal="+workEndItem.target_goal()
                            +", facilityName="+workEndItem.facility_name()
                            +", facilityCode="+workEndItem.facility_code()
                            +", processDefectCode="+workEndItem.process_defect_code()
                            +", processDefectName="+workEndItem.process_defect_name()
                            +", companyCode="+workEndItem.company_code()
                            +", tagCompanyCode="+companyCode.getValue().getValue().getValue()
                            +", tagWorkOrderCodeDetail="+workOrderCodeDetail.getValue().getValue().getValue());
                    workStartTime[0]="00:00:00";
                    workEndTime[0]="00:00:00";
                    workStartCaptured[0]=false;
                    workStatus.setValue(new DataValue(new Variant((short)0)));
                    workStatusNow=0;
                    System.out.println("[OPC-UA][WORK-END-DEBUG] workStartTime and workEndTime reset to 00:00:00, workStatus reset to 0 after work end debug log");
                }
                activeWorkStatus[0]=workStatusNow;
                lastTimerMillis[0]=timerNow;
            }
            workTime.setValue(new DataValue(new Variant(ZES_formatElapsedTime(workSeconds[0]))));
            pauseTime.setValue(new DataValue(new Variant(ZES_formatElapsedTime(pauseSeconds[0]))));
            String ictRaw=ZES_readIctNumberSafe(ict);
            String ictNo=ZES_sanitizeIctNumber(ictRaw);
            System.out.println("[OPC-UA][ICT-TAG] rawType=" + (ict.getValue().getValue().getValue()==null?"null":ict.getValue().getValue().getValue().getClass().getName()) + ", raw=" + ictRaw + ", sanitized=" + ictNo);
            if (!ictNo.isEmpty()) {
                lastValidIct[0] = ictNo;
            }
            boolean enterNow=Boolean.TRUE.equals(enter.getValue().getValue().getValue());
            boolean enterEdge=!lastEnter[0] && enterNow;
            lastEnter[0]=enterNow;
            String queryIctRaw = lastValidIct[0];
            if (queryIctRaw.isEmpty()) {
                System.out.println("[OPC-UA][ICT-TAG] waiting for valid HMI ict_number input...");
                return;
            }
            String queryIct = ZES_normalizeIctNumberForDb(queryIctRaw);
            if (queryIct.isEmpty()) {
                System.out.println("[OPC-UA][ICT-TAG] waiting for normalized ict_number for DB select... raw="+queryIctRaw);
                return;
            }
            short requestManageNow=ZES_readInt16Safe(requestManage);
            boolean ictChanged=!queryIct.equals(lastIct[0]);
            if(ictChanged){ lastIct[0]=queryIct; cur[0]=1; totalPages[0]=1; pageCache.clear(); cachedItems[0]=List.of(); page.setValue(new DataValue(new Variant((short)1))); }
            if(enterEdge){ cur[0]=1; page.setValue(new DataValue(new Variant((short)1))); enter.setValue(new DataValue(new Variant(false))); }
            if(requestManageNow == 1){
                cur[0]=1;
                totalPages[0]=1;
                pageCache.clear();
                page.setValue(new DataValue(new Variant((short)1)));
                if(USE_PAGED_DB_FETCH){
                    ZES_opcUaWorkItemPage firstPage=ZES_gv_workItemProvider.ZES_getWorkItemsByIctNumber(queryIct, cur[0], WORK_ITEMS_PAGE_SIZE);
                    totalPages[0]=firstPage.totalPage();
                    cachedItems[0]=firstPage.items();
                    pageCache.put(cur[0], cachedItems[0]);
                } else {
                    cachedItems[0]=ZES_gv_workItemProvider.ZES_getWorkItemsByIctNumber(queryIct);
                    totalPages[0]=(short)Math.max(1,(cachedItems[0].size()+WORK_ITEMS_PAGE_SIZE-1)/WORK_ITEMS_PAGE_SIZE);
                }
                requestManage.setValue(new DataValue(new Variant((short)0)));
                System.out.println("[OPC-UA][REQUEST-MANAGE] request_manage=1, selectedIctNumber="+queryIct+", fetchedItems="+cachedItems[0].size()+", request_manage reset to 0");
            }

            short pages=USE_PAGED_DB_FETCH?totalPages[0]:(short)Math.max(1,(cachedItems[0].size()+WORK_ITEMS_PAGE_SIZE-1)/WORK_ITEMS_PAGE_SIZE); totalPage.setValue(new DataValue(new Variant(pages)));

            short req=((Number)page.getValue().getValue().getValue()).shortValue();
            boolean p=Boolean.TRUE.equals(plus.getValue().getValue().getValue()), m=Boolean.TRUE.equals(minus.getValue().getValue().getValue());
            if(p){req++; plus.setValue(new DataValue(new Variant(false)));}
            if(m){req--; minus.setValue(new DataValue(new Variant(false)));}
            req=(short)Math.max(1,Math.min(pages,req)); cur[0]=req; page.setValue(new DataValue(new Variant(req)));

            List<ZES_opcUaWorkItem> items;
            int offset;
            if(USE_PAGED_DB_FETCH){
                items=pageCache.get(req);
                if(items==null){
                    ZES_opcUaWorkItemPage requestedPage=ZES_gv_workItemProvider.ZES_getWorkItemsByIctNumber(queryIct, req, WORK_ITEMS_PAGE_SIZE);
                    totalPages[0]=requestedPage.totalPage();
                    pages=totalPages[0];
                    totalPage.setValue(new DataValue(new Variant(pages)));
                    items=requestedPage.items();
                    pageCache.put(req, items);
                    System.out.println("[OPC-UA][PAGE-FETCH] selectedIctNumber="+queryIct+", page="+req+"/"+pages+", fetchedItems="+items.size());
                }
                cachedItems[0]=items;
                offset=0;
            } else {
                items=cachedItems[0];
                offset=(req-1)*WORK_ITEMS_PAGE_SIZE;
            }
            short sel=((Number)selectedRow.getValue().getValue().getValue()).shortValue(); if(sel<1)sel=1; if(sel>WORK_ITEMS_PAGE_SIZE)sel=WORK_ITEMS_PAGE_SIZE; selectedRow.setValue(new DataValue(new Variant(sel)));
            for(int i=0;i<WORK_ITEMS_PAGE_SIZE;i++){int idx=offset+i; boolean hasItem=idx<items.size(); ZES_opcUaWorkItem w=hasItem?items.get(idx):ZES_emptyWorkItem();
                serial[i].setValue(new DataValue(new Variant(w.serial_code()))); pname[i].setValue(new DataValue(new Variant(w.product_name()))); target[i].setValue(new DataValue(new Variant(hasItem?String.valueOf(w.target_goal()):""))); process[i].setValue(new DataValue(new Variant(w.process_row()))); deadline[i].setValue(new DataValue(new Variant(w.deadline()))); processCode[i].setValue(new DataValue(new Variant(w.process_row()))); workOrderCode[i].setValue(new DataValue(new Variant(w.work_order_code())));}
            int di=offset+(sel-1); ZES_opcUaWorkItem d=di<items.size()?items.get(di):ZES_emptyWorkItem();
            selectedWorkItem[0]=d;
            serialCodeDetail.setValue(new DataValue(new Variant(d.serial_code()))); productNameDetail.setValue(new DataValue(new Variant(d.product_name()))); workOrderCodeDetail.setValue(new DataValue(new Variant(d.work_order_code()))); processDetail.setValue(new DataValue(new Variant(d.process_row()))); processCodeDetail.setValue(new DataValue(new Variant(d.process_row()))); facilityName.setValue(new DataValue(new Variant(d.facility_name()))); facilityCode.setValue(new DataValue(new Variant(d.facility_code()))); processDefectCode.setValue(new DataValue(new Variant(d.process_defect_code()))); processDefectName.setValue(new DataValue(new Variant(d.process_defect_name()))); companyCode.setValue(new DataValue(new Variant(d.company_code()))); targetGoalDetail.setValue(new DataValue(new Variant(d.target_goal())));

            System.out.println("[OPC-UA][DB-RESULT] itemCount="+items.size()+", queryIct="+queryIct+", page="+req+", selectedRow="+sel);
            for(int i=0;i<5;i++){
                Object serialTagVal=serial[i].getValue().getValue().getValue();
                Object pnameTagVal=pname[i].getValue().getValue().getValue();
                Object targetTagVal=target[i].getValue().getValue().getValue();
                Object processTagVal=process[i].getValue().getValue().getValue();
                Object deadlineTagVal=deadline[i].getValue().getValue().getValue();
                int row=i+1;
                System.out.println("[OPC-UA][WORKITEM-TAG] row"+row+"_serialCode="+serialTagVal+", row"+row+"_productName="+pnameTagVal+", row"+row+"_targetGoal="+targetTagVal+", row"+row+"_process="+processTagVal+", row"+row+"_deadline="+deadlineTagVal);
            }
            System.out.println("[OPC-UA][WORKITEM-DETAIL-TAG] serialCodeDetail="+serialCodeDetail.getValue().getValue().getValue()+", productNameDetail="+productNameDetail.getValue().getValue().getValue()+", workOrderCodeDetail="+workOrderCodeDetail.getValue().getValue().getValue()+", processDetail="+processDetail.getValue().getValue().getValue()+", targetGoalDetail="+targetGoalDetail.getValue().getValue().getValue());

            System.out.println("[OPC-UA] polling cycle running... ict="+queryIct+", page="+req+"/"+pages+", selectedRow="+sel);
        },0,500, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(sch::shutdownNow));
    }


    private ZES_opcUaWorkItem ZES_emptyWorkItem()
    {
        return new ZES_opcUaWorkItem("", "", "", "", "", "", "", "", "", "", "", "");
    }

    private String ZES_formatElapsedTime(long totalSeconds)
    {
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String ZES_formatCurrentTime()
    {
        return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private short ZES_readInt16Safe(UaVariableNode node)
    {
        Object raw = node.getValue().getValue().getValue();
        if (raw instanceof Number number) return number.shortValue();
        if (raw == null) return 0;
        try { return Short.parseShort(String.valueOf(raw).trim()); } catch (Exception e) { return 0; }
    }

    private String ZES_readIctNumberSafe(UaVariableNode ictNode)
    {
        Object raw = ictNode.getValue().getValue().getValue();
        if (raw == null) return "";
        if (raw instanceof String str) return str.trim();
        if (raw instanceof byte[] b) return new String(b).trim();
        if (raw instanceof ByteString bs && bs.bytes() != null) return new String(bs.bytes()).trim();
        return String.valueOf(raw).trim();
    }

    private String ZES_sanitizeIctNumber(String raw)
    {
        if (raw == null) return "";
        String v = raw.replace("\u0000", "").trim();
        if (v.isEmpty()) return "";
        if (!v.matches("[A-Za-z0-9_-]+")) {
            System.out.println("[OPC-UA][ICT-TAG] invalid ict_number format from HMI: '" + v + "'");
            return "";
        }
        return v;
    }

    private String ZES_normalizeIctNumberForDb(String ictRaw)
    {
        if (ictRaw == null) return "";
        String v = ictRaw.trim();
        if (v.isEmpty()) return "";
        if (v.matches("^\\d{6}$")) return "P0" + v;
        if (v.matches("^\\d{7}$")) return "P" + v;
        return v;
    }

    private void add(NodeManager<UaNode> nm, OpcUaServer s, UaFolderNode root, UaVariableNode n){nm.addNode(n);nm.addReferences(new Reference(root.getNodeId(),Identifiers.Organizes,n.getNodeId().expanded(),true),s.getNamespaceTable());}
    private UaVariableNode roString(UaNodeContext c,UShort n,String id,String b,String v){UaVariableNode x= UaVariableNode.builder(c).setNodeId(new NodeId(n,id)).setBrowseName(new QualifiedName(n,b)).setDisplayName(LocalizedText.english(b)).setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType).build();x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY));x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY));x.setValue(new DataValue(new Variant(v)));return x;}
    private UaVariableNode rwString(UaNodeContext c,UShort n,String id,String b,String v){UaVariableNode x=roString(c,n,id,b,v);x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));return x;}
    private UaVariableNode roInt16(UaNodeContext c,UShort n,String id,String b,short v){UaVariableNode x= UaVariableNode.builder(c).setNodeId(new NodeId(n,id)).setBrowseName(new QualifiedName(n,b)).setDisplayName(LocalizedText.english(b)).setDataType(Identifiers.Int16).setTypeDefinition(Identifiers.BaseDataVariableType).build();x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY));x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY));x.setValue(new DataValue(new Variant(v)));return x;}
    private UaVariableNode rwInt16(UaNodeContext c,UShort n,String id,String b,short v){UaVariableNode x=roInt16(c,n,id,b,v);x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));return x;}
    private UaVariableNode roInt32(UaNodeContext c,UShort n,String id,String b,int v){UaVariableNode x= UaVariableNode.builder(c).setNodeId(new NodeId(n,id)).setBrowseName(new QualifiedName(n,b)).setDisplayName(LocalizedText.english(b)).setDataType(Identifiers.Int32).setTypeDefinition(Identifiers.BaseDataVariableType).build();x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY));x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_ONLY));x.setValue(new DataValue(new Variant(v)));return x;}
    private UaVariableNode rwInt32(UaNodeContext c,UShort n,String id,String b,int v){UaVariableNode x=roInt32(c,n,id,b,v);x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));return x;}
    private UaVariableNode rwBool(UaNodeContext c,UShort n,String id,String b,boolean v){UaVariableNode x= UaVariableNode.builder(c).setNodeId(new NodeId(n,id)).setBrowseName(new QualifiedName(n,b)).setDisplayName(LocalizedText.english(b)).setDataType(Identifiers.Boolean).setTypeDefinition(Identifiers.BaseDataVariableType).build();x.setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));x.setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE));x.setValue(new DataValue(new Variant(v)));return x;}
}