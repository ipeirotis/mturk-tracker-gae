package com.tracker.servlet.schedule;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Builder;

@SuppressWarnings("serial")
public class ScheduleLatestTasksPulling extends HttpServlet {
  
  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(ScheduleLatestTasksPulling.class.getName());
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    String numOfPagesParam = req.getParameter("numOfPages");
    Integer numOfPages = numOfPagesParam == null ? 1 : Integer.valueOf(numOfPagesParam);
    String sortType = req.getParameter("sortType");
    String sortDirection = req.getParameter("sortDirection");

    for(int i = 1; i <= numOfPages; i++){
       schedule(i, sortType, sortDirection);
    }
  }
  
  private void schedule(Integer pageNumber, String sortType, String sortDirection){
    Queue queue = QueueFactory.getQueue("pullLatestTasks");
    
    // We put a spacing of 2.5 seconds between requests.
    int timepause = 2500;
    TaskOptions taskOptions = Builder
        .withUrl("/pullLatestTasks")
        .param("pageNumber", String.valueOf(pageNumber))
        .etaMillis(System.currentTimeMillis() + pageNumber*timepause)
        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(1))
        .method(TaskOptions.Method.GET);
    
    if(sortType != null){
        taskOptions = taskOptions.param("sortType", sortType);
    }
    
    if(sortDirection != null){
        taskOptions = taskOptions.param("sortDirection", sortDirection);
    }
    
    queue.add(taskOptions);
  }
  
}
