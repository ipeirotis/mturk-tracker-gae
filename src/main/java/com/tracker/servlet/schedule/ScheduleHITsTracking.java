package com.tracker.servlet.schedule;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.cmd.Query;
import com.tracker.entity.HITgroup;
import com.tracker.util.TaskUtil;

@SuppressWarnings("serial")
public class ScheduleHITsTracking extends HttpServlet {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ScheduleHITsTracking.class.getName());

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // Get the list of all HITgroups that are active.
        Query<HITgroup> q = ofy().load().type(HITgroup.class)
                .filter("active", true);
        Cursor cursor = null;

        while (true) {
            if (cursor != null) {
                q = q.startAt(cursor);
            }

            boolean continu = false;
            QueryResultIterator<HITgroup> iterator = q.iterator();
            cursor = iterator.getCursor();

            while (iterator.hasNext()) {
                HITgroup group = iterator.next();
                Map<String, String> params = new HashMap<String, String>();
                params.put("groupId", group.getGroupId());
                TaskUtil.queueTask("/trackHits", "trackHITs1", params);
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
    }

}
