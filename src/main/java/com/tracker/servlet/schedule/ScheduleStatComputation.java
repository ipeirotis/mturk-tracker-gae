package com.tracker.servlet.schedule;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tracker.util.TaskUtil;

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
	    for(long t = cal.getTimeInMillis(); t<cal.getTimeInMillis() + DAY_IN_MILLIS; t+=duration*MIN_IN_MILLIS) {	    
	        Map<String, String> params = new HashMap<String, String>();
	        params.put("diff", duration_param);
	        params.put("from", String.valueOf(t));
	        params.put("to", String.valueOf(t + duration*MIN_IN_MILLIS));
	        TaskUtil.queueTask("/computeArrivalCompletions", params);
	    }
	}

}
