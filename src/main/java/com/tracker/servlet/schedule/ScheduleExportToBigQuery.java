package com.tracker.servlet.schedule;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tracker.util.TaskUtil;

@SuppressWarnings("serial")
public class ScheduleExportToBigQuery extends HttpServlet {

  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(ScheduleExportToBigQuery.class.getName());
  
  private static final List<String> kinds = Arrays.asList("ArrivalCompletions", "HITgroup",
          "HITinstance", "HITrequester", "MarketStatistics", "TopRequester");

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      Map<String, String> params = new HashMap<String, String>();
      for(String kind: kinds) {
          params.put("kind", kind);
          TaskUtil.queueTask("/exportToBigQuery", params);
      }
  }

}
