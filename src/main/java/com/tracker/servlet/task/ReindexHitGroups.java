package com.tracker.servlet.task;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.cmd.Query;
import com.tracker.entity.HITcontent;
import com.tracker.entity.HITgroup;
import com.tracker.entity.HITrequester;

@SuppressWarnings("serial")
public class ReindexHitGroups extends HttpServlet {

  @SuppressWarnings("unused")
private static final Logger logger = Logger.getLogger(ReindexHitGroups.class.getName());
  private static final Set<String> ENGLISH_STOP_WORDS_SET;
  
  static {
      final List<String> stopWords = Arrays.asList(
        "a", "an", "and", "are", "as", "at", "be", "but", "by",
        "for", "if", "in", "into", "is", "it",
        "no", "not", "of", "on", "or", "such",
        "that", "the", "their", "then", "there", "these",
        "they", "this", "to", "was", "will", "with"
      );
      ENGLISH_STOP_WORDS_SET = new HashSet<String>(stopWords);
  }
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      String cursor = req.getParameter("cursor");
      String sched = req.getParameter("sched");
      Boolean saveEnabled = req.getParameter("saveEnabled") == null ? 
              false : Boolean.valueOf(req.getParameter("saveEnabled"));
      Integer pageNumber = req.getParameter("pageNumber") == null ? 
              1 : Integer.parseInt(req.getParameter("pageNumber"));
      
      if(sched == null) {
          String nextPageToken = reindex(cursor, saveEnabled);
          if(nextPageToken != null) {
              queueTask("/reindexHitGroups", nextPageToken, saveEnabled, ++pageNumber);
          }
      } else {
          queueTask("/reindexHitGroups", null, saveEnabled, pageNumber);
      }
  }

  private void addToIndex(List<HITgroup> groups){
      Index index = SearchServiceFactory.getSearchService().getIndex(IndexSpec.newBuilder().setName("hit_group_index"));
      List<Document> docs = new ArrayList<Document>();

      for(HITgroup hitGroup : groups) {
          HITrequester requester = null;
          if(hitGroup.getRequesterId() != null) {
              requester = ofy().load().type(HITrequester.class).id(hitGroup.getRequesterId()).now();
          }
          HITcontent content = ofy().load().type(HITcontent.class).id(hitGroup.getGroupId()).now();
          String requesterId = null;
          String requesterName = null;
          if(requester != null) {
              requesterId = requester.getRequesterId();
              requesterName = requester.getRequesterName();
          }
          String contentString = null;
          if(content != null) {
              contentString = content.getContent();
          }
          Document doc = com.google.appengine.api.search.Document.newBuilder()
                  .addField(Field.newBuilder().setName("requesterId").setText(requesterId))
                  .addField(Field.newBuilder().setName("requesterName").setText(requesterName))
                  .addField(Field.newBuilder().setName("title").setText(hitGroup.getTitle()))
                  .addField(Field.newBuilder().setName("description").setText(hitGroup.getDescription()))
                  .addField(Field.newBuilder().setName("hitContent").setText(optimizeContentForIndex(contentString)))
                  .addField(Field.newBuilder().setName("keywords").setText(StringUtils.join(hitGroup.getKeywords(), ", ")))
                  .addField(Field.newBuilder().setName("qualifications").setText(StringUtils.join(hitGroup.getQualificationsRequired(), ", ")))
                  .addField(Field.newBuilder().setName("reward").setText(String.valueOf(hitGroup.getReward())))
                  .addField(Field.newBuilder().setName("timeAllotted").setText(String.valueOf(hitGroup.getTimeAlloted())))
                  .setId(hitGroup.getGroupId())
                  .build();
          docs.add(doc);
      }
      index.put(docs);
  }

  private String reindex(String cursorString, Boolean saveEnabled) {
      List<HITgroup> list = new ArrayList<HITgroup>();
      Query<HITgroup> query = ofy().load().type(HITgroup.class).limit(200);

      if (cursorString != null) {
          query = query.startAt(Cursor.fromWebSafeString(cursorString));
      }

      boolean cont = false;
      QueryResultIterator<HITgroup> iterator = query.iterator();

      while (iterator.hasNext()) {
          HITgroup entity = iterator.next();
          list.add(entity);
          cont = true;
      }

      if(saveEnabled) {
          addToIndex(list);
      }
      
      if(cont) {
          Cursor cursor = iterator.getCursor();
          return cursor.toWebSafeString();
      } else {
          return null;
      }
  }

  public void queueTask(String url, String cursorStr, Boolean saveEnabled, Integer pageNumber) {
      TaskOptions taskOptions = TaskOptions.Builder
              .withMethod(TaskOptions.Method.GET)
              .url(url)
              .retryOptions(RetryOptions.Builder.withTaskRetryLimit(0));

      if(cursorStr != null) {
          taskOptions.param("cursor", cursorStr);
      }

      if (saveEnabled != null) {
          taskOptions.param("saveEnabled", saveEnabled.toString());
      }

      if (pageNumber != null) {
          taskOptions.param("pageNumber", String.valueOf(pageNumber));
      }

      Queue queue = QueueFactory.getDefaultQueue();
      queue.add(taskOptions);
  }

  /*remove html tags, styles, scripts, stop words and duplicate words*/
  private String optimizeContentForIndex(String content) {
      if(StringUtils.isEmpty(content)) {
          return null;
      }
      
      String text = Jsoup.parse(content).text();
      StringTokenizer st = new StringTokenizer(text);
      Set<String> set = new LinkedHashSet<String>();
      
      while (st.hasMoreTokens()) {
          String token = st.nextToken();
          if(!ENGLISH_STOP_WORDS_SET.contains(token)){
              set.add(token);
          }
      }

      return StringUtils.join(set, " ");
  }

}
