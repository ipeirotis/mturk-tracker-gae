package com.tracker.servlet.schedule;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Builder;

@SuppressWarnings("serial")
public class ScheduleStatComputation extends HttpServlet {
	
	private static final long DAY_IN_MILLIS = 1000*60*60*24;
	private static final long MIN_IN_MILLIS = 60*1000;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	    
	    String date_param = req.getParameter("date");
	    Integer date = Integer.parseInt(date_param);

	    String duration_param = req.getParameter("duration");
	    Integer duration = Integer.parseInt(duration_param);
	    
	    Calendar cal = Calendar.getInstance();
	    cal.setTime(new Date());
	    cal.set(Calendar.HOUR_OF_DAY, 0);
	    cal.set(Calendar.MINUTE, 0);
	    cal.set(Calendar.SECOND, 0);
	    cal.set(Calendar.MILLISECOND, 0);
	    cal.add(Calendar.DATE, -date);
	    for(long t = cal.getTimeInMillis(); t<cal.getTimeInMillis() + DAY_IN_MILLIS; t+=duration*MIN_IN_MILLIS){
		    scheduleTask(duration_param, t, t + duration*MIN_IN_MILLIS);
	    }
	}
	
	private void scheduleTask(String duration, long from, long to){
	    Queue queue = QueueFactory.getDefaultQueue();
	    queue.add(Builder
        .withUrl("/computeArrivalCompletions")
        .param("diff", duration)
	    .param("from", String.valueOf(from))
	    .param("to", String.valueOf(to))
        .etaMillis(System.currentTimeMillis())
        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(1))
        .method(TaskOptions.Method.GET));
	}

}
