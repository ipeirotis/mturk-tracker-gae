package com.tracker.endpoints;

import static com.tracker.ofy.OfyService.ofy;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.googlecode.objectify.Key;
import com.tracker.entity.HITgroup;

@Api(name = "mturk", description = "The API for mturk-tracker", version = "v1")
public class HitGroupEndpoint {

	@ApiMethod(name = "hitgroup.getByGroupId", path = "hitgroup/getByGroupId", httpMethod = HttpMethod.GET)
	public HITgroup getByGroupId(@Named("groupId") String groupId) {
		return ofy().load().type(HITgroup.class).id(groupId).now();
	}
	
	@ApiMethod(name = "hitgroup.listByRequesterId", path = "hitgroup/listByRequesterId", httpMethod = HttpMethod.GET)
	public List<String> listByRequesterId(@Named("requesterId") String requesterId) {
	  List<String> result = new ArrayList<String>();
	  Iterable<Key<HITgroup>> iterator = ofy().load().type(HITgroup.class).filter("requesterId", requesterId).keys();
	  for (Key<HITgroup> k : iterator) {
	    result.add(k.getRaw().getName());
	  }
		return result;
	}

}
