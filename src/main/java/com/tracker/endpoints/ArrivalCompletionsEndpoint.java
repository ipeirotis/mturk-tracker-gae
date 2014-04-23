package com.tracker.endpoints;

import static com.tracker.ofy.OfyService.ofy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.tracker.entity.ArrivalCompletions;

@Api(name = "mturk", description = "The API for mturk-tracker", version = "v1")
public class ArrivalCompletionsEndpoint {

	private SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

	@ApiMethod(name = "arrivalCompletions.list", path = "arrivalCompletions/list", httpMethod = HttpMethod.GET)
	public List<ArrivalCompletions> list(@Named("from") String from,
			@Named("to") String to) throws ParseException {
		return ofy().load().type(ArrivalCompletions.class)
				.filter("from >=", formatter.parse(from)).filter("from <", formatter.parse(to)).list();
	}

}
