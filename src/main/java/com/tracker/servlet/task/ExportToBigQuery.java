package com.tracker.servlet.task;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.googleapis.extensions.appengine.auth.oauth2.AppIdentityCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.BigqueryScopes;
import com.google.api.services.bigquery.model.Table;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.utils.SystemProperty;
import com.googlecode.objectify.cmd.Query;
import com.tracker.bigquery.BigQueryService;
import com.tracker.entity.ArrivalCompletions;
import com.tracker.entity.HITgroup;
import com.tracker.entity.HITinstance;
import com.tracker.entity.HITrequester;
import com.tracker.entity.MarketStatistics;
import com.tracker.entity.TopRequester;
import com.tracker.ofy.ListByCursorResult;
import com.tracker.ofy.OfyService;
import com.tracker.util.SafeDateFormat;

@SuppressWarnings("serial")
public class ExportToBigQuery extends HttpServlet {
  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(ExportToBigQuery.class.getName());
  
  private static final String APPLICATION_ID = SystemProperty.applicationId.get();
  private static final String BUCKET = "entities";
  private static final DateFormat dateFormat = SafeDateFormat.forPattern("yyyy-MM-dd HH:mm:ss"); 

  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private static final JsonFactory JSON_FACTORY = new JacksonFactory();

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

      Calendar dateFrom = Calendar.getInstance();
      dateFrom.setTime(new Date());
      dateFrom.set(Calendar.HOUR_OF_DAY, 0);
      dateFrom.set(Calendar.MINUTE, 0);
      dateFrom.set(Calendar.SECOND, 0);
      dateFrom.set(Calendar.MILLISECOND, 0);
      dateFrom.add(Calendar.DAY_OF_MONTH, -1);

      Calendar dateTo = Calendar.getInstance();
      dateTo.setTime(dateFrom.getTime());
      dateTo.set(Calendar.HOUR_OF_DAY, 23);
      dateTo.set(Calendar.MINUTE, 59);
      dateTo.set(Calendar.SECOND, 59);
      dateTo.set(Calendar.SECOND, 59);
      dateTo.set(Calendar.MILLISECOND, 999);

      AppIdentityCredential appIdentityCredential = new AppIdentityCredential(Collections.singleton(BigqueryScopes.BIGQUERY));
      Bigquery bigquery = new Bigquery.Builder(HTTP_TRANSPORT, JSON_FACTORY, appIdentityCredential)
          .setApplicationName(APPLICATION_ID)
          .build();

      String kind = req.getParameter("kind");
      BigQueryService bigQueryService = new BigQueryService(bigquery, APPLICATION_ID, BUCKET);
      ensureTableExists(kind, bigQueryService);

      if("ArrivalCompletions".equals(kind)) {
          exportArrivalCompletions(kind, bigQueryService, dateFrom.getTime(), dateTo.getTime());
      } else if("HITgroup".equals(kind)) {
          exportHITGroups(kind, bigQueryService, dateFrom.getTime(), dateTo.getTime());
      } else if("HITinstance".equals(kind)) {
          exportHITinstances(kind, bigQueryService, dateFrom.getTime(), dateTo.getTime());
      } else if("HITrequester".equals(kind)) {
          exportHITrequesters(kind, bigQueryService, dateFrom.getTime(), dateTo.getTime());
      } else if("MarketStatistics".equals(kind)) {
          exportMarketStatistics(kind, bigQueryService, dateFrom.getTime(), dateTo.getTime());
      } else if("TopRequester".equals(kind)) {
          exportTopRequesters(kind, bigQueryService, dateFrom.getTime(), dateTo.getTime());
      }
  }

  private void ensureTableExists(String kind, BigQueryService bigQueryService) {
      Table table = bigQueryService.getTable(kind);
      if(table == null) {
          Map<String, String> columns = new HashMap<String, String>();
          try {
              Field[] declaredFields = Class.forName("com.tracker.entity." + kind).getDeclaredFields();
              for(Field field : declaredFields) {
                  if("Long".equals(field.getType().getSimpleName())) {
                      columns.put(field.getName(), "INTEGER");
                  } else if("Integer".equals(field.getType().getSimpleName())) {
                      columns.put(field.getName(), "INTEGER");
                  } else if("String".equals(field.getType().getSimpleName())) {
                      columns.put(field.getName(), "STRING");
                  } else if("Double".equals(field.getType().getSimpleName())) {
                      columns.put(field.getName(), "FLOAT");
                  } else if("Date".equals(field.getType().getSimpleName())) {
                      columns.put(field.getName(), "TIMESTAMP");
                  } else if("Boolean".equals(field.getType().getSimpleName())) {
                      columns.put(field.getName(), "BOOLEAN");
                  } else if("boolean".equals(field.getType().getSimpleName())) {
                      columns.put(field.getName(), "BOOLEAN");
                  }
              }
          } catch (Exception e) {
              throw new RuntimeException(e);
          }
          
          bigQueryService.createTable(kind, columns);
      }
  }

  private void exportArrivalCompletions(String kind, BigQueryService bigQueryService, Date from, Date to) {
      ListByCursorResult<ArrivalCompletions> arrivalCompletionsResult = null;
      Map<String, Object> params = new HashMap<String, Object>();
      params.put("from >=", from);
      params.put("from <", to);

      while(true) {
          arrivalCompletionsResult = listByCursor(params,
                  arrivalCompletionsResult == null ? null : arrivalCompletionsResult.getNextPageToken(), ArrivalCompletions.class);

          List<ArrivalCompletions> list = arrivalCompletionsResult.getItems();
          
          List<Map<String, Object>> listToSave = new ArrayList<Map<String,Object>>(); 
          
          if(list.size() > 0) {
              for(ArrivalCompletions ac : list) {
                  Map<String, Object> data = new LinkedHashMap<String, Object>();
                  data.put("id", ac.getId());
                  data.put("from", formatDate(ac.getFrom()));
                  data.put("to", formatDate(ac.getTo()));
                  data.put("hitGroupsAvailableUI", ac.getHitGroupsAvailableUI());
                  data.put("hitGroupsArrived", ac.getHitGroupsArrived());
                  data.put("hitGroupsCompleted", ac.getHitGroupsCompleted());
                  data.put("hitsAvailableUI", ac.getHitGroupsAvailableUI());
                  data.put("hitsArrived", ac.getHitsArrived());
                  data.put("hitsCompleted", ac.getHitsCompleted());
                  data.put("rewardsArrived", ac.getRewardsArrived());
                  data.put("rewardsCompleted", ac.getRewardsCompleted());
                  data.put("length", ac.getLength());
    
                  listToSave.add(data);
              } 
              bigQueryService.insert(kind, listToSave);
          }

          OfyService.ofy().clear();

          if(arrivalCompletionsResult.getNextPageToken() == null) {
              break;
          }
      }
  }

  private void exportHITGroups(String kind, BigQueryService bigQueryService, Date from, Date to) {
      ListByCursorResult<HITgroup> hitGroupsResult = null;
      Map<String, Object> params = new HashMap<String, Object>();
      params.put("lastSeen >=", from);
      params.put("lastSeen <", to);

      while(true) {
          hitGroupsResult = listByCursor(params,
                  hitGroupsResult == null ? null : hitGroupsResult.getNextPageToken(), HITgroup.class);

          List<HITgroup> list = hitGroupsResult.getItems();
          
          List<Map<String, Object>> listToSave = new ArrayList<Map<String,Object>>(); 

          if(list.size() > 0) {
              for(HITgroup hg : list) {
                  Map<String, Object> data = new LinkedHashMap<String, Object>();
                  data.put("groupId", hg.getGroupId());
                  data.put("requesterId", hg.getRequesterId());
                  data.put("title", hg.getTitle());
                  data.put("description", hg.getDescription());
                  data.put("expirationDate", formatDate(hg.getExpirationDate()));
                  data.put("reward", hg.getReward());
                  data.put("timeAlloted", hg.getTimeAlloted());
                  data.put("firstSeen", formatDate(hg.getFirstSeen()));
                  data.put("lastSeen", formatDate(hg.getLastSeen()));
                  data.put("active", hg.isActive());
    
                  listToSave.add(data);
              }
              bigQueryService.insert(kind, listToSave);
          }

          OfyService.ofy().clear();

          if(hitGroupsResult.getNextPageToken() == null) {
              break;
          }
      }
  }

  private void exportHITinstances(String kind, BigQueryService bigQueryService, Date from, Date to) {
      ListByCursorResult<HITinstance> hitInstanceResult = null;
      Map<String, Object> params = new HashMap<String, Object>();
      params.put("timestamp >=", from);
      params.put("timestamp <", to);

      while(true) {
          hitInstanceResult = listByCursor(params,
                  hitInstanceResult == null ? null : hitInstanceResult.getNextPageToken(), HITinstance.class);

          List<HITinstance> list = hitInstanceResult.getItems();
          
          List<Map<String, Object>> listToSave = new ArrayList<Map<String,Object>>(); 
          
          if(list.size() > 0) {
              for(HITinstance hi : list) {
                  Map<String, Object> data = new LinkedHashMap<String, Object>();
                  data.put("id", hi.getId());
                  data.put("groupId", hi.getGroupId());
                  data.put("timestamp", formatDate(hi.getTimestamp()));
                  data.put("hitsAvailable", hi.getHitsAvailable());
                  data.put("hitsDiff", hi.getHitsDiff());
                  data.put("rewardsAvailable", hi.getRewardsAvailable());
                  data.put("rewardDiff", hi.getRewardDiff());
    
                  listToSave.add(data);
              } 
              bigQueryService.insert(kind, listToSave);
          }

          OfyService.ofy().clear();

          if(hitInstanceResult.getNextPageToken() == null) {
              break;
          }
      }
  }

  private void exportHITrequesters(String kind, BigQueryService bigQueryService, Date from, Date to) {
      ListByCursorResult<HITrequester> hitRequesterResult = null;
      Map<String, Object> params = new HashMap<String, Object>();
      params.put("lastActivity >=", from);
      params.put("lastActivity <", to);

      while(true) {
          hitRequesterResult = listByCursor(params,
                  hitRequesterResult == null ? null : hitRequesterResult.getNextPageToken(), HITrequester.class);

          List<HITrequester> list = hitRequesterResult.getItems();
          
          List<Map<String, Object>> listToSave = new ArrayList<Map<String,Object>>(); 
          
          if(list.size() > 0) {
              for(HITrequester hr : list) {
                  Map<String, Object> data = new LinkedHashMap<String, Object>();
                  data.put("requesterId", hr.getRequesterId());
                  data.put("requesterName", hr.getRequesterName());
                  data.put("lastActivity", formatDate(hr.getLastActivity()));

                  listToSave.add(data);
              } 
              bigQueryService.insert(kind, listToSave);
          }

          OfyService.ofy().clear();

          if(hitRequesterResult.getNextPageToken() == null) {
              break;
          }
      }
  }

  private void exportMarketStatistics(String kind, BigQueryService bigQueryService, Date from, Date to) {
      ListByCursorResult<MarketStatistics> marketStatisticsResult = null;
      Map<String, Object> params = new HashMap<String, Object>();
      params.put("timestamp >=", from);
      params.put("timestamp <", to);

      while(true) {
          marketStatisticsResult = listByCursor(params,
                  marketStatisticsResult == null ? null : marketStatisticsResult.getNextPageToken(), MarketStatistics.class);

          List<MarketStatistics> list = marketStatisticsResult.getItems();
          
          List<Map<String, Object>> listToSave = new ArrayList<Map<String,Object>>(); 
          
          if(list.size() > 0) {
              for(MarketStatistics ms : list) {
                  Map<String, Object> data = new LinkedHashMap<String, Object>();
                  data.put("id", ms.getId());
                  data.put("timestamp", formatDate(ms.getTimestamp()));
                  data.put("hitGroupsAvailable", ms.getHitGroupsAvailable());
                  data.put("hitsAvailable", ms.getHitsAvailable());

                  listToSave.add(data);
              } 
              bigQueryService.insert(kind, listToSave);
          }

          OfyService.ofy().clear();

          if(marketStatisticsResult.getNextPageToken() == null) {
              break;
          }
      }
  }

  private void exportTopRequesters(String kind, BigQueryService bigQueryService, Date from, Date to) {
      ListByCursorResult<TopRequester> topRequesterResult = null;
      Map<String, Object> params = new HashMap<String, Object>();
      params.put("timestamp >=", from);
      params.put("timestamp <", to);

      while(true) {
          topRequesterResult = listByCursor(params,
                  topRequesterResult == null ? null : topRequesterResult.getNextPageToken(), TopRequester.class);

          List<TopRequester> list = topRequesterResult.getItems();
          
          List<Map<String, Object>> listToSave = new ArrayList<Map<String,Object>>(); 
          
          if(list.size() > 0) {
              for(TopRequester tr : list) {
                  Map<String, Object> data = new LinkedHashMap<String, Object>();
                  data.put("id", tr.getId());
                  data.put("requesterId", tr.getRequesterId());
                  data.put("requesterName", tr.getRequesterName());
                  data.put("hits", tr.getHits());
                  data.put("reward", tr.getReward());
                  data.put("projects", tr.getProjects());
                  data.put("timestamp", formatDate(tr.getTimestamp()));

                  listToSave.add(data);
              } 
              bigQueryService.insert(kind, listToSave);
          }

          OfyService.ofy().clear();

          if(topRequesterResult.getNextPageToken() == null) {
              break;
          }
      }
  }
  
  public <T> ListByCursorResult<T> listByCursor(Map<String, Object> params, String cursorString, Class<T> clazz) {
      List<T> entities = new ArrayList<T>();
      Query<T> query = ofy().load().type(clazz).limit(1000);
      
      if (params != null) {
          for (Map.Entry<String, Object> entry : params.entrySet()) {
              query = query.filter(entry.getKey(), entry.getValue());
          }
      }

      if (cursorString != null) {
          query = query.startAt(Cursor.fromWebSafeString(cursorString));
      }

      boolean cont = false;
      QueryResultIterator<T> iterator = query.iterator();

      while (iterator.hasNext()) {
          T entity = iterator.next();
          entities.add(entity);
          cont = true;
      }

      if(cont) {
          Cursor cursor = iterator.getCursor();
          return new ListByCursorResult<T>().setItems(entities).setNextPageToken(cursor.toWebSafeString());
      } else {
          return new ListByCursorResult<T>().setItems(entities);
      }
  }

  private String formatDate(Date date) {
      if(date == null) {
          return null;
      } else {
          return dateFormat.format(date.getTime());
      }
  }
  
}
