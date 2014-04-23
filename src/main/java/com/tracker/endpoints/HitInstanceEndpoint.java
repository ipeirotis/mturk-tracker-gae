package com.tracker.endpoints;

import static com.tracker.ofy.OfyService.ofy;

import java.util.List;

import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.tracker.entity.HITinstance;

@Api(name = "mturk", description = "The API for mturk-tracker", version = "v1")
public class HitInstanceEndpoint {

	@ApiMethod(name = "hitinstance.getByGroupId", path = "hitinstance/listByGroupId", httpMethod = HttpMethod.GET)
	public List<HITinstance> listByGroupId(@Named("groupId") String groupId) {
		return ofy().load().type(HITinstance.class).filter("getByGroupId", groupId).list();
	}

}
