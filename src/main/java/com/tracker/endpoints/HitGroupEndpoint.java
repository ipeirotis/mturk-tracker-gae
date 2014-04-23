package com.tracker.endpoints;

import static com.tracker.ofy.OfyService.ofy;

import java.util.List;

import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.tracker.entity.HITgroup;

@Api(name = "mturk", description = "The API for mturk-tracker", version = "v1")
public class HitGroupEndpoint {

	@ApiMethod(name = "hitgroup.getByGroupId", path = "hitgroup/getByGroupId", httpMethod = HttpMethod.GET)
	public HITgroup getByGroupId(@Named("groupId") String groupId) {
		return ofy().load().type(HITgroup.class).id(groupId).now();
	}
	
	@ApiMethod(name = "hitgroup.listByRequesterId", path = "hitgroup/listByRequesterId", httpMethod = HttpMethod.GET)
	public List<HITgroup> listByRequesterId(@Named("requesterId") String requesterId) {
		return ofy().load().type(HITgroup.class).filter("requesterId", requesterId).list();
	}

}
