package com.tracker.servlet.task;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.QueryKeys;
import com.tracker.entity.ArrivalCompletions;
import com.tracker.entity.HITgroup;
import com.tracker.entity.HITinstance;
import com.tracker.entity.MarketStatistics;

@SuppressWarnings("serial")
public class ComputeArrivalCompletions extends HttpServlet {
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    
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

    compute(from, to, diff);
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
  
  private void compute(Calendar from, Calendar to, Integer diff){
    
    List<Key<HITgroup>> arrivedGroups = ofy().load().type(HITgroup.class)
        .filter("firstSeen >=", from.getTime())
        .filter("firstSeen <", to.getTime())
        .keys().list();
    
    Long hitGroupsArrived = new Long(arrivedGroups.size());
    
    List<Key<HITgroup>> completedGroups = ofy().load().type(HITgroup.class)
        .filter("lastSeen >=", from.getTime())
        .filter("lastSeen <", to.getTime())
        .filter("active", Boolean.FALSE)
        .keys().list();
    
    Long hitGroupsCompleted = new Long(completedGroups.size());
    
    List<HITinstance> hitInstances = ofy().load().type(HITinstance.class)
        .filter("timestamp >=", from.getTime())
        .filter("timestamp <", to.getTime())
        .list();
    
    Long hitsArrived = 0L;
    Long hitsCompleted = 0L;
    Long rewardsArrived = 0L;
    Long rewardsCompleted = 0L;
    

    
    //HashMap<String, ArrayList<Integer>> hitsAvailablePerGroup = new HashMap<String, ArrayList<Integer>>();
    //HashMap<String, ArrayList<Integer>> rewardsAvailablePerGroup = new HashMap<String, ArrayList<Integer>>();
    
    
    for(HITinstance inst : hitInstances){
      //addHITsAvailable(hitsAvailablePerGroup, inst);
      //addRewardsAvailable(rewardsAvailablePerGroup, inst);
      
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
    
    /*
    Integer hitGroupsAvailable = hitsAvailablePerGroup.keySet().size();
    
    Double hitsAvailable = 0.0;
    for (String groupid : hitsAvailablePerGroup.keySet()) {
      ArrayList<Integer> entries = hitsAvailablePerGroup.get(groupid);
      int size = entries.size();
      double sum = 0;
      for (Integer i : entries) {
        sum += i;
      }
      hitsAvailable += sum/size; 
    }
    
    Double rewardsAvailable = 0.0;
    for (String groupid : rewardsAvailablePerGroup.keySet()) {
      ArrayList<Integer> entries = rewardsAvailablePerGroup.get(groupid);
      int size = entries.size();
      double sum = 0;
      for (Integer i : entries) {
        sum += i;
      }
      rewardsAvailable += sum/size; 
    }
    */
    
    List<MarketStatistics> statsInstances = ofy().load().type(MarketStatistics.class)
        .filter("timestamp >=", from.getTime())
        .filter("timestamp <", to.getTime())
        .list();
    
    Double hitGroupsAvailableUI = 0.0;
    Double hitsAvailableUI = 0.0;
    
    if (statsInstances.size()>0) {
      for(MarketStatistics stat : statsInstances){
        hitGroupsAvailableUI += stat.getHitGroupsAvailable();
        hitsAvailableUI += stat.getHitsAvailable();
      }
      hitGroupsAvailableUI = hitGroupsAvailableUI / statsInstances.size();
      hitsAvailableUI = hitsAvailableUI / statsInstances.size();
    } else {
      // If there are no market statistics, then we should not be computing stats
      return;
    }
    
    ArrivalCompletions existing = ofy().load().type(ArrivalCompletions.class)
            .filter("from", from.getTime()).filter("to", to.getTime()).limit(1).first().now();
    if (existing != null) {
      ofy().delete().entity(existing);
    }
    
    ArrivalCompletions ac = new ArrivalCompletions(from.getTime(), to.getTime());
    ac.setHitGroupsArrived(hitGroupsArrived);
    //ac.setHitGroupsAvailable(hitGroupsAvailable);
    ac.setHitGroupsAvailableUI(hitGroupsAvailableUI);
    ac.setHitGroupsCompleted(hitGroupsCompleted);
    ac.setHitsArrived(hitsArrived);
    //ac.setHitsAvailable(hitsAvailable);
    ac.setHitsAvailableUI(hitsAvailableUI);
    ac.setHitsCompleted(hitsCompleted);
    ac.setRewardsArrived(rewardsArrived);
    //ac.setRewardsAvailable(rewardsAvailable);
    ac.setRewardsCompleted(rewardsCompleted);
    ac.setLength(diff);
    
    ofy().save().entity(ac);
  }
  

  private void addRewardsAvailable(HashMap<String, ArrayList<Integer>> rewardsAvailablePerGroup, HITinstance inst) {
    ArrayList<Integer> groupRewardsAvailable;
    if (rewardsAvailablePerGroup.containsKey(inst.getGroupId())) {
      groupRewardsAvailable = rewardsAvailablePerGroup.get(inst.getGroupId());
    } else {
      groupRewardsAvailable = new ArrayList<Integer>();        
    }
    Integer reward = inst.getRewardsAvailable();
    groupRewardsAvailable.add(reward);
    rewardsAvailablePerGroup.put(inst.getGroupId(), groupRewardsAvailable);
  }

  private void addHITsAvailable(HashMap<String, ArrayList<Integer>> hitsAvailablePerGroup, HITinstance inst) {
    ArrayList<Integer> groupHitsAvailable;
    if (hitsAvailablePerGroup.containsKey(inst.getGroupId())) {
      groupHitsAvailable = hitsAvailablePerGroup.get(inst.getGroupId());
    } else {
      groupHitsAvailable = new ArrayList<Integer>();        
    }
    Integer hits = inst.getHitsAvailable();
    groupHitsAvailable.add(hits);
    hitsAvailablePerGroup.put(inst.getGroupId(), groupHitsAvailable);
  }

}
