package com.tracker.endpoints;

import static com.tracker.ofy.OfyService.ofy;

import java.util.ArrayList;
import java.util.Collection;
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
	public List<HITgroup> listByRequesterId(@Named("requesterId") String requesterId) {
	    return ofy().load().type(HITgroup.class).filter("requesterId", requesterId).list();
	}

	@ApiMethod(name = "hitgroup.search", path = "hitgroup/search", httpMethod = HttpMethod.GET)
	public List<Map<String, String>> search(
	        @Nullable @Named("requesterName") String requesterName,
	        @Nullable @Named("title") String title,
	        @Nullable @Named("description") String description,
	        @Nullable @Named("hitContent") String hitContent,
	        @Nullable @Named("keyword") String keyword,
	        @Nullable @Named("qualification") String qualification) {
	    
	    List<String> params = new ArrayList<String>();
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
	    
	    String queryString = StringUtils.join(params, " AND ");	    
	    
	    IndexSpec indexSpec = IndexSpec.newBuilder().setName("hit_group_index").build();
	    Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);
	    QueryOptions options = QueryOptions.newBuilder()
	            .setFieldsToReturn("requesterName", "title", "description").setLimit(1000).build();
	    Query query = Query.newBuilder().setOptions(options).build(queryString);
	    Collection<ScoredDocument> docs = index.search(query).getResults();
	    
	    List<Map<String, String>> result = new ArrayList<Map<String, String>>();
	    
	    for(ScoredDocument doc : docs) {
	        Map<String, String> fields = new HashMap<String, String>();
	        for (Field field : doc.getFields()){
	            fields.put(field.getName(), field.getText());
	        }
	        result.add(fields);
	    }

	    return result;
	}
}
