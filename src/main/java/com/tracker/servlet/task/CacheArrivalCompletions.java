package com.tracker.servlet.task;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.tracker.entity.ArrivalCompletions;

@SuppressWarnings("serial")
public class CacheArrivalCompletions extends HttpServlet {
  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(CacheArrivalCompletions.class.getName());

  private MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
      
      Calendar dateFrom = Calendar.getInstance();
      dateFrom.setTime(new Date());
      dateFrom.set(Calendar.HOUR_OF_DAY, 0);
      dateFrom.set(Calendar.MINUTE, 0);
      dateFrom.set(Calendar.SECOND, 0);
      dateFrom.set(Calendar.MILLISECOND, 0);
      dateFrom.add(Calendar.MONTH, -1);

      Calendar dateTo = Calendar.getInstance();
      dateTo.setTime(new Date());
      dateTo.set(Calendar.HOUR_OF_DAY, 23);
      dateTo.set(Calendar.MINUTE, 59);
      dateTo.set(Calendar.SECOND, 59);
      dateTo.set(Calendar.SECOND, 59);
      dateTo.set(Calendar.MILLISECOND, 999);
      
      String memcacheKey = "arrival_completions_" + dateFrom.getTime() + 
              "_" + dateTo.getTime();

      List<ArrivalCompletions> list = ofy().load().type(ArrivalCompletions.class)
              .filter("from >=", dateFrom.getTime())
              .filter("from <=", dateTo.getTime()).list();
      memcacheService.put(memcacheKey, list);
  }

}
