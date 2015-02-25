package com.tracker.servlet.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.GetRequest;
import com.google.appengine.api.search.GetResponse;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchServiceFactory;

@SuppressWarnings("serial")
public class ClearHitGroupsIndex extends HttpServlet {

  private static final Logger logger = Logger.getLogger(ClearHitGroupsIndex.class.getName());
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
      Index index = SearchServiceFactory.getSearchService()
              .getIndex(IndexSpec.newBuilder().setName("hit_group_index"));
    
    try {
        // looping because getRange by default returns up to 100 documents at a time
        while (true) {
            List<String> docIds = new ArrayList<String>();
            GetRequest request = GetRequest.newBuilder().setReturningIdsOnly(true).build();
            GetResponse<Document> response = index.getRange(request);
            if (response.getResults().isEmpty()) {
                break;
            }
            for (Document doc : response) {
                docIds.add(doc.getId());
            }
            index.delete(docIds);
        }
    } catch (RuntimeException e) {
        logger.log(Level.SEVERE, "Failed to delete documents", e);
    }
  }

}
