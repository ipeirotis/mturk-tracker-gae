package com.tracker.servlet.schedule;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tracker.util.TaskUtil;

@SuppressWarnings("serial")
public class ScheduleLatestTasksPulling extends HttpServlet {
  
  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(ScheduleLatestTasksPulling.class.getName());
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String numOfPagesParam = req.getParameter("numOfPages");
    Integer numOfPages = numOfPagesParam == null ? 1 : Integer.valueOf(numOfPagesParam);
    String sortType = req.getParameter("sortType");
    String sortDirection = req.getParameter("sortDirection");
    long timepause = 2500;// We put a spacing of 2.5 seconds between requests.

    for(int i = 1; i <= numOfPages; i++) {
       Map<String, String> params = new HashMap<String, String>();
       params.put("pageNumber", String.valueOf(i));
       if(sortType != null){
           params.put("sortType", sortType);
       }

       if(sortDirection != null){
           params.put("sortDirection", sortDirection);
       }

       TaskUtil.queueTask("/pullLatestTasks", "pullLatestTasks1", i*timepause, params);
    }
  }
  
}
