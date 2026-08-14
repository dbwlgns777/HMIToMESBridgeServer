package com.zes.hmitomesbridgeserver.opcua;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    private static final String WORK_HISTORY_REGISTER_URL = "https://api.z-fas.com:5500/api/v1/workHistory/kiosk/register";
    private static final String WORK_HISTORY_UPDATE_URL = "https://api.z-fas.com:5500/api/v1/workHistory/kiosk/update";
    private static final String WORK_HISTORY_INPUT_MATERIAL_LOT_AUTO_PROCESS_URL = "https://api.z-fas.com:5500/api/v1/workHistory/inputMaterial/lot/auto/process";
    private static final HttpClient WORK_HISTORY_HTTP_CLIENT = HttpClient.newHttpClient();

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
        UaVariableNode resultTag=rwInt16(ctx,ns,"LS_EXP2/ResultTag","ResultTag",(short)0);
        UaVariableNode enter=rwBool(ctx,ns,"LS_EXP2/workOrderPageEnter","workOrderPageEnter",false);
        UaVariableNode workMode=rwInt16(ctx,ns,"LS_EXP2/workMode","workMode",(short)0);
        UaVariableNode workStatus=rwInt16(ctx,ns,"LS_EXP2/workStatus","workStatus",(short)0);
        UaVariableNode workTime=roString(ctx,ns,"LS_EXP2/workTime","workTime","00:00:00");
        UaVariableNode pauseTime=roString(ctx,ns,"LS_EXP2/pauseTime","pauseTime","00:00:00");
        UaVariableNode goodQuantity=rwInt32(ctx,ns,"LS_EXP2/goodQuantity","goodQuantity",0);
        UaVariableNode totalDefectiveQuantity=rwInt32(ctx,ns,"LS_EXP2/totalDefectiveQuantity","totalDefectiveQuantity",0);
        UaVariableNode totalPauseTime=rwString(ctx,ns,"LS_EXP2/totalPauseTime","totalPauseTime","00:00:00");
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
        add(nm,server,root,ict);add(nm,server,root,requestManage);add(nm,server,root,enter);add(nm,server,root,workMode);add(nm,server,root,workStatus);add(nm,server,root,workTime);add(nm,server,root,pauseTime);add(nm,server,root,goodQuantity);add(nm,server,root,totalDefectiveQuantity);add(nm,server,root,totalPauseTime);add(nm,server,root,page);add(nm,server,root,plus);add(nm,server,root,minus);add(nm,server,root,totalPage);
        add(nm,server,root,selectedRow);add(nm,server,root,serialCodeDetail);add(nm,server,root,productNameDetail);add(nm,server,root,workOrderCodeDetail);add(nm,server,root,processDetail);add(nm,server,root,processCodeDetail);add(nm,server,root,facilityName);add(nm,server,root,facilityCode);add(nm,server,root,processDefectCode);add(nm,server,root,processDefectName);add(nm,server,root,companyCode);add(nm,server,root,targetGoalDetail);

        UaVariableNode[] serial=new UaVariableNode[5], pname=new UaVariableNode[5], target=new UaVariableNode[5], process=new UaVariableNode[5], deadline=new UaVariableNode[5], processCode=new UaVariableNode[5], workOrderCode=new UaVariableNode[5];
        for(int i=0;i<5;i++){int r=i+1; serial[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/serial_code","serial_code_row"+r,""); pname[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/product_name","product_name_row"+r,"");
            target[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/target_goal","target_goal_row"+r,""); process[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/process","process_row"+r,""); deadline[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/deadline","deadline_row"+r,"");
            processCode[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/process_code","process_code_row"+r,""); workOrderCode[i]=roString(ctx,ns,"LS_EXP2/row"+r+"/workOrderCode","workOrderCode_row"+r,"");
            add(nm,server,root,serial[i]);add(nm,server,root,pname[i]);add(nm,server,root,target[i]);add(nm,server,root,process[i]);add(nm,server,root,deadline[i]);add(nm,server,root,processCode[i]);add(nm,server,root,workOrderCode[i]);}

        ScheduledExecutorService sch= Executors.newSingleThreadScheduledExecutor(); final short[] cur={1}; final short[] totalPages={1}; final String[] lastIct={""}; final String[] lastValidIct={""}; final boolean[] lastEnter={false}; final boolean[] workItemsLoaded={false}; final List<ZES_opcUaWorkItem>[] cachedItems=new List[]{List.of()}; final Map<Short, List<ZES_opcUaWorkItem>> pageCache=new HashMap<>(); final long[] workSeconds={0L}; final long[] pauseSeconds={0L}; final long[] lastTimerMillis={System.currentTimeMillis()}; final short[] activeWorkMode={(short)0}; final short[] lastWorkModeCommand={(short)0}; final boolean[] forceWorkModeReset={false}; final boolean[] workStartCaptured={false}; final String[] workStartTime={"0000-00-00 00:00:00"}; final String[] workEndTime={"0000-00-00 00:00:00"}; final ZES_opcUaWorkItem[] selectedWorkItem={ZES_emptyWorkItem()}; final JSONObject[] registerResponse={null}; final String[] activeWorkHistoryCode={""}; final String[] activeCompanyCode={""};
        sch.scheduleAtFixedRate(()->{
            try {
            long timerNow=System.currentTimeMillis();
            short workModeNow=ZES_readInt16Safe(workMode);
            if(forceWorkModeReset[0]){
                boolean resetAcknowledged=workModeNow == 0;
                workModeNow=0;
                workMode.setValue(new DataValue(new Variant((short)0)));
                lastWorkModeCommand[0]=0;
                if(resetAcknowledged){
                    forceWorkModeReset[0]=false;
                    System.out.println("[OPC-UA][WORK-MODE-RESET] HMI observed workMode=0; reset latch released");
                }
            }
            // Apply pause/resume mode before attributing elapsed time. Otherwise the
            // first polling interval after 1 -> 2 is incorrectly counted as work.
            if(workStartCaptured[0] && workModeNow == 1) activeWorkMode[0]=1;
            if(workStartCaptured[0] && workModeNow == 2) activeWorkMode[0]=2;
            long elapsedSeconds=(timerNow-lastTimerMillis[0])/1000L;
            if(elapsedSeconds > 0){
                if(activeWorkMode[0] == 1) workSeconds[0]+=elapsedSeconds;
                if(activeWorkMode[0] == 2) pauseSeconds[0]+=elapsedSeconds;
                lastTimerMillis[0]+=elapsedSeconds*1000L;
            }
            if(workModeNow == 0){
                lastWorkModeCommand[0]=0;
            } else if(workModeNow != lastWorkModeCommand[0]){
                if(workModeNow == 1 && !workStartCaptured[0]){
                    ZES_opcUaWorkItem workStartItem=selectedWorkItem[0];
                    ZES_workHistoryState existingHistory=ZES_gv_workItemProvider.ZES_getLatestActiveWorkHistory(workStartItem.work_order_code());
                    if(existingHistory != null && "working".equalsIgnoreCase(existingHistory.workStatement())){
                        ZES_restoreWorkingHistory(existingHistory, workStartItem, workStatus, workTime, serialCodeDetail, productNameDetail, workOrderCodeDetail, processDetail, processCodeDetail, facilityName, facilityCode, processDefectCode, processDefectName, companyCode, targetGoalDetail, selectedWorkItem, workSeconds, pauseSeconds, workStartTime, workStartCaptured, lastTimerMillis);
                        activeWorkHistoryCode[0]=existingHistory.workHistoryCode();
                        activeCompanyCode[0]=existingHistory.companyCode();
                        registerResponse[0]=ZES_buildRestoredRegisterResponse(existingHistory);
                        workStatus.setValue(new DataValue(new Variant((short)1)));
                        activeWorkMode[0]=1;
                        System.out.println("[OPC-UA][WORK-START-SKIP] existing working history, workStatus=1, workHistoryCode="+activeWorkHistoryCode[0]);
                    } else if(workStartItem.work_order_code().isBlank()){
                        workStatus.setValue(new DataValue(new Variant((short)0)));
                        System.out.println("[OPC-UA][WORK-START-SKIP] selected work order is empty");
                    } else {
                        workStartTime[0]=ZES_formatCurrentTime();
                        JSONObject startPayload=ZES_buildWorkStartPayload(companyCode, facilityCode, workOrderCodeDetail, workStartTime[0]);
                        JSONObject response=ZES_sendWorkHistoryPayload(WORK_HISTORY_REGISTER_URL, "REGISTER", startPayload);
                        ZES_workHistoryState registeredState=ZES_extractRegisteredWorkHistory(response, workStartItem.work_order_code(), ZES_readNodeValueAsString(companyCode), workStartTime[0]);
                        if(registeredState == null){
                            workStatus.setValue(new DataValue(new Variant((short)0)));
                            System.out.println("[OPC-UA][WORK-START-FAIL] register response did not contain workHistoryCode");
                        } else {
                            registerResponse[0]=response;
                            activeWorkHistoryCode[0]=registeredState.workHistoryCode();
                            activeCompanyCode[0]=registeredState.companyCode();
                            workStartCaptured[0]=true;
                            activeWorkMode[0]=1;
                            workStatus.setValue(new DataValue(new Variant((short)0)));
                            System.out.println("[OPC-UA][WORK-START] workMode=1, workStatus=0, workStartTime="+workStartTime[0]+", workHistoryCode="+activeWorkHistoryCode[0]);
                        }
                    }
                }
                if(workModeNow == 1 && workStartCaptured[0]) activeWorkMode[0]=1;
                if(workModeNow == 2 && workStartCaptured[0]) activeWorkMode[0]=2;
                if(workModeNow == 3){
                    boolean switchedToOtherWorking=false;
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
                            +", tagWorkOrderCodeDetail="+workOrderCodeDetail.getValue().getValue().getValue()
                            +", goodQuantity="+goodQuantity.getValue().getValue().getValue()
                            +", totalDefectiveQuantity="+totalDefectiveQuantity.getValue().getValue().getValue()
                            +", totalPauseTime="+totalPauseTime.getValue().getValue().getValue());
                    ZES_workHistoryState currentHistory=ZES_gv_workItemProvider.ZES_getActiveWorkHistory(activeWorkHistoryCode[0]);
                    if(currentHistory != null && "working".equalsIgnoreCase(currentHistory.workStatement())){
                    boolean updateSucceeded=ZES_sendWorkHistoryUpdateIfRegisterSuccess(
                            registerResponse[0],
                            companyCode,
                            goodQuantity,
                            totalDefectiveQuantity,
                            facilityCode,
                            processDefectName,
                            processDefectCode,
                            pauseTime,
                            workEndItem.product_code(),
                            workStartTime[0],
                            workEndTime[0]
                    );
                    if(updateSucceeded){
                        ZES_workHistoryState updatedHistory=ZES_gv_workItemProvider.ZES_getActiveWorkHistory(activeWorkHistoryCode[0]);
                        ZES_workHistoryState otherWorkingHistory=ZES_gv_workItemProvider.ZES_getOtherWorkingHistory(activeCompanyCode[0], activeWorkHistoryCode[0]);
                        boolean currentHistoryStillWorking=updatedHistory != null && "working".equalsIgnoreCase(updatedHistory.workStatement());
                        if(!currentHistoryStillWorking && otherWorkingHistory == null){
                            registerResponse[0]=null;
                            activeWorkHistoryCode[0]="";
                            activeCompanyCode[0]="";
                            workStartTime[0]="0000-00-00 00:00:00";
                            workEndTime[0]="0000-00-00 00:00:00";
                            workSeconds[0]=0L;
                            pauseSeconds[0]=0L;
                            workStartCaptured[0]=false;
                            workTime.setValue(new DataValue(new Variant("00:00:00")));
                            pauseTime.setValue(new DataValue(new Variant("00:00:00")));
                            workStatus.setValue(new DataValue(new Variant((short)0)));
                            workModeNow=0;
                            workMode.setValue(new DataValue(new Variant((short)0)));
                            forceWorkModeReset[0]=true;
                            activeWorkMode[0]=0;
                            System.out.println("[OPC-UA][WORK-END] update succeeded and no working history remains, workStatus=0, workMode=0");
                        } else {
                            System.out.println("[OPC-UA][WORK-END] update succeeded but a working history remains, workStatus was not reset");
                        }
                    } else {
                        System.out.println("[OPC-UA][WORK-END] update failed or was skipped, runtime state and workStatus were not reset");
                    }
                    } else {
                        ZES_workHistoryState otherHistory=ZES_gv_workItemProvider.ZES_getOtherWorkingHistory(
                                activeCompanyCode[0].isBlank() && currentHistory != null?currentHistory.companyCode():activeCompanyCode[0],
                                activeWorkHistoryCode[0]);
                        if(otherHistory == null){
                            workStatus.setValue(new DataValue(new Variant((short)2)));
                            activeWorkMode[0]=0;
                            System.out.println("[OPC-UA][WORK-END-SKIP] current history ended and no other working history, workStatus=2");
                        } else {
                            ZES_opcUaWorkItem otherItem=ZES_findWorkItem(pageCache, otherHistory.workOrderCode());
                            if(otherItem.work_order_code().isBlank()){
                                ZES_opcUaWorkItem dbItem=ZES_gv_workItemProvider.ZES_getWorkItemByWorkOrderCode(otherHistory.workOrderCode());
                                if(dbItem != null) otherItem=dbItem;
                            }
                            if(otherItem.work_order_code().isBlank()){
                                workStatus.setValue(new DataValue(new Variant((short)2)));
                                activeWorkMode[0]=0;
                                System.out.println("[OPC-UA][WORK-END-SKIP] other working item details not found, workStatus=2");
                            } else {
                                pauseSeconds[0]=0L;
                                pauseTime.setValue(new DataValue(new Variant("00:00:00")));
                                ZES_restoreWorkingHistory(otherHistory, otherItem, workStatus, workTime, serialCodeDetail, productNameDetail, workOrderCodeDetail, processDetail, processCodeDetail, facilityName, facilityCode, processDefectCode, processDefectName, companyCode, targetGoalDetail, selectedWorkItem, workSeconds, pauseSeconds, workStartTime, workStartCaptured, lastTimerMillis);
                                activeWorkHistoryCode[0]=otherHistory.workHistoryCode();
                                activeCompanyCode[0]=otherHistory.companyCode();
                                registerResponse[0]=ZES_buildRestoredRegisterResponse(otherHistory);
                                activeWorkMode[0]=1;
                                workStatus.setValue(new DataValue(new Variant((short)3)));
                                switchedToOtherWorking=true;
                                System.out.println("[OPC-UA][WORK-HISTORY-SWITCH] switched to other working history, workStatus=3, workHistoryCode="+otherHistory.workHistoryCode());
                            }
                        }
                    }
                    workModeNow=switchedToOtherWorking?(short)1:(short)0;
                    workMode.setValue(new DataValue(new Variant(workModeNow)));
                }
                lastWorkModeCommand[0]=workModeNow;
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
            boolean requestManageTriggered=requestManageNow == 1;
            List<ZES_opcUaWorkItem> requestManageItems=new ArrayList<>();
            boolean ictChanged=!queryIct.equals(lastIct[0]);
            if(ictChanged){ lastIct[0]=queryIct; cur[0]=1; totalPages[0]=1; workItemsLoaded[0]=false; pageCache.clear(); cachedItems[0]=List.of(); page.setValue(new DataValue(new Variant((short)1))); }
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
                    requestManageItems.addAll(cachedItems[0]);
                    for(short requestedPage=2;requestedPage<=totalPages[0];requestedPage++){
                        ZES_opcUaWorkItemPage additionalPage=ZES_gv_workItemProvider.ZES_getWorkItemsByIctNumber(queryIct, requestedPage, WORK_ITEMS_PAGE_SIZE);
                        pageCache.put(requestedPage, additionalPage.items());
                        requestManageItems.addAll(additionalPage.items());
                    }
                } else {
                    cachedItems[0]=ZES_gv_workItemProvider.ZES_getWorkItemsByIctNumber(queryIct);
                    totalPages[0]=(short)Math.max(1,(cachedItems[0].size()+WORK_ITEMS_PAGE_SIZE-1)/WORK_ITEMS_PAGE_SIZE);
                    requestManageItems.addAll(cachedItems[0]);
                }
                requestManage.setValue(new DataValue(new Variant((short)0)));
                workItemsLoaded[0]=true;
                System.out.println("[OPC-UA][REQUEST-MANAGE] request_manage=1, selectedIctNumber="+queryIct+", totalFetchedItems="+requestManageItems.size()+", totalPages="+totalPages[0]+", request_manage reset to 0");
            }

            if(!workItemsLoaded[0]){
                totalPage.setValue(new DataValue(new Variant((short)1)));
                return;
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
            boolean activeWorkItemLocked=!activeWorkHistoryCode[0].isBlank()
                    && !selectedWorkItem[0].work_order_code().isBlank();
            if(!activeWorkItemLocked){
                selectedWorkItem[0]=d;
                ZES_setWorkItemDetailTags(d, serialCodeDetail, productNameDetail, workOrderCodeDetail, processDetail, processCodeDetail, facilityName, facilityCode, processDefectCode, processDefectName, companyCode, targetGoalDetail);
            }
            if(requestManageTriggered){
                boolean workingHistoryRestored=false;
                for(int itemIndex=0;itemIndex<requestManageItems.size();itemIndex++){
                    ZES_opcUaWorkItem requestedItem=requestManageItems.get(itemIndex);
                    ZES_workHistoryState history=ZES_gv_workItemProvider.ZES_getLatestActiveWorkHistory(requestedItem.work_order_code());
                    System.out.println("[OPC-UA][WORK-HISTORY-CHECK] item="+(itemIndex+1)+"/"+requestManageItems.size()+", workOrderCode="+requestedItem.work_order_code()+", workStatement="+(history==null?"":history.workStatement())+", startTime="+(history==null?"":history.startTime()));
                    if(history == null || !"working".equalsIgnoreCase(history.workStatement())) continue;
                    LocalDateTime startTime=ZES_parseWorkStartTime(history.startTime());
                    if(startTime != null){
                        short restoredWorkMode=ZES_readInt16Safe(workMode);
                        if(restoredWorkMode == 0){
                            pauseSeconds[0]=0L;
                            pauseTime.setValue(new DataValue(new Variant("00:00:00")));
                            workMode.setValue(new DataValue(new Variant((short)1)));
                            restoredWorkMode=1;
                        }
                        long restoreTimerMillis=System.currentTimeMillis();
                        long restoreElapsedSeconds=(restoreTimerMillis-lastTimerMillis[0])/1000L;
                        if(restoreElapsedSeconds > 0 && restoredWorkMode == 2){
                            pauseSeconds[0]+=restoreElapsedSeconds;
                        }
                        LocalDateTime now=LocalDateTime.now();
                        long totalElapsedSeconds=Math.max(0L, ChronoUnit.SECONDS.between(startTime, now));
                        workSeconds[0]=Math.max(0L, totalElapsedSeconds-pauseSeconds[0]);
                        workStartTime[0]=startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        workStartCaptured[0]=true;
                        activeWorkMode[0]=restoredWorkMode == 2?(short)2:(short)1;
                        lastTimerMillis[0]=restoreTimerMillis;
                        workStatus.setValue(new DataValue(new Variant((short)1)));
                        workTime.setValue(new DataValue(new Variant(ZES_formatElapsedTime(workSeconds[0]))));
                        selectedWorkItem[0]=requestedItem;
                        ZES_setWorkItemDetailTags(requestedItem, serialCodeDetail, productNameDetail, workOrderCodeDetail, processDetail, processCodeDetail, facilityName, facilityCode, processDefectCode, processDefectName, companyCode, targetGoalDetail);
                        activeWorkHistoryCode[0]=history.workHistoryCode();
                        activeCompanyCode[0]=history.companyCode();
                        registerResponse[0]=ZES_buildRestoredRegisterResponse(history);
                        System.out.println("[OPC-UA][WORK-HISTORY-RESTORE] workOrderCode="+requestedItem.work_order_code()+", serialCode="+requestedItem.serial_code()+", productName="+requestedItem.product_name()+", startTime="+workStartTime[0]+", totalElapsedSeconds="+totalElapsedSeconds+", pauseSeconds="+pauseSeconds[0]+", workTime="+ZES_formatElapsedTime(workSeconds[0])+", workStatus=1, workMode="+restoredWorkMode);
                        workingHistoryRestored=true;
                        break;
                    }
                }
                if(!workingHistoryRestored){
                    workStatus.setValue(new DataValue(new Variant((short)0)));
                    workMode.setValue(new DataValue(new Variant((short)0)));
                    forceWorkModeReset[0]=true;
                    activeWorkMode[0]=0;
                    lastWorkModeCommand[0]=0;
                    workStartCaptured[0]=false;
                    workStartTime[0]="0000-00-00 00:00:00";
                    workSeconds[0]=0L;
                    lastTimerMillis[0]=System.currentTimeMillis();
                    workTime.setValue(new DataValue(new Variant("00:00:00")));
                    System.out.println("[OPC-UA][WORK-HISTORY-RESTORE] no working history found, workStatus=0, workMode=0, workTime=00:00:00");
                }
            }

            System.out.println("[OPC-UA][DB-RESULT] itemCount="+items.size()+", queryIct="+queryIct+", page="+req+", selectedRow="+sel);
            for(int i=0;i<5;i++){
                Object serialTagVal=serial[i].getValue().getValue().getValue();
                Object pnameTagVal=pname[i].getValue().getValue().getValue();
                Object targetTagVal=target[i].getValue().getValue().getValue();
                Object processTagVal=process[i].getValue().getValue().getValue();
                Object deadlineTagVal=deadline[i].getValue().getValue().getValue();
                Object workOrderCodeTagVal=workOrderCode[i].getValue().getValue().getValue();
                int row=i+1;
                System.out.println("[OPC-UA][WORKITEM-TAG] row"+row+"_serialCode="+serialTagVal+", row"+row+"_productName="+pnameTagVal+", row"+row+"_targetGoal="+targetTagVal+", row"+row+"_process="+processTagVal+", row"+row+"_deadline="+deadlineTagVal+", row"+row+"_workOrderCode="+workOrderCodeTagVal);
            }
            System.out.println("[OPC-UA][WORKITEM-DETAIL-TAG] serialCodeDetail="+serialCodeDetail.getValue().getValue().getValue()+", productNameDetail="+productNameDetail.getValue().getValue().getValue()+", workOrderCodeDetail="+workOrderCodeDetail.getValue().getValue().getValue()+", processDetail="+processDetail.getValue().getValue().getValue()+", targetGoalDetail="+targetGoalDetail.getValue().getValue().getValue());

            System.out.println("[OPC-UA] polling cycle running... ict="+queryIct+", page="+req+"/"+pages+", selectedRow="+sel);
            } catch (Exception e) {
                System.out.println("[OPC-UA][POLLING-ERROR] polling cycle failed but scheduler will continue: "+e.getMessage());
                e.printStackTrace(System.out);
                if(ZES_readInt16Safe(requestManage) == 1){
                    requestManage.setValue(new DataValue(new Variant((short)0)));
                    System.out.println("[OPC-UA][POLLING-ERROR] request_manage reset to 0 after failed DB request");
                }
            }
        },0,500, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(sch::shutdownNow));
    }


    private JSONObject ZES_buildWorkStartPayload(
            UaVariableNode companyCode,
            UaVariableNode facilityCode,
            UaVariableNode workOrderCodeDetail,
            String workStartTime
    )
    {
        JSONObject workEndPayload=new JSONObject(true);
        workEndPayload.put("companyCode", ZES_readNodeValueAsString(companyCode));

        JSONArray facilityCodeArray=new JSONArray();
        facilityCodeArray.add(ZES_readNodeValueAsString(facilityCode));
        workEndPayload.put("facilityCode", facilityCodeArray);

        workEndPayload.put("goodQuantity", "0");
        workEndPayload.put("frequentlyInspectionCode", "");
        workEndPayload.put("workOrderCode", ZES_readNodeValueAsString(workOrderCodeDetail));
        workEndPayload.put("totalDefectiveQuantity", "0");
        workEndPayload.put("worker", "Company");
        workEndPayload.put("totalPauseTime", "00:00:00");
        workEndPayload.put("workEndTime", LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        workEndPayload.put("defectInfo", new JSONArray());
        workEndPayload.put("workStartTime", workStartTime);
        return workEndPayload;
    }

    private ZES_workHistoryState ZES_extractRegisteredWorkHistory(JSONObject response, String workOrderCode, String fallbackCompanyCode, String startTime)
    {
        if(response == null || !"success".equalsIgnoreCase(ZES_readJsonValueAsString(response.get("message")))) return null;
        JSONObject data=ZES_toJsonObject(response.get("data"));
        if(data == null) return null;
        String workHistoryCode=ZES_firstNonBlank(data, "workHistoryCode", "work_history_code");
        if(workHistoryCode.isBlank()) return null;
        String companyCode=ZES_firstNonBlank(data, "companyCode", "company_code");
        if(companyCode.isBlank()) companyCode=fallbackCompanyCode;
        return new ZES_workHistoryState(workHistoryCode, workOrderCode, companyCode, "working", startTime);
    }

    private JSONObject ZES_buildRestoredRegisterResponse(ZES_workHistoryState history)
    {
        JSONObject data=new JSONObject(true);
        data.put("workHistoryCode", history.workHistoryCode());
        data.put("companyCode", history.companyCode());
        JSONObject response=new JSONObject(true);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }

    private String ZES_firstNonBlank(JSONObject object, String... keys)
    {
        for(String key : keys){
            String value=ZES_readJsonValueAsString(object.get(key));
            if(!value.isBlank()) return value;
        }
        return "";
    }

    private JSONObject ZES_sendWorkHistoryPayload(String url, String apiName, JSONObject payload)
    {
        String ZES_lv_body = payload.toJSONString();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(ZES_lv_body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = WORK_HISTORY_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if(response.statusCode() >= 200 && response.statusCode() < 300){
                System.out.println("[OPC-UA][WORK-HISTORY-"+apiName+"-API] success url="+url+", status="+response.statusCode()+", body="+response.body());
            } else {
                System.out.println("[OPC-UA][WORK-HISTORY-"+apiName+"-API][FAIL] url="+url+", status="+response.statusCode()+", requestBody="+ZES_lv_body+", responseBody="+response.body());
            }
            return ZES_debugWorkHistoryApiReturn(apiName, response.body());
        } catch (Exception e) {
            System.out.println("[OPC-UA][WORK-HISTORY-"+apiName+"-API][ERROR] url="+url+", requestBody="+ZES_lv_body+", message="+e.getMessage());
            return null;
        }
    }

    private boolean ZES_sendWorkHistoryUpdateIfRegisterSuccess(
            JSONObject registerResponse,
            UaVariableNode companyCode,
            UaVariableNode goodQuantity,
            UaVariableNode totalDefectiveQuantity,
            UaVariableNode facilityCode,
            UaVariableNode processDefectName,
            UaVariableNode processDefectCode,
            UaVariableNode pauseTime,
            String productCode,
            String workStartTime,
            String workEndTime
    )
    {
        if(registerResponse == null){
            System.out.println("[OPC-UA][WORK-HISTORY-UPDATE-SKIP] register response is null");
            return false;
        }
        String ZES_lv_message = ZES_readJsonValueAsString(registerResponse.get("message"));
        if(!"success".equalsIgnoreCase(ZES_lv_message)){
            System.out.println("[OPC-UA][WORK-HISTORY-UPDATE-SKIP] register message="+ZES_lv_message);
            return false;
        }

        JSONObject ZES_lv_data = ZES_toJsonObject(registerResponse.get("data"));
        String ZES_lv_workHistoryCode = ZES_lv_data == null ? "" : ZES_readJsonValueAsString(ZES_lv_data.get("workHistoryCode"));
        if(ZES_lv_workHistoryCode.isEmpty()){
            System.out.println("[OPC-UA][WORK-HISTORY-UPDATE-SKIP] workHistoryCode is empty, data="+registerResponse.get("data"));
            return false;
        }

        Object ZES_lv_bomInfo = ZES_getWorkHistoryInputMaterialLotAutoProcess(productCode, goodQuantity);
        JSONObject updatePayload = ZES_buildWorkHistoryUpdatePayload(
                ZES_lv_workHistoryCode,
                ZES_lv_bomInfo,
                companyCode,
                goodQuantity,
                totalDefectiveQuantity,
                facilityCode,
                processDefectName,
                processDefectCode,
                pauseTime,
                workStartTime,
                workEndTime
        );
        System.out.println("[OPC-UA][WORK-HISTORY-UPDATE-PAYLOAD] url="+WORK_HISTORY_UPDATE_URL+", payload="+updatePayload.toJSONString());
        JSONObject updateResponse = ZES_sendWorkHistoryPayload(WORK_HISTORY_UPDATE_URL, "UPDATE", updatePayload);
        ZES_debugWorkHistoryUpdateResponse(updateResponse);
        return updateResponse != null && "success".equalsIgnoreCase(ZES_readJsonValueAsString(updateResponse.get("message")));
    }

    private void ZES_debugWorkHistoryUpdateResponse(JSONObject updateResponse)
    {
        // TEST DEBUG: workHistory update API response 확인 후 테스트 완료 시 이 메서드 호출/메서드를 삭제해도 됩니다.
        if(updateResponse == null){
            System.out.println("[OPC-UA][WORK-HISTORY-UPDATE-RESPONSE-DEBUG] update response is null");
            return;
        }
        Object ZES_lv_code = updateResponse.containsKey("code") ? updateResponse.get("code") : updateResponse.get("status");
        System.out.println("[OPC-UA][WORK-HISTORY-UPDATE-RESPONSE-DEBUG] codeOrStatus="+ZES_lv_code+", message="+updateResponse.get("message")+", data="+updateResponse.get("data")+", raw="+updateResponse.toJSONString());
    }

    private JSONObject ZES_buildWorkHistoryUpdatePayload(
            String workHistoryCode,
            Object bomInfo,
            UaVariableNode companyCode,
            UaVariableNode goodQuantity,
            UaVariableNode totalDefectiveQuantity,
            UaVariableNode facilityCode,
            UaVariableNode processDefectName,
            UaVariableNode processDefectCode,
            UaVariableNode pauseTime,
            String workStartTime,
            String workEndTime
    )
    {
        String ZES_lv_totalDefectiveQuantity = ZES_readNodeValueAsString(totalDefectiveQuantity);
        JSONObject updatePayload = new JSONObject(true);
        updatePayload.put("workHistoryCode", workHistoryCode);
        updatePayload.put("companyCode", ZES_readNodeValueAsString(companyCode));
        updatePayload.put("workStartTime", workStartTime);
        updatePayload.put("workEndTime", workEndTime);
        updatePayload.put("totalCount", ZES_readNodeValueAsString(goodQuantity));
        updatePayload.put("totalDefectiveQuantity", ZES_lv_totalDefectiveQuantity);
        updatePayload.put("frequentlyInspectionCode", "");
        JSONArray facilityCodeArray = new JSONArray();
        facilityCodeArray.add(ZES_readNodeValueAsString(facilityCode));
        updatePayload.put("facilityCode", facilityCodeArray);

        JSONArray defectInfo = new JSONArray();
        JSONObject defectItem = new JSONObject(true);
        defectItem.put("processDefectName", ZES_readNodeValueAsString(processDefectName));
        defectItem.put("defectQuantity", ZES_lv_totalDefectiveQuantity);
        defectItem.put("processDefectCode", ZES_readNodeValueAsString(processDefectCode));
        defectInfo.add(defectItem);
        updatePayload.put("defectInfo", defectInfo);

        updatePayload.put("totalPauseTime", ZES_readNodeValueAsString(pauseTime));
        updatePayload.put("pauseStartTime", null);
        updatePayload.put("bomInfo", bomInfo == null ? new JSONArray() : bomInfo);
        return updatePayload;
    }

    private Object ZES_getWorkHistoryInputMaterialLotAutoProcess(String productCode, UaVariableNode goodQuantity)
    {
        String ZES_lv_productCode = productCode == null ? "" : productCode.trim();
        String ZES_lv_output = ZES_readNodeValueAsString(goodQuantity);
        if(ZES_lv_productCode.isEmpty()){
            System.out.println("[OPC-UA][WORK-HISTORY-INPUT-MATERIAL-SKIP] productCode is empty");
            return new JSONArray();
        }

        JSONObject requestBody = new JSONObject(true);
        requestBody.put("productCode", ZES_lv_productCode);
        requestBody.put("output", ZES_lv_output);
        String ZES_lv_body = requestBody.toJSONString();
        System.out.println("[OPC-UA][WORK-HISTORY-INPUT-MATERIAL-AUTO-PROCESS-REQUEST-DEBUG] productCode="+ZES_lv_productCode+", output="+ZES_lv_output+", payload="+ZES_lv_body);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WORK_HISTORY_INPUT_MATERIAL_LOT_AUTO_PROCESS_URL))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(ZES_lv_body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = WORK_HISTORY_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            System.out.println("[OPC-UA][WORK-HISTORY-INPUT-MATERIAL-AUTO-PROCESS-RESPONSE-DEBUG] status="+response.statusCode()+", body="+response.body());
            if(response.statusCode() >= 200 && response.statusCode() < 300){
                System.out.println("[OPC-UA][WORK-HISTORY-INPUT-MATERIAL-AUTO-PROCESS-API] success url="+WORK_HISTORY_INPUT_MATERIAL_LOT_AUTO_PROCESS_URL+", status="+response.statusCode()+", requestBody="+ZES_lv_body+", body="+response.body());
            } else {
                System.out.println("[OPC-UA][WORK-HISTORY-INPUT-MATERIAL-AUTO-PROCESS-API][FAIL] url="+WORK_HISTORY_INPUT_MATERIAL_LOT_AUTO_PROCESS_URL+", status="+response.statusCode()+", requestBody="+ZES_lv_body+", responseBody="+response.body());
                return new JSONArray();
            }
            return ZES_extractBomInfoFromInputMaterialResponse(response.body());
        } catch (Exception e) {
            System.out.println("[OPC-UA][WORK-HISTORY-INPUT-MATERIAL-AUTO-PROCESS-API][ERROR] url="+WORK_HISTORY_INPUT_MATERIAL_LOT_AUTO_PROCESS_URL+", requestBody="+ZES_lv_body+", message="+e.getMessage());
            return new JSONArray();
        }
    }

    private Object ZES_extractBomInfoFromInputMaterialResponse(String responseBody)
    {
        if(responseBody == null || responseBody.isBlank()) return new JSONArray();
        try {
            Object parsed = JSON.parse(responseBody);
            if(parsed instanceof JSONArray || parsed instanceof JSONObject){
                if(parsed instanceof JSONObject jsonObject){
                    Object data = jsonObject.get("data");
                    if(data != null) return data;
                    Object bomInfo = jsonObject.get("BOMInfo");
                    if(bomInfo != null) return bomInfo;
                    bomInfo = jsonObject.get("bomInfo");
                    if(bomInfo != null) return bomInfo;
                }
                return parsed;
            }
        } catch (Exception e) {
            System.out.println("[OPC-UA][WORK-HISTORY-INPUT-MATERIAL-AUTO-PROCESS-API][PARSE-ERROR] body="+responseBody+", message="+e.getMessage());
        }
        return new JSONArray();
    }

    private JSONObject ZES_debugWorkHistoryApiReturn(String apiName, String responseBody)
    {
        // TEST DEBUG: workHistory API return 확인 후 운영 반영 시 아래 로그 메서드 호출/메서드를 삭제해도 됩니다.
        try {
            JSONObject ZES_lv_response = JSONObject.parseObject(responseBody);
            Object ZES_lv_code = ZES_lv_response.containsKey("code") ? ZES_lv_response.get("code") : ZES_lv_response.get("status");
            System.out.println("[OPC-UA][WORK-HISTORY-"+apiName+"-API][RETURN-DEBUG] codeOrStatus="+ZES_lv_code+", message="+ZES_lv_response.get("message")+", data="+ZES_lv_response.get("data"));
            return ZES_lv_response;
        } catch (Exception e) {
            System.out.println("[OPC-UA][WORK-HISTORY-"+apiName+"-API][RETURN-DEBUG] nonJsonBody="+responseBody+", parseMessage="+e.getMessage());
            return null;
        }
    }

    private JSONObject ZES_toJsonObject(Object value)
    {
        if(value instanceof JSONObject jsonObject) return jsonObject;
        if(value == null) return null;
        try { return JSONObject.parseObject(String.valueOf(value)); } catch (Exception e) { return null; }
    }

    private String ZES_readJsonValueAsString(Object value)
    {
        return value == null ? "" : String.valueOf(value);
    }

    private String ZES_readNodeValueAsString(UaVariableNode node)
    {
        if(node == null || node.getValue() == null || node.getValue().getValue() == null) return "";
        Object raw = node.getValue().getValue().getValue();
        return raw == null ? "" : String.valueOf(raw);
    }

    private ZES_opcUaWorkItem ZES_emptyWorkItem()
    {
        return new ZES_opcUaWorkItem("", "", "", "", "", "", "", "", "", "", "", "");
    }

    private ZES_opcUaWorkItem ZES_findWorkItem(Map<Short, List<ZES_opcUaWorkItem>> pageCache, String workOrderCode)
    {
        if(workOrderCode == null || workOrderCode.isBlank()) return ZES_emptyWorkItem();
        for(List<ZES_opcUaWorkItem> pageItems : pageCache.values()){
            for(ZES_opcUaWorkItem item : pageItems){
                if(workOrderCode.equals(item.work_order_code())) return item;
            }
        }
        return ZES_emptyWorkItem();
    }

    private void ZES_restoreWorkingHistory(
            ZES_workHistoryState history,
            ZES_opcUaWorkItem item,
            UaVariableNode workStatus,
            UaVariableNode workTime,
            UaVariableNode serialCodeDetail,
            UaVariableNode productNameDetail,
            UaVariableNode workOrderCodeDetail,
            UaVariableNode processDetail,
            UaVariableNode processCodeDetail,
            UaVariableNode facilityName,
            UaVariableNode facilityCode,
            UaVariableNode processDefectCode,
            UaVariableNode processDefectName,
            UaVariableNode companyCode,
            UaVariableNode targetGoalDetail,
            ZES_opcUaWorkItem[] selectedWorkItem,
            long[] workSeconds,
            long[] pauseSeconds,
            String[] workStartTime,
            boolean[] workStartCaptured,
            long[] lastTimerMillis)
    {
        LocalDateTime startTime=ZES_parseWorkStartTime(history.startTime());
        if(startTime == null) return;
        long totalElapsedSeconds=Math.max(0L, ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()));
        workSeconds[0]=Math.max(0L, totalElapsedSeconds-pauseSeconds[0]);
        workStartTime[0]=startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        workStartCaptured[0]=true;
        lastTimerMillis[0]=System.currentTimeMillis();
        workStatus.setValue(new DataValue(new Variant((short)1)));
        workTime.setValue(new DataValue(new Variant(ZES_formatElapsedTime(workSeconds[0]))));
        selectedWorkItem[0]=item;
        ZES_setWorkItemDetailTags(item, serialCodeDetail, productNameDetail, workOrderCodeDetail, processDetail, processCodeDetail, facilityName, facilityCode, processDefectCode, processDefectName, companyCode, targetGoalDetail);
    }

    private void ZES_setWorkItemDetailTags(
            ZES_opcUaWorkItem item,
            UaVariableNode serialCodeDetail,
            UaVariableNode productNameDetail,
            UaVariableNode workOrderCodeDetail,
            UaVariableNode processDetail,
            UaVariableNode processCodeDetail,
            UaVariableNode facilityName,
            UaVariableNode facilityCode,
            UaVariableNode processDefectCode,
            UaVariableNode processDefectName,
            UaVariableNode companyCode,
            UaVariableNode targetGoalDetail)
    {
        serialCodeDetail.setValue(new DataValue(new Variant(item.serial_code())));
        productNameDetail.setValue(new DataValue(new Variant(item.product_name())));
        workOrderCodeDetail.setValue(new DataValue(new Variant(item.work_order_code())));
        processDetail.setValue(new DataValue(new Variant(item.process_row())));
        processCodeDetail.setValue(new DataValue(new Variant(item.process_row())));
        facilityName.setValue(new DataValue(new Variant(item.facility_name())));
        facilityCode.setValue(new DataValue(new Variant(item.facility_code())));
        processDefectCode.setValue(new DataValue(new Variant(item.process_defect_code())));
        processDefectName.setValue(new DataValue(new Variant(item.process_defect_name())));
        companyCode.setValue(new DataValue(new Variant(item.company_code())));
        targetGoalDetail.setValue(new DataValue(new Variant(item.target_goal())));
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
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private LocalDateTime ZES_parseWorkStartTime(String value)
    {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > 19) normalized = normalized.substring(0, 19);
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            System.out.println("[OPC-UA][WORK-HISTORY-RESTORE] invalid start_time="+value);
            return null;
        }
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
        if (v.isEmpty() || "0".equals(v)) return "";
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
