package com.tracker.endpoints;

import static com.tracker.ofy.OfyService.ofy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.Nullable;
import com.googlecode.objectify.cmd.Query;
import com.tracker.entity.HITinstance;

@Api(name = "mturk", description = "The API for mturk-tracker", version = "v1")
public class HitInstanceEndpoint {

    private SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

	@ApiMethod(name = "hitinstance.list", path = "hitinstance/list", httpMethod = HttpMethod.GET)
	public List<HITinstance> list(
	        @Named("groupId") String groupId,
            @Nullable @Named("from") String from,
            @Nullable @Named("to") String to) throws ParseException {

	    Query<HITinstance> query = ofy().load().type(HITinstance.class).filter("groupId", groupId);

	    if(StringUtils.isNotEmpty(from) && StringUtils.isNotEmpty(to)) {
	        Calendar dateFrom = Calendar.getInstance();
	        dateFrom.setTime(formatter.parse(from));
	        dateFrom.set(Calendar.HOUR_OF_DAY, 0);
	        dateFrom.set(Calendar.MINUTE, 0);
	        dateFrom.set(Calendar.SECOND, 0);

	        Calendar dateTo = Calendar.getInstance();
	        dateTo.setTime(formatter.parse(to));
	        dateTo.set(Calendar.HOUR_OF_DAY, 23);
	        dateTo.set(Calendar.MINUTE, 59);
	        dateTo.set(Calendar.SECOND, 59);

	        query = query.filter("timestamp >=", dateFrom.getTime()).filter("timestamp <=", dateTo.getTime());
	    }
		return query.list();
	}

}
