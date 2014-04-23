package com.tracker.servlet;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Builder;
import com.tracker.entity.ArrivalCompletions;
import com.tracker.entity.HITgroup;
import com.tracker.entity.HITinstance;

@SuppressWarnings("serial")
public class ComputeArrivalCompletionsServlet extends HttpServlet {
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    String schedule = req.getParameter("schedule");
    
    // We can specify either a "from" and "to" timestamps
    // or we can specify a "diff" in minutes
    
    // The "to" is either passed as a parameter, or if ommitted is set to "now"
    String to_param = req.getParameter("to");
    Calendar to = getToTime(to_param);
    
    // This is an optional parameters that shows how much back we should look (in minutes)
    // If the "diff" is not specified, then "diff" is set by default to 60 minutes (1 hour)
    String diff_param = req.getParameter("diff");
    Integer diff = getDiffTime(diff_param);
    
    // If the "from" is not passed as a parameter, it is set to: "to"-"diff"
    String from_param = req.getParameter("from");
    Calendar from = getFromTime(to, diff, from_param);

    if("true".equals(schedule)){
      schedule(from, to);
      return;
    } else {
      compute(from, to);
    }
  }

  private Integer getDiffTime(String diff_param) {
    if (diff_param == null) {
      diff_param = "60";
    }
    Integer diff = null;
    try {
      diff = Integer.parseInt(diff_param);
    } catch (Exception e) {
      diff = 60;
    }
    return diff;
  }

  private Calendar getFromTime(Calendar to, Integer diff, String from_param) {
    Calendar from = Calendar.getInstance();
    
    if (from_param==null) {
      from.setTimeInMillis(to.getTimeInMillis());
      from.add(Calendar.MINUTE, -diff);
    } else {
      Long from_timestamp = Long.parseLong(from_param);
      from.setTimeInMillis(from_timestamp);
    }
    return from;
  }

  private Calendar getToTime(String to_param) {
    Calendar to = Calendar.getInstance();
    
    if (to_param==null) {
      to.setTime(new Date());
    } else {
      Long to_timestamp = Long.parseLong(to_param);
      to.setTimeInMillis(to_timestamp);
    }
    return to;
  }
  
  private void compute(Calendar from, Calendar to){
    
    List<HITgroup> arrivedGroups = ofy().load().type(HITgroup.class)
        .filter("firstSeen >", from.getTime())
        .filter("firstSeen <", to.getTime())
        .list();
    
    Long hitGroupsArrived = new Long(arrivedGroups.size());
    
    List<HITgroup> completedGroups = ofy().load().type(HITgroup.class)
        .filter("lastSeen >", from.getTime())
        .filter("lastSeen <", to.getTime())
        .filter("active", Boolean.FALSE)
        .list();
    
    Long hitGroupsCompleted = new Long(completedGroups.size());
    
    List<HITinstance> hitInstances = ofy().load().type(HITinstance.class)
        .filter("timestamp >", from.getTime())
        .filter("timestamp <", to.getTime())
        .list();
    
    Long hitsArrived = 0L;
    Long hitsCompleted = 0L;
    Long rewardsArrived = 0L;
    Long rewardsCompleted = 0L;
  
    for(HITinstance inst : hitInstances){
      if(inst.getHitsDiff() != null) {
        if(inst.getHitsDiff() > 0){
          hitsArrived+= Math.abs(inst.getHitsDiff());
        } else {
          hitsCompleted+= Math.abs(inst.getHitsDiff());
        }
      }
      if(inst.getRewardDiff() != null) {
        if(inst.getRewardDiff() > 0){
          rewardsArrived += Math.abs(inst.getRewardDiff());
        } else {
          rewardsCompleted += Math.abs(inst.getRewardDiff());
        }
      }
    }
    
    ofy().save().entity(new ArrivalCompletions(from.getTime(), to.getTime(), hitGroupsArrived, 
        hitGroupsCompleted, hitsArrived, hitsCompleted, rewardsArrived, rewardsCompleted));
  }

  private void schedule(Calendar from, Calendar to){
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(Builder
        .withUrl("/computeArrivalCompletions")
        .param("from", Long.toString(from.getTimeInMillis()))
        .param("to", Long.toString(to.getTimeInMillis()))
        .etaMillis(System.currentTimeMillis())
        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(1))
        .method(TaskOptions.Method.GET));
  }

}
