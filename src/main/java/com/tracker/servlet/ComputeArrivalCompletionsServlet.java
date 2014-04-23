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
    if("true".equals(schedule)){
      schedule();
      return;
    } else {
      compute();
    }
  }
  
  private void compute(){
    Calendar from = Calendar.getInstance();
    from.setTime(new Date());
    from.set(Calendar.MINUTE, 0);
    from.set(Calendar.SECOND, 0);
    from.set(Calendar.MILLISECOND, 0);
    
    Calendar to = Calendar.getInstance();
    to.setTime(new Date());
    to.set(Calendar.MINUTE, 59);
    to.set(Calendar.SECOND, 59);
    from.set(Calendar.MILLISECOND, 999);
    
    List<HITgroup> arrivedGroups = ofy().load().type(HITgroup.class)
        .filter("firstSeen >", from).filter("firstSeen <", to).list();
    
    int hitGroupsArrived = arrivedGroups.size();
    
    List<HITgroup> completedGroups = ofy().load().type(HITgroup.class)
        .filter("lastSeen >", from).filter("lastSeen <", to).list();
    
    int hitGroupsCompleted = completedGroups.size();
    
    List<HITinstance> hitInstances = ofy().load().type(HITinstance.class)
        .filter("timestamp >", from).filter("timestamp <", to).list();
    
    int hitsArrived = 0;
    int hitsCompleted = 0;
    int rewardsArrived = 0;
    int rewardsCompleted = 0;
  
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

  private void schedule(){
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(Builder
        .withUrl("/computeArrivalCompletions")
        .etaMillis(System.currentTimeMillis())
        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(1))
        .method(TaskOptions.Method.GET));
  }

}
