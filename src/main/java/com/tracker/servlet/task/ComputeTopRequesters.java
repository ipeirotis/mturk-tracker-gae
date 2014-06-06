package com.tracker.servlet.task;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.cmd.Query;
import com.tracker.entity.HITgroup;
import com.tracker.entity.HITinstance;
import com.tracker.entity.TopRequester;

@SuppressWarnings("serial")
public class ComputeTopRequesters extends HttpServlet {
    
  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(ComputeTopRequesters.class.getName());
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
      
      String requesterId = req.getParameter("requesterId");
      String fromParam = req.getParameter("from");
      Date from = new Date(Long.parseLong(fromParam));
      
      List<HITgroup> groups = ofy().load().type(HITgroup.class)
              .filter("expirationDate >", from).filter("requesterId", requesterId).list();
      
      Integer hitsArrived = 0;
      Integer rewardsArrived = 0;
      String requesterName = null;
      
      for(HITgroup group : groups) {
          requesterName = group.getRequesterName();
          List<HITinstance> instances = getHitInstances(group.getGroupId());
          for(HITinstance inst : instances) {
              if(inst.getHitsDiff() != null) {
                  if(inst.getHitsDiff() > 0){
                    hitsArrived+= Math.abs(inst.getHitsDiff());
                  }
              }
              if(inst.getRewardDiff() != null) {
                 if(inst.getRewardDiff() > 0){
                    rewardsArrived += Math.abs(inst.getRewardDiff());
                 }
              }
          }
      }
      TopRequester topRequester = new TopRequester(requesterId, requesterName, hitsArrived, rewardsArrived, 
              0, from);
      
      ofy().save().entity(topRequester);
  }
  
  public List<HITinstance> getHitInstances(String groupId) {
      List<HITinstance> result = new ArrayList<HITinstance>();
      Query<HITinstance> q = ofy().load().type(HITinstance.class).filter("groupId", groupId);
      Cursor cursor = null;

      while (true) {
          if (cursor != null) {
              q = q.startAt(cursor);
          }

          boolean continu = false;
          QueryResultIterator<HITinstance> iterator = q.iterator();
          cursor = iterator.getCursor();

          while (iterator.hasNext()) {
              HITinstance instance = iterator.next();
              result.add(instance);
              continu = true;
          }

          if (continu) {
              cursor = iterator.getCursor();
              if (cursor == null) {
                  break;
              }
          } else {
              break;
          }
      }
      return result;
  }

}
