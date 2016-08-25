/*
 * Copyright 2012 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package azkaban.webapp.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.Minutes;
import org.joda.time.ReadablePeriod;
import org.joda.time.format.DateTimeFormat;

import azkaban.executor.ExecutableFlow;
import azkaban.executor.ExecutionOptions;
import azkaban.executor.ExecutorManagerAdapter;
import azkaban.executor.ExecutorManagerException;
import azkaban.flow.Flow;
import azkaban.flow.Node;
import azkaban.project.Project;
import azkaban.project.ProjectLogEvent.EventType;
import azkaban.project.ProjectManager;
import azkaban.scheduler.Schedule;
import azkaban.scheduler.ScheduleManager;
import azkaban.scheduler.ScheduleManagerException;
import azkaban.server.session.Session;
import azkaban.server.HttpRequestUtils;
import azkaban.sla.SlaOption;
import azkaban.user.Permission;
import azkaban.user.Permission.Type;
import azkaban.user.User;
import azkaban.user.UserManager;
import azkaban.utils.JSONUtils;
import azkaban.utils.SplitterOutputStream;
import azkaban.utils.Utils;
import azkaban.webapp.AzkabanWebServer;
import azkaban.webapp.SchedulerStatistics;

public class ScheduleServlet extends LoginAbstractAzkabanServlet {
  private static final String FILTER_BY_DATE_PATTERN = "MM/dd/yyyy hh:mm aa";
  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(ScheduleServlet.class);
  private ProjectManager projectManager;
  private ScheduleManager scheduleManager;
  private UserManager userManager;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    AzkabanWebServer server = (AzkabanWebServer) getApplication();
    userManager = server.getUserManager();
    projectManager = server.getProjectManager();
    scheduleManager = server.getScheduleManager();
  }

  @Override
  protected void handleGet(HttpServletRequest req, HttpServletResponse resp,
      Session session) throws ServletException, IOException {
    if (hasParam(req, "ajax")) {
      handleAJAXAction(req, resp, session);
    } else if (hasParam(req, "calendar")) {
      handleGetScheduleCalendar(req, resp, session);
    } else {
      handleGetAllSchedules(req, resp, session);
    }
  }

  private void handleAJAXAction(HttpServletRequest req,
      HttpServletResponse resp, Session session) throws ServletException,
      IOException {
    HashMap<String, Object> ret = new HashMap<String, Object>();
    String ajaxName = getParam(req, "ajax");

    if (ajaxName.equals("slaInfo")) {
      ajaxSlaInfo(req, ret, session.getUser());
    } else if (ajaxName.equals("setSla")) {
      ajaxSetSla(req, ret, session.getUser());
    } else if (ajaxName.equals("loadFlow")) {
      ajaxLoadFlows(req, ret, session.getUser());
    } else if (ajaxName.equals("loadHistory")) {
      ajaxLoadHistory(req, resp, session.getUser());
      ret = null;
    } else if (ajaxName.equals("scheduleFlow")) {
      ajaxScheduleFlow(req, ret, session.getUser());
    } else if (ajaxName.equals("fetchSchedule")) {
      ajaxFetchSchedule(req, ret, session.getUser());
    }

    if (ret != null) {
      this.writeJSON(resp, ret);
    }
  }

  private void ajaxSetSla(HttpServletRequest req, HashMap<String, Object> ret,
      User user) {
    try {
      int scheduleId = getIntParam(req, "scheduleId");
      Schedule sched = scheduleManager.getSchedule(scheduleId);

      Project project = projectManager.getProject(sched.getProjectId());
      if (!hasPermission(project, user, Permission.Type.SCHEDULE)) {
        ret.put("error", "User " + user
            + " does not have permission to set SLA for this flow.");
        return;
      }

      String emailStr = getParam(req, "slaEmails");
      String[] emailSplit = emailStr.split("\\s*,\\s*|\\s*;\\s*|\\s+");
      List<String> slaEmails = Arrays.asList(emailSplit);

      String shortMessageStr = getParam(req, "shortMessage");
      String[] shortMessageSplit = shortMessageStr.split("\\s*,\\s*|\\s*;\\s*|\\s+");
      List<String> shortMessage = Arrays.asList(shortMessageSplit);

      Map<String, String> settings = getParamGroup(req, "settings");

      List<SlaOption> slaOptions = new ArrayList<SlaOption>();
      for (String set : settings.keySet()) {
        SlaOption sla;
        try {
          sla = parseSlaSetting(settings.get(set));
        } catch (Exception e) {
          throw new ServletException(e);
        }
        if (sla != null) {
          sla.getInfo().put(SlaOption.INFO_FLOW_NAME, sched.getFlowName());
          sla.getInfo().put(SlaOption.INFO_EMAIL_LIST, slaEmails);
          sla.getInfo().put(SlaOption.INFO_SHORTMESSAGE_LIST, shortMessage);
          slaOptions.add(sla);
        }
      }

      sched.setSlaOptions(slaOptions);
      scheduleManager.insertSchedule(sched);

      if (slaOptions != null) {
        projectManager.postProjectEvent(project, EventType.SLA,
            user.getUserId(), "SLA for flow " + sched.getFlowName()
                + " has been added/changed.");
      }

    } catch (ServletException e) {
      ret.put("error", e.getMessage());
    } catch (ScheduleManagerException e) {
      ret.put("error", e.getMessage());
    }

  }

  private SlaOption parseSlaSetting(String set) throws ScheduleManagerException {
    logger.info("Tryint to set sla with the following set: " + set);

    String slaType;
    List<String> slaActions = new ArrayList<String>();
    Map<String, Object> slaInfo = new HashMap<String, Object>();
    String[] parts = set.split(",", -1);
    String id = parts[0];
    String rule = parts[1];
    String duration = parts[2];
    String emailAction = parts[3];
    String shortMessageAction = parts[5];
    String killAction = parts[4];
    if (emailAction.equals("true")||shortMessageAction.equals("true")||killAction.equals("true")) {
      if (emailAction.equals("true")) {
        slaActions.add(SlaOption.ACTION_ALERT);
        slaInfo.put(SlaOption.ALERT_TYPE, "email");
      }
      if (shortMessageAction.equals("true")) {
        slaActions.add(SlaOption.ACTION_ALERT);
        slaInfo.put(SlaOption.ALERT_SHORTMESSAGE_TYPE, "shortMessage");
      }
      if (killAction.equals("true")) {
        slaActions.add(SlaOption.ACTION_CANCEL_FLOW);
      }
      if (id.equals("")) {
        if (rule.equals("SUCCESS")) {
          slaType = SlaOption.TYPE_FLOW_SUCCEED;
        } else {
          slaType = SlaOption.TYPE_FLOW_FINISH;
        }
      } else {
        slaInfo.put(SlaOption.INFO_JOB_NAME, id);
        if (rule.equals("SUCCESS")) {
          slaType = SlaOption.TYPE_JOB_SUCCEED;
        } else {
          slaType = SlaOption.TYPE_JOB_FINISH;
        }
      }

      ReadablePeriod dur;
      try {
        dur = parseDuration(duration);
      } catch (Exception e) {
        throw new ScheduleManagerException(
            "Unable to parse duration for a SLA that needs to take actions!", e);
      }

      slaInfo.put(SlaOption.INFO_DURATION, Utils.createPeriodString(dur));
      SlaOption r = new SlaOption(slaType, slaActions, slaInfo);
      logger.info("Parsing sla as id:" + id + " type:" + slaType + " rule:"
          + rule + " Duration:" + duration + " actions:" + slaActions);
      return r;
    }
    return null;
  }

  private ReadablePeriod parseDuration(String duration) {
    int hour = Integer.parseInt(duration.split(":")[0]);
    int min = Integer.parseInt(duration.split(":")[1]);
    return Minutes.minutes(min + hour * 60).toPeriod();
  }

  private void ajaxFetchSchedule(HttpServletRequest req,
      HashMap<String, Object> ret, User user) throws ServletException {

    int projectId = getIntParam(req, "projectId");
    String flowId = getParam(req, "flowId");
    try {
      Schedule schedule = scheduleManager.getSchedule(projectId, flowId);

      if (schedule != null) {
        Map<String, Object> jsonObj = new HashMap<String, Object>();
        jsonObj.put("scheduleId", Integer.toString(schedule.getScheduleId()));
        jsonObj.put("submitUser", schedule.getSubmitUser());
        jsonObj.put("firstSchedTime",
            utils.formatDateTime(schedule.getFirstSchedTime()));
        jsonObj.put("nextExecTime",
            utils.formatDateTime(schedule.getNextExecTime()));
        jsonObj.put("period", utils.formatPeriod(schedule.getPeriod()));
        jsonObj.put("executionOptions", schedule.getExecutionOptions());
        ret.put("schedule", jsonObj);
      }
    } catch (ScheduleManagerException e) {
      ret.put("error", e);
    }
  }

  private void ajaxSlaInfo(HttpServletRequest req, HashMap<String, Object> ret,
      User user) {
    int scheduleId;
    try {
      scheduleId = getIntParam(req, "scheduleId");
      Schedule sched = scheduleManager.getSchedule(scheduleId);
      Project project =
          getProjectAjaxByPermission(ret, sched.getProjectId(), user, Type.READ);
      if (project == null) {
        ret.put("error",
            "Error loading project. Project " + sched.getProjectId()
                + " doesn't exist");
        return;
      }

      Flow flow = project.getFlow(sched.getFlowName());
      if (flow == null) {
        ret.put("error", "Error loading flow. Flow " + sched.getFlowName()
            + " doesn't exist in " + sched.getProjectId());
        return;
      }

      List<SlaOption> slaOptions = sched.getSlaOptions();
      ExecutionOptions flowOptions = sched.getExecutionOptions();

      if (slaOptions != null && slaOptions.size() > 0) {
        ret.put("slaEmails",
            slaOptions.get(0).getInfo().get(SlaOption.INFO_EMAIL_LIST));

        ret.put("shortMessage",
                slaOptions.get(0).getInfo().get(SlaOption.INFO_SHORTMESSAGE_LIST));

        List<Object> setObj = new ArrayList<Object>();
        for (SlaOption sla : slaOptions) {
          setObj.add(sla.toWebObject());
        }
        ret.put("settings", setObj);
      } else if (flowOptions != null) {
        if (flowOptions.getFailureEmails() != null) {
          List<String> emails = flowOptions.getFailureEmails();
          if (emails.size() > 0) {
            ret.put("slaEmails", emails);
          }
        }
      } else {
        if (flow.getFailureEmails() != null) {
          List<String> emails = flow.getFailureEmails();
          if (emails.size() > 0) {
            ret.put("slaEmails", emails);
          }
        }
      }

      List<String> allJobs = new ArrayList<String>();
      for (Node n : flow.getNodes()) {
        allJobs.add(n.getId());
      }

      ret.put("allJobNames", allJobs);
    } catch (ServletException e) {
      ret.put("error", e);
    } catch (ScheduleManagerException e) {
      ret.put("error", e);
    }
  }

  protected Project getProjectAjaxByPermission(Map<String, Object> ret,
      int projectId, User user, Permission.Type type) {
    Project project = projectManager.getProject(projectId);

    if (project == null) {
      ret.put("error", "Project '" + project + "' not found.");
    } else if (!hasPermission(project, user, type)) {
      ret.put("error",
          "User '" + user.getUserId() + "' doesn't have " + type.name()
              + " permissions on " + project.getName());
    } else {
      return project;
    }

    return null;
  }

  private void handleGetAllSchedules(HttpServletRequest req,
      HttpServletResponse resp, Session session) throws ServletException,
      IOException {

    Page page =
        newPage(req, resp, session,
            "azkaban/webapp/servlet/velocity/scheduledflowpage.vm");

    List<Schedule> schedules;
    Map<Integer,Object> map = new HashMap<Integer,Object>();
    Map<Integer,Object> nummap = new HashMap<Integer,Object>();
    Map<Integer,Object> finalruntimemap = new HashMap<Integer,Object>();
    try {
      schedules = scheduleManager.getSchedules();
      if (hasParam(req, "advfilter")) {
        String projContain = getParam(req, "projcontain");
        String flowContain = getParam(req, "flowcontain");
        String userContain = getParam(req, "usercontain");
        String statusContain = getParam(req, "status");
        String period_units = getParam(req, "period_units");
        String has_shortMessage = getParam(req, "has_shortMessage");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm");
        String begin = getParam(req, "begin");
        long beginval = -1;
        if(begin!=""){
          String beginstr = begin.substring(0,16);
          String zwstr = begin.substring(11,13);
          try {
            if(begin.indexOf("PM")!=-1&&zwstr!="12"){
                Date beginDate = sdf.parse(beginstr);
                Calendar ca=Calendar.getInstance();
                ca.setTime(beginDate);
                ca.add(Calendar.HOUR_OF_DAY, 12);
                beginval = ca.getTimeInMillis();
            }else{
                Date beginDate = sdf.parse(beginstr);
                Calendar ca=Calendar.getInstance();
                ca.setTime(beginDate);
                beginval = ca.getTimeInMillis();
            }
          } catch (ParseException e) {
            e.printStackTrace();
          }
        }
        long beginTime = begin == "" ? -1 : beginval;
        String end = getParam(req, "end");
        long endval = -1;
        if(end!=""){
          String endstr = end.substring(0,16);
          String zwstr = end.substring(11,13);
          try {
            if(end.indexOf("PM")!=-1&&zwstr!="12"){
              Date endDate = sdf.parse(endstr);
              Calendar ca=Calendar.getInstance();
              ca.setTime(endDate);
              ca.add(Calendar.HOUR_OF_DAY, 12);
              endval = ca.getTimeInMillis();
            }else{
              Date endDate = sdf.parse(endstr);
              Calendar ca=Calendar.getInstance();
              ca.setTime(endDate);
              endval = ca.getTimeInMillis();
            }
          } catch (ParseException e) {
            e.printStackTrace();
          }
        }
        long endTime = end == "" ? -1 : endval;

        for(int i=0;i<schedules.size();i++){
          Schedule sch = schedules.get(i);
          String projectname = sch.getProjectName();
          if(projContain!=""&&projContain!=null){
            if(!projectname.equals(projContain)){
              schedules.remove(i);
              i--;
            }
          }else{
            break;
          }
        }

        for(int i=0;i<schedules.size();i++){
          Schedule sch = schedules.get(i);
          String flowname = sch.getFlowName();
          if(flowContain!=""&&flowContain!=null){
            if(!flowname.equals(flowContain)){
              schedules.remove(i);
              i--;
            }
          }else{
            break;
          }
        }

        for(int i=0;i<schedules.size();i++){
          Schedule sch = schedules.get(i);
          String status = sch.getStatus();
          if(statusContain!=""&&statusContain!=null){
            if(!status.equals(statusContain)){
              schedules.remove(i);
              i--;
            }
          }else{
            break;
          }
        }

        for(int i=0;i<schedules.size();i++){
          Schedule sch = schedules.get(i);
          String  successShortMessage = "";
          String  failureShortMessage = "";
          String  shortMessageFlag = "";
          List successNumberList = sch.getExecutionOptions().getSuccessNumber();
          List failureNumberList = sch.getExecutionOptions().getFailureNumber();
          if(successNumberList.size()>0){
            successShortMessage = (String) successNumberList.get(0);
          }
          if(failureNumberList.size()>0){
            failureShortMessage = (String) failureNumberList.get(0);
          }
          if(successShortMessage!=""||failureShortMessage!=""){
              shortMessageFlag = "1";
          }else{
              shortMessageFlag = "2";
          }

          if(has_shortMessage!=""&&has_shortMessage!=null){
            if(!has_shortMessage.equals(shortMessageFlag)){
              schedules.remove(i);
              i--;
            }
          }else{
            break;
          }
        }

        for(int i=0;i<schedules.size();i++){
          Schedule sch = schedules.get(i);
          String periodStr = sch.getPeriod().toString();
          String firstChar = periodStr.substring(0,1);
          String secondChar = periodStr.substring(0,2);
          String periodUnit = periodStr.substring(periodStr.length()-1,periodStr.length());
          if("P".equals(firstChar)&&"PT".equals(secondChar)&&"M".equals(periodUnit)){
            periodUnit = "m";
          }
          if(period_units!=""&&period_units!=null){
            if(!periodUnit.equals(period_units)){
              schedules.remove(i);
              i--;
            }
          }else{
            break;
          }
        }

        for(int i=0;i<schedules.size();i++){
          Schedule sch = schedules.get(i);
          String user = sch.getSubmitUser();
          if(userContain!=""&&userContain!=null){
            if(!user.equals(userContain)){
              schedules.remove(i);
              i--;
            }
          }else{
            break;
          }
        }

        for(int i=0;i<schedules.size();i++){
          Schedule sch = schedules.get(i);
          long lastModifyTime = sch.getLastModifyTime();
          if((begin!=""&&begin!=null)||(end!=""&&end!=null)){
              if(begin!=""&&beginTime>lastModifyTime){
                schedules.remove(i);
                i--;
              }

              if(end!=""&&endTime<lastModifyTime){
                schedules.remove(i);
                i--;
              }
          }else{
            break;
          }
        }

      }
    } catch (ScheduleManagerException e) {
      throw new ServletException(e);
    }
   // schedules.get(0).getExecutionOptions().getSuccessNumber().get(0);
    for(int i=0;i<schedules.size();i++){
       int scheduleId = schedules.get(i).getScheduleId();
       String  successShortMessage = "";
       String  failureShortMessage = "";
       List successNumberList = schedules.get(i).getExecutionOptions().getSuccessNumber();
       List failureNumberList = schedules.get(i).getExecutionOptions().getFailureNumber();
       if(successNumberList.size()>0){
        // successShortMessage = (String) successNumberList.get(0);
         for(int m=0;m<successNumberList.size();m++){
           successShortMessage+=(String) successNumberList.get(m)+",";
         }
       }
       if(failureNumberList.size()>0){
        // failureShortMessage = (String) failureNumberList.get(0);
         for(int n=0;n<failureNumberList.size();n++){
           failureShortMessage+=(String) failureNumberList.get(n)+",";
         }
       }
      String numMessage = "";
      if(successShortMessage!=""||failureShortMessage!=""){
        map.put(scheduleId,"true");
        if(successShortMessage!=""){
          numMessage+="Success:"+successShortMessage.substring(0,successShortMessage.length()-1)+";";
        }
        if(failureShortMessage!=""){
          numMessage+="Failure:"+failureShortMessage.substring(0,failureShortMessage.length()-1)+";";
        }
        nummap.put(scheduleId,numMessage);
      }else{
        map.put(scheduleId,"false");
        nummap.put(scheduleId,numMessage);
      }
    }
    for(int i=0;i<schedules.size();i++){
      int scheduleId = schedules.get(i).getScheduleId();
      long lastModifyTime = schedules.get(i).getLastModifyTime();
      Calendar ca = Calendar.getInstance();
      ca.setTimeInMillis(lastModifyTime);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      String finalruntime = sdf.format(ca.getTime());
      finalruntimemap.put(scheduleId,finalruntime);
    }
    page.add("schedules", schedules);
    page.add("map", map);
    page.add("nummap", nummap);
    page.add("finalruntimemap", finalruntimemap);
    page.render();
  }

  private void handleGetScheduleCalendar(HttpServletRequest req,
      HttpServletResponse resp, Session session) throws ServletException,
      IOException {

    Page page =
        newPage(req, resp, session,
            "azkaban/webapp/servlet/velocity/scheduledflowcalendarpage.vm");

    List<Schedule> schedules;
    try {
      schedules = scheduleManager.getSchedules();
    } catch (ScheduleManagerException e) {
      throw new ServletException(e);
    }
    page.add("schedules", schedules);
    page.render();
  }

  @Override
  protected void handlePost(HttpServletRequest req, HttpServletResponse resp,
      Session session) throws ServletException, IOException {
    if (hasParam(req, "ajax")) {
      handleAJAXAction(req, resp, session);
    } else {
      HashMap<String, Object> ret = new HashMap<String, Object>();
      if (hasParam(req, "action")) {
        String action = getParam(req, "action");
        if (action.equals("scheduleFlow")) {
          ajaxScheduleFlow(req, ret, session.getUser());
        } else if (action.equals("removeSched")) {
          ajaxRemoveSched(req, ret, session.getUser());
        }
      }

      if (ret.get("status") == ("success"))
        setSuccessMessageInCookie(resp, (String) ret.get("message"));
      else
        setErrorMessageInCookie(resp, (String) ret.get("message"));

      this.writeJSON(resp, ret);
    }
  }

  private void ajaxLoadFlows(HttpServletRequest req,
      HashMap<String, Object> ret, User user) throws ServletException {
    List<Schedule> schedules;
    try {
      schedules = scheduleManager.getSchedules();
    } catch (ScheduleManagerException e) {
      throw new ServletException(e);
    }
    // See if anything is scheduled
    if (schedules.size() <= 0)
      return;

    List<HashMap<String, Object>> output =
        new ArrayList<HashMap<String, Object>>();
    ret.put("items", output);

    for (Schedule schedule : schedules) {
      try {
        writeScheduleData(output, schedule);
      } catch (ScheduleManagerException e) {
        throw new ServletException(e);
      }
    }
  }

  private void writeScheduleData(List<HashMap<String, Object>> output,
      Schedule schedule) throws ScheduleManagerException {
    Map<String, Object> stats =
        SchedulerStatistics.getStatistics(schedule.getScheduleId(),
            (AzkabanWebServer) getApplication());
    HashMap<String, Object> data = new HashMap<String, Object>();
    data.put("scheduleid", schedule.getScheduleId());
    data.put("flowname", schedule.getFlowName());
    data.put("projectname", schedule.getProjectName());
    data.put("time", schedule.getFirstSchedTime());

    DateTime time = DateTime.now();
    long period = 0;
    if (schedule.getPeriod() != null) {
      period = time.plus(schedule.getPeriod()).getMillis() - time.getMillis();
    }
    data.put("period", period);
    int length = 3600 * 1000;
    if (stats.get("average") != null && stats.get("average") instanceof Integer) {
      length = (int) (Integer) stats.get("average");
      if (length == 0) {
        length = 3600 * 1000;
      }
    }
    data.put("length", length);
    data.put("history", false);
    data.put("stats", stats);
    output.add(data);
  }

  private void ajaxLoadHistory(HttpServletRequest req,
      HttpServletResponse resp, User user) throws ServletException, IOException {
    resp.setContentType(JSON_MIME_TYPE);
    long today = DateTime.now().withTime(0, 0, 0, 0).getMillis();
    long startTime = getLongParam(req, "startTime");
    DateTime start = new DateTime(startTime);
    // Ensure start time is 12:00 AM
    startTime = start.withTime(0, 0, 0, 0).getMillis();
    boolean useCache = false;
    if (startTime < today) {
      useCache = true;
    }
    long endTime = startTime + 24 * 3600 * 1000;
    int loadAll = getIntParam(req, "loadAll");

    // Cache file
    String cacheDir =
        getApplication().getServerProps().getString("cache.directory", "cache");
    File cacheDirFile = new File(cacheDir, "schedule-history");
    File cache = new File(cacheDirFile, startTime + ".cache");
    cache.getParentFile().mkdirs();

    if (useCache) {
      // Determine if cache exists
      boolean cacheExists = false;
      synchronized (this) {
        cacheExists = cache.exists() && cache.isFile();
      }
      if (cacheExists) {
        // Send the cache instead
        InputStream cacheInput =
            new BufferedInputStream(new FileInputStream(cache));
        try {
          IOUtils.copy(cacheInput, resp.getOutputStream());
          return;
        } finally {
          IOUtils.closeQuietly(cacheInput);
        }
      }
    }

    // Load data if not cached
    List<ExecutableFlow> history = null;
    try {
      AzkabanWebServer server = (AzkabanWebServer) getApplication();
      ExecutorManagerAdapter executorManager = server.getExecutorManager();
      history =
          executorManager.getExecutableFlows(null, null, null, 0, startTime,
              endTime, -1, -1);
    } catch (ExecutorManagerException e) {
      logger.error(e);
    }

    HashMap<String, Object> ret = new HashMap<String, Object>();
    List<HashMap<String, Object>> output =
        new ArrayList<HashMap<String, Object>>();
    ret.put("items", output);
    for (ExecutableFlow historyItem : history) {
      // Check if it is an scheduled execution
      if (historyItem.getScheduleId() >= 0 || loadAll != 0) {
        writeHistoryData(output, historyItem);
      }
    }

    // Make sure we're ready to cache it, otherwise output and return
    synchronized (this) {
      if (!useCache || cache.exists()) {
        JSONUtils.toJSON(ret, resp.getOutputStream(), false);
        return;
      }
    }

    // Create cache file
    File cacheTemp = new File(cacheDirFile, startTime + ".tmp");
    cacheTemp.createNewFile();
    OutputStream cacheOutput =
        new BufferedOutputStream(new FileOutputStream(cacheTemp));
    try {
      OutputStream outputStream =
          new SplitterOutputStream(cacheOutput, resp.getOutputStream());
      // Write to both the cache file and web output
      JSONUtils.toJSON(ret, outputStream, false);
    } finally {
      IOUtils.closeQuietly(cacheOutput);
    }
    // Move cache file
    synchronized (this) {
      cacheTemp.renameTo(cache);
    }
  }

  private void writeHistoryData(List<HashMap<String, Object>> output,
      ExecutableFlow history) {
    HashMap<String, Object> data = new HashMap<String, Object>();

    data.put("scheduleid", history.getScheduleId());
    Project project = projectManager.getProject(history.getProjectId());
    data.put("flowname", history.getFlowId());
    data.put("projectname", project.getName());
    data.put("time", history.getStartTime());
    data.put("period", "0");
    long endTime = history.getEndTime();
    if (endTime == -1) {
      endTime = System.currentTimeMillis();
    }
    data.put("length", endTime - history.getStartTime());
    data.put("history", true);
    data.put("status", history.getStatus().getNumVal());

    output.add(data);
  }

  private void ajaxRemoveSched(HttpServletRequest req, Map<String, Object> ret,
      User user) throws ServletException {
    int scheduleId = getIntParam(req, "scheduleId");
    Schedule sched;
    try {
      sched = scheduleManager.getSchedule(scheduleId);
    } catch (ScheduleManagerException e) {
      throw new ServletException(e);
    }
    if (sched == null) {
      ret.put("message", "Schedule with ID " + scheduleId + " does not exist");
      ret.put("status", "error");
      return;
    }

    Project project = projectManager.getProject(sched.getProjectId());

    if (project == null) {
      ret.put("message", "Project " + sched.getProjectId() + " does not exist");
      ret.put("status", "error");
      return;
    }

    if (!hasPermission(project, user, Type.SCHEDULE)) {
      ret.put("status", "error");
      ret.put("message", "Permission denied. Cannot remove schedule with id "
          + scheduleId);
      return;
    }

    scheduleManager.removeSchedule(sched);
    logger.info("User '" + user.getUserId() + " has removed schedule "
        + sched.getScheduleName());
    projectManager
        .postProjectEvent(project, EventType.SCHEDULE, user.getUserId(),
            "Schedule " + sched.toString() + " has been removed.");

    ret.put("status", "success");
    ret.put("message", "flow " + sched.getFlowName()
        + " removed from Schedules.");
    return;
  }

  private void ajaxScheduleFlow(HttpServletRequest req,
      HashMap<String, Object> ret, User user) throws ServletException {
    String projectName = getParam(req, "projectName");
    String flowName = getParam(req, "flow");
    int projectId = getIntParam(req, "projectId");

    Project project = projectManager.getProject(projectId);

    if (project == null) {
      ret.put("message", "Project " + projectName + " does not exist");
      ret.put("status", "error");
      return;
    }

    if (!hasPermission(project, user, Type.SCHEDULE)) {
      ret.put("status", "error");
      ret.put("message", "Permission denied. Cannot execute " + flowName);
      return;
    }

    Flow flow = project.getFlow(flowName);
    if (flow == null) {
      ret.put("status", "error");
      ret.put("message", "Flow " + flowName + " cannot be found in project "
          + project);
      return;
    }

    String scheduleTime = getParam(req, "scheduleTime");
    String scheduleDate = getParam(req, "scheduleDate");
    DateTime firstSchedTime;
    try {
      firstSchedTime = parseDateTime(scheduleDate, scheduleTime);
    } catch (Exception e) {
      ret.put("error", "Invalid date and/or time '" + scheduleDate + " "
          + scheduleTime);
      return;
    }

    ReadablePeriod thePeriod = null;
    try {
      if (hasParam(req, "is_recurring")
          && getParam(req, "is_recurring").equals("on")) {
        thePeriod = Schedule.parsePeriodString(getParam(req, "period"));
      }
    } catch (Exception e) {
      ret.put("error", e.getMessage());
    }

    ExecutionOptions flowOptions = null;
    try {
      flowOptions = HttpRequestUtils.parseFlowOptions(req);
      HttpRequestUtils.filterAdminOnlyFlowParams(userManager, flowOptions, user);
    } catch (Exception e) {
      ret.put("error", e.getMessage());
    }

    List<SlaOption> slaOptions = null;

    Schedule schedule =
        scheduleManager.scheduleFlow(-1, projectId, projectName, flowName,
            "ready", firstSchedTime.getMillis(), firstSchedTime.getZone(),
            thePeriod, DateTime.now().getMillis(), firstSchedTime.getMillis(),
            firstSchedTime.getMillis(), user.getUserId(), flowOptions,
            slaOptions);
    logger.info("User '" + user.getUserId() + "' has scheduled " + "["
        + projectName + flowName + " (" + projectId + ")" + "].");
    projectManager.postProjectEvent(project, EventType.SCHEDULE,
        user.getUserId(), "Schedule " + schedule.toString()
            + " has been added.");

    ret.put("status", "success");
    ret.put("scheduleId", schedule.getScheduleId());
    ret.put("message", projectName + "." + flowName + " scheduled.");
  }

  private DateTime parseDateTime(String scheduleDate, String scheduleTime) {
    // scheduleTime: 12,00,pm,PDT
    String[] parts = scheduleTime.split(",", -1);
    int hour = Integer.parseInt(parts[0]);
    int minutes = Integer.parseInt(parts[1]);
    boolean isPm = parts[2].equalsIgnoreCase("pm");

    DateTimeZone timezone =
        parts[3].equals("UTC") ? DateTimeZone.UTC : DateTimeZone.getDefault();

    // scheduleDate: 02/10/2013
    DateTime day = null;
    if (scheduleDate == null || scheduleDate.trim().length() == 0) {
      day = new LocalDateTime().toDateTime();
    } else {
      day = DateTimeFormat.forPattern("MM/dd/yyyy")
          .withZone(timezone).parseDateTime(scheduleDate);
    }

    hour %= 12;

    if (isPm)
      hour += 12;

    DateTime firstSchedTime =
        day.withHourOfDay(hour).withMinuteOfHour(minutes).withSecondOfMinute(0);

    return firstSchedTime;
  }
}
