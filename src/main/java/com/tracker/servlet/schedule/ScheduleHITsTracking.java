package com.tracker.servlet.schedule;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.objectify.Key;
import com.tracker.entity.HITgroup;
import com.tracker.util.TaskUtil;

@SuppressWarnings("serial")
public class ScheduleHITsTracking extends HttpServlet {

  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(ScheduleHITsTracking.class.getName());

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    // Get the list of all HITgroups that are active.
    Iterable<Key<HITgroup>> groups = ofy().load().type(HITgroup.class).filter("active", true).keys();
    for (Key<HITgroup> group : groups) {
      Map<String, String> params = new HashMap<String, String>();
      params.put("groupId", group.getRaw().getName());
      TaskUtil.queueTask("/trackHits", "trackHITs1", params);
    }
  }

}
