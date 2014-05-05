package com.tracker.servlet.schedule;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Builder;
import com.googlecode.objectify.Key;
import com.tracker.entity.HITgroup;

@SuppressWarnings("serial")
public class ScheduleHITsTracking extends HttpServlet {

  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(ScheduleHITsTracking.class.getName());

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    // Get the list of all HITgroups that are active.
    Iterable<Key<HITgroup>> groups = ofy().load().type(HITgroup.class).filter("active", true).keys();
    for (Key<HITgroup> group : groups) {
      schedule(group.getRaw().getName());
    }
  }

  private void schedule(String groupId) {
    Queue queue = QueueFactory.getQueue("trackHITs");
    queue.add(Builder
        .withUrl("/trackHits")
        .param("groupId", groupId)
        .etaMillis(System.currentTimeMillis())
        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(0))
        .method(TaskOptions.Method.GET));
  }

}
