package com.tracker.endpoints;

import static com.tracker.ofy.OfyService.ofy;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.tracker.entity.HITrequester;
import com.tracker.entity.TopRequester;

@Api(name = "mturk", description = "The API for mturk-tracker", version = "v1")
public class RequesterEndpoint {

    @ApiMethod(name = "requester.get", path = "requester", httpMethod = HttpMethod.GET)
    public HITrequester get(@Named("requesterId") String requesterId) {
        return ofy().load().type(HITrequester.class).id(requesterId).now();
    }

	@ApiMethod(name = "toprequester.list", path = "toprequester/list", httpMethod = HttpMethod.GET)
	public List<TopRequester> listTopRequesters() {
	    Calendar cal = Calendar.getInstance();
	    cal.setTime(new Date());
	    cal.set(Calendar.HOUR_OF_DAY, 0);
	    cal.set(Calendar.MINUTE, 0);
	    cal.set(Calendar.SECOND, 0);
	    cal.set(Calendar.MILLISECOND, 0);
	    cal.add(Calendar.DATE, -30);//last 30 days

	    return ofy().load().type(TopRequester.class).filter("timestamp >=", cal.getTime())
	            .order("timestamp").order("-reward").limit(100).list();
	}

}
