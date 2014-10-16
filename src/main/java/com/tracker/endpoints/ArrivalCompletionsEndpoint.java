package com.tracker.endpoints;

import static com.tracker.ofy.OfyService.ofy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.tracker.entity.ArrivalCompletions;

@Api(name = "mturk", description = "The API for mturk-tracker", version = "v1")
public class ArrivalCompletionsEndpoint {
    private MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
	private SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

	@SuppressWarnings("unchecked")
    @ApiMethod(name = "arrivalCompletions.list", path = "arrivalCompletions/list", httpMethod = HttpMethod.GET)
	public List<ArrivalCompletions> list(@Named("from") String from,
	        @Named("to") String to) throws ParseException {

	    Calendar dateFrom = Calendar.getInstance();
	    dateFrom.setTime(formatter.parse(from));
	    dateFrom.set(Calendar.HOUR_OF_DAY, 0);
	    dateFrom.set(Calendar.MINUTE, 0);
	    dateFrom.set(Calendar.SECOND, 0);
	    dateFrom.set(Calendar.MILLISECOND, 0);

	    Calendar dateTo = Calendar.getInstance();
	    dateTo.setTime(formatter.parse(to));
	    dateTo.set(Calendar.HOUR_OF_DAY, 23);
	    dateTo.set(Calendar.MINUTE, 59);
	    dateTo.set(Calendar.SECOND, 59);
	    dateTo.set(Calendar.MILLISECOND, 999);

	    String memcacheKey = "arrival_completions_" + dateFrom.getTime() +
	            "_" + dateTo.getTime();

	    if (memcacheService.contains(memcacheKey)){
	        return (List<ArrivalCompletions>) memcacheService.get(memcacheKey);
	    } else {
	        return ofy().load().type(ArrivalCompletions.class)
	                .filter("from >=", dateFrom.getTime())
	                .filter("from <=", dateTo.getTime()).list();
	    }
	}

}
