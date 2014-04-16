package com.tracker.servlet;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Builder;
import com.tracker.entity.HITgroup;
import com.tracker.entity.HITinstance;

@SuppressWarnings("serial")
public class TrackHITsServlet extends HttpServlet {
  
  private static final Logger logger = Logger.getLogger(TrackHITsServlet.class.getName());
  private static final String PREVIEW_URL = "https://www.mturk.com/mturk/preview?groupId=";
  private static final String NOT_AVAILABLE_ALERT = "There are no more available HITs in this group";
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    
    String schedule = req.getParameter("schedule");
    
    if("true".equals(schedule)){
      schedule();
      return;
    } else {
      try {
        loadAndParse();
      } catch (Exception e) {
        //TODO:
        e.printStackTrace();
        logger.severe(e.getMessage());
      }
    }
  }
  
  private void loadAndParse(){
    List<HITgroup> groups = ofy().load().type(HITgroup.class).filter("expirationDate >", new Date()).list();
    
    for(HITgroup group : groups){
      Document doc = null;
      try {
        doc = Jsoup.connect(PREVIEW_URL + group.getGroupId()).get();
      } catch (Exception e) {
        continue;
      }
      Element alertboxHeader = doc.getElementById("alertboxHeader");
      if(alertboxHeader != null && alertboxHeader.text().startsWith(NOT_AVAILABLE_ALERT)){
        //skip
      } else {
        Element hitElement = doc.select("a:matchesOwn(HITs Available:+)").first();
        System.out.println(hitElement);
        String hits = hitElement.parent().nextElementSibling().text();
        
        ofy().save().entity(new HITinstance(group.getGroupId(), new Date(), Integer.parseInt(hits)));
      }
    }
  }
  
  private void schedule(){
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(Builder
        .withUrl("/trackHits")
        .etaMillis(System.currentTimeMillis())
        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(1))
        .method(TaskOptions.Method.GET));
  }

}
