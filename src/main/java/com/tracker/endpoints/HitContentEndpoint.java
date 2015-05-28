package com.tracker.endpoints;

import static com.tracker.ofy.OfyService.ofy;

import java.util.logging.Logger;
import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.tracker.entity.HITcontent;

@Api(name = "mturk", description = "The API for mturk-tracker", version = "v1")
public class HitContentEndpoint {

	@SuppressWarnings("unused")
	private static final Logger logger = Logger
			.getLogger(HitContentEndpoint.class.getName());

	@ApiMethod(name = "hitcontent.getByGroupId", path = "hitcontent/getByGroupId", httpMethod = HttpMethod.GET)
	public HITcontent getByGroupId(@Named("groupId") String groupId) {
		return ofy().load().type(HITcontent.class).id(groupId).now();
	}

}
