package com.tracker.servlet.schedule;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.cmd.Query;
import com.tracker.entity.HITrequester;
import com.tracker.util.TaskUtil;

@SuppressWarnings("serial")
public class ScheduleTopRequestersComputation extends HttpServlet {
  
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ScheduleTopRequestersComputation.class.getName());
    
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, -30); //last 30 days

        List<HITrequester> requesters = getRequesters(cal);
        for (HITrequester requester : requesters) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("requesterId", requester.getRequesterId());
            params.put("requesterName", requester.getRequesterName());
            params.put("from", String.valueOf(cal.getTimeInMillis()));
            TaskUtil.queueTask("/computeTopRequesters", params);
        }
    }
    
    public List<HITrequester> getRequesters(Calendar cal) {
        List<HITrequester> result = new ArrayList<HITrequester>();
        Query<HITrequester> q = ofy().load().type(HITrequester.class).filter("lastActivity >", cal.getTime());
        Cursor cursor = null;

        while (true) {
            if (cursor != null) {
                q = q.startAt(cursor);
            }

            boolean continu = false;
            QueryResultIterator<HITrequester> iterator = q.iterator();
            cursor = iterator.getCursor();

            while (iterator.hasNext()) {
                HITrequester requester = iterator.next();
                result.add(requester);
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
