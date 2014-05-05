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
import com.tracker.entity.ArrivalCompletions;

@Api(name = "mturk", description = "The API for mturk-tracker", version = "v1")
public class ArrivalCompletionsEndpoint {

	private SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

	@ApiMethod(name = "arrivalCompletions.list", path = "arrivalCompletions/list", httpMethod = HttpMethod.GET)
	public List<ArrivalCompletions> list(@Named("from") String from,
			@Named("to") String to) throws ParseException {
		Calendar dateTo = Calendar.getInstance();
		dateTo.setTime(formatter.parse(to));
		dateTo.set(Calendar.HOUR_OF_DAY, 23);
		dateTo.set(Calendar.MINUTE, 59);
		dateTo.set(Calendar.SECOND, 59);
		return ofy().load().type(ArrivalCompletions.class)
				.filter("from >=", formatter.parse(from)).filter("from <=", dateTo.getTime()).list();
	}

}
