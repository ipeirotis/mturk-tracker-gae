package com.tracker.endpoints;

import static com.tracker.ofy.OfyService.ofy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.Nullable;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.Query;
import com.google.appengine.api.search.QueryOptions;
import com.google.appengine.api.search.ScoredDocument;
import com.google.appengine.api.search.SearchServiceFactory;
import com.tracker.entity.HITgroup;

@Api(name = "mturk", description = "The API for mturk-tracker", version = "v1")
public class HitGroupEndpoint {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(HitGroupEndpoint.class.getName());

	@ApiMethod(name = "hitgroup.getByGroupId", path = "hitgroup/getByGroupId", httpMethod = HttpMethod.GET)
	public HITgroup getByGroupId(@Named("groupId") String groupId) {
		return ofy().load().type(HITgroup.class).id(groupId).now();
	}

	@ApiMethod(name = "hitgroup.listByRequesterId", path = "hitgroup/listByRequesterId", httpMethod = HttpMethod.GET)
	public List<HITgroup> listByRequesterId(
	        @Named("requesterId") String requesterId,
	        @Nullable @Named("days") Integer days) {
	    com.googlecode.objectify.cmd.Query<HITgroup> query =
	            ofy().load().type(HITgroup.class).filter("requesterId", requesterId);

	    if(days != null) {
	        Calendar cal = Calendar.getInstance();
	        cal.setTime(new Date());
	        cal.set(Calendar.HOUR_OF_DAY, 0);
	        cal.set(Calendar.MINUTE, 0);
	        cal.set(Calendar.SECOND, 0);
	        cal.set(Calendar.MILLISECOND, 0);
	        cal.add(Calendar.DATE, -days);

	        query = query.filter("lastSeen >", cal.getTime());
	    }

	    return query.list();
	}

	@ApiMethod(name = "hitgroup.search", path = "hitgroup/search", httpMethod = HttpMethod.GET)
	public List<Map<String, String>> search(
	        @Nullable @Named("all") String all,
	        @Nullable @Named("requesterName") String requesterName,
	        @Nullable @Named("titleT") String title,
	        @Nullable @Named("description") String description,
	        @Nullable @Named("hitContent") String hitContent,
	        @Nullable @Named("keyword") String keyword,
	        @Nullable @Named("qualification") String qualification) {

	    List<String> params = new ArrayList<String>();
	    String queryString;
	    if(all != null) {
	        for(String param : Arrays.asList("requesterName", "title", "description", "hitContent", "keywords", "qualifications")) {
	            params.add(param + "=" + all);
	        }
	        queryString = StringUtils.join(params, " OR ");
	    } else {
	        if(requesterName != null) {
	            params.add("requesterName=" + requesterName);
	        }
	        if(title != null) {
	            params.add("title=" + title);
	        }
	        if(description != null) {
	            params.add("description=" + description);
	        }
	        if(hitContent != null) {
	            params.add("hitContent=" + hitContent);
	        }
	        if(keyword != null) {
	            params.add("keywords=" + keyword);
	        }
	        if(qualification != null) {
	            params.add("qualifications=" + qualification);
	        }
	        queryString = StringUtils.join(params, " AND ");
	    }

	    IndexSpec indexSpec = IndexSpec.newBuilder().setName("hit_group_index").build();
	    Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);
	    QueryOptions options = QueryOptions.newBuilder()
	            .setFieldsToReturn("requesterId", "requesterName", "title", "description", "keywords", "reward", "timeAllotted")
	            .setLimit(1000).build();
	    Query query = Query.newBuilder().setOptions(options).build(queryString);
	    Collection<ScoredDocument> docs = index.search(query).getResults();
	    
	    List<Map<String, String>> result = new ArrayList<Map<String, String>>();
	    
	    for(ScoredDocument doc : docs) {
	        Map<String, String> fields = new HashMap<String, String>();
	        for (Field field : doc.getFields()){
	            fields.put(field.getName(), field.getText());
	        }
	        fields.put("id", doc.getId());
	        result.add(fields);
	    }

	    return result;
	}
}
