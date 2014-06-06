package com.tracker.servlet.schedule;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Builder;
import com.googlecode.objectify.cmd.Query;
import com.tracker.entity.HITgroup;

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

        Set<String> requesterIds = getRequesterIds(cal);
        for (String id : requesterIds) {
            schedule(id, cal.getTimeInMillis());
        }
    }

    private void schedule(String requesterId, long from) {
        Queue queue = QueueFactory.getDefaultQueue();
        queue.add(Builder
            .withUrl("/computeTopRequesters")
            .param("requesterId", requesterId)
            .param("from", String.valueOf(from))
            .etaMillis(System.currentTimeMillis())
            .retryOptions(RetryOptions.Builder.withTaskRetryLimit(0))
            .method(TaskOptions.Method.GET));
    }
    
    public Set<String> getRequesterIds(Calendar cal) {
        Set<String> result = new HashSet<String>();
        Query<HITgroup> q = ofy().load().type(HITgroup.class).filter("expirationDate >", cal.getTime());
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
                if(group.getRequesterId() != null) {
                    result.add(group.getRequesterId());
                }
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
