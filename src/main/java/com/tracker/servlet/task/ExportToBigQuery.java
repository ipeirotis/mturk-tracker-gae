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
import java.util.logging.Level;
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
import com.tracker.util.TaskUtil;

@SuppressWarnings("serial")
public class ExportToBigQuery extends HttpServlet {

  private static final Logger logger = Logger.getLogger(ExportToBigQuery.class.getName());
  
  private static final String APPLICATION_ID = SystemProperty.applicationId.get();
  private static final String BUCKET = "entities";
  private static final DateFormat dateFormat = SafeDateFormat.forPattern("yyyy-MM-dd HH:mm:ss");
  private static final int DEADLINE = 8*60*1000;//8 min

  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private static final JsonFactory JSON_FACTORY = new JacksonFactory();

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

      String kind = req.getParameter("kind");
      String cursor = req.getParameter("cursor");

      Calendar calFrom = Calendar.getInstance();
      calFrom.setTime(new Date());
      calFrom.set(Calendar.HOUR_OF_DAY, 0);
      calFrom.set(Calendar.MINUTE, 0);
      calFrom.set(Calendar.SECOND, 0);
      calFrom.set(Calendar.MILLISECOND, 0);
      calFrom.add(Calendar.DAY_OF_MONTH, -1);

      Calendar calTo = Calendar.getInstance();
      calTo.setTime(calFrom.getTime());
      calTo.add(Calendar.DAY_OF_MONTH, 1);

      Date dateFrom = calFrom.getTime();
      Date dateTo = calTo.getTime();

      AppIdentityCredential appIdentityCredential = new AppIdentityCredential(Collections.singleton(BigqueryScopes.BIGQUERY));
      Bigquery bigquery = new Bigquery.Builder(HTTP_TRANSPORT, JSON_FACTORY, appIdentityCredential)
          .setApplicationName(APPLICATION_ID)
          .build();

      BigQueryService bigQueryService = new BigQueryService(bigquery, APPLICATION_ID, BUCKET);
      ensureTableExists(kind, bigQueryService);

      Map<String, Object> params = new HashMap<String, Object>();

      if("ArrivalCompletions".equals(kind)) {
          params.put("from >=", dateFrom.getTime());
          params.put("from <", dateTo.getTime());
          export(ArrivalCompletions.class, bigQueryService, params, cursor);
      } else if("HITgroup".equals(kind)) {
          params.put("lastSeen >=", dateFrom);
          params.put("lastSeen <", dateTo);
          export(HITgroup.class, bigQueryService, params, cursor);
      } else if("HITinstance".equals(kind)) {
          params.put("timestamp >=", dateFrom);
          params.put("timestamp <", dateTo);
          export(HITinstance.class, bigQueryService, params, cursor);
      } else if("HITrequester".equals(kind)) {
          params.put("lastActivity >=", dateFrom);
          params.put("lastActivity <", dateTo);
          export(HITrequester.class, bigQueryService, params, cursor);
      } else if("MarketStatistics".equals(kind)) {
          params.put("timestamp >=", dateFrom);
          params.put("timestamp <", dateTo);
          export(MarketStatistics.class, bigQueryService, params, cursor);
      } else if("TopRequester".equals(kind)) {
          params.put("timestamp >=", dateFrom);
          params.put("timestamp <", dateTo);
          export(TopRequester.class, bigQueryService, params, cursor);
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
                  } else if("List".equals(field.getType().getSimpleName())) {
                      columns.put(field.getName(), "STRING_REPEATED");//TODO: support other types(Integer etc)
                  }
              }
          } catch (Exception e) {
              throw new RuntimeException(e);
          }

          bigQueryService.createTable(kind, columns);
      }
  }

  @SuppressWarnings("unchecked")
  private <T> void export(Class<T> clazz, BigQueryService bigQueryService, Map<String, Object> params, String cursor) {
      String kind = clazz.getSimpleName();
      ListByCursorResult<T> result = null;
      if(cursor != null) {
          result = new ListByCursorResult<T>();
          result.setNextPageToken(cursor);
      }
      long start = System.currentTimeMillis();
      long counter = 0;

      while(true) {
          long duration = System.currentTimeMillis() - start;

          if(duration < DEADLINE) {
              result = listByCursor(params, result == null ? null : result.getNextPageToken(), clazz);

              List<T> list = result.getItems();
              List<Map<String, Object>> listToSave = new ArrayList<Map<String,Object>>();

              if(list.size() > 0) {
                  if("ArrivalCompletions".equals(kind)) {
                      listToSave = mapArrivalCompletions((List<ArrivalCompletions>)list);
                  } else if("HITgroup".equals(kind)) {
                      listToSave = mapHitGroups((List<HITgroup>)list);
                  } else if("HITinstance".equals(kind)) {
                      listToSave = mapHitInstances((List<HITinstance>)list);
                  } else if("HITrequester".equals(kind)) {
                      listToSave = mapHitRequesters((List<HITrequester>)list);
                  } else if("MarketStatistics".equals(kind)) {
                      listToSave = mapMarketStatistics((List<MarketStatistics>)list);
                  } else if("TopRequester".equals(kind)) {
                      listToSave = mapTopRequesters((List<TopRequester>)list);
                  }

                  try {
                      bigQueryService.insert(kind, listToSave);
                      counter += listToSave.size();
                  } catch (IOException e) {
                      //BigQuery error, schedule from current position
                      logger.log(Level.WARNING, e.getMessage(), e);
                      schedule(kind, result.getNextPageToken());
                      break;
                  }
              }

              OfyService.ofy().clear();

              if(result.getNextPageToken() == null) {
                  break;
              }
          } else {
              schedule(kind, result.getNextPageToken());
              break;
          }
      }

      logger.log(Level.INFO, String.format("Exported %d '%s'", counter, kind));
  }

  private <T> void schedule(String kind, String nextPageToken) {
      Map<String, String> taskParams = new HashMap<String, String>();
      taskParams.put("kind", kind);
      taskParams.put("cursor", nextPageToken);
      TaskUtil.queueTask("/exportToBigQuery", taskParams);
  }

  private List<Map<String, Object>> mapArrivalCompletions(List<ArrivalCompletions> list) {
      List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
      for(ArrivalCompletions arrivalCompletions : list) {
          result.add(mapArrivalCompletions(arrivalCompletions));
      }
      return result;
  }

  private Map<String, Object> mapArrivalCompletions(ArrivalCompletions arrivalCompletions) {
      Map<String, Object> result = new LinkedHashMap<String, Object>();
      result.put("id", arrivalCompletions.getId());
      result.put("from", formatDate(arrivalCompletions.getFrom()));
      result.put("to", formatDate(arrivalCompletions.getTo()));
      result.put("hitGroupsAvailableUI", arrivalCompletions.getHitGroupsAvailableUI());
      result.put("hitGroupsArrived", arrivalCompletions.getHitGroupsArrived());
      result.put("hitGroupsCompleted", arrivalCompletions.getHitGroupsCompleted());
      result.put("hitsAvailableUI", arrivalCompletions.getHitGroupsAvailableUI());
      result.put("hitsArrived", arrivalCompletions.getHitsArrived());
      result.put("hitsCompleted", arrivalCompletions.getHitsCompleted());
      result.put("rewardsArrived", arrivalCompletions.getRewardsArrived());
      result.put("rewardsCompleted", arrivalCompletions.getRewardsCompleted());
      result.put("length", arrivalCompletions.getLength());
      return result;
  }

  private List<Map<String, Object>> mapHitGroups(List<HITgroup> list) {
      List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
      for(HITgroup hitGroup : list) {
          result.add(mapHitGroup(hitGroup));
      }
      return result;
  }

  private Map<String, Object> mapHitGroup(HITgroup hitGroup) {
      Map<String, Object> result = new LinkedHashMap<String, Object>();
      result.put("groupId", hitGroup.getGroupId());
      result.put("requesterId", hitGroup.getRequesterId());
      result.put("title", hitGroup.getTitle());
      result.put("description", hitGroup.getDescription());
      result.put("keywords", hitGroup.getKeywords());
      result.put("expirationDate", formatDate(hitGroup.getExpirationDate()));
      result.put("reward", hitGroup.getReward());
      result.put("timeAlloted", hitGroup.getTimeAlloted());
      result.put("firstSeen", formatDate(hitGroup.getFirstSeen()));
      result.put("lastSeen", formatDate(hitGroup.getLastSeen()));
      result.put("active", hitGroup.isActive());
      return result;
  }

  private List<Map<String, Object>> mapHitInstances(List<HITinstance> list) {
      List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
      for(HITinstance hitInstance : list) {
          result.add(mapHitInstance(hitInstance));
      }
      return result;
  }

  private Map<String, Object> mapHitInstance(HITinstance hitInstance) {
      Map<String, Object> result = new LinkedHashMap<String, Object>();
      result.put("id", hitInstance.getId());
      result.put("groupId", hitInstance.getGroupId());
      result.put("timestamp", formatDate(hitInstance.getTimestamp()));
      result.put("hitsAvailable", hitInstance.getHitsAvailable());
      result.put("hitsDiff", hitInstance.getHitsDiff());
      result.put("rewardsAvailable", hitInstance.getRewardsAvailable());
      result.put("rewardDiff", hitInstance.getRewardDiff());
      return result;
  }

  private List<Map<String, Object>> mapHitRequesters(List<HITrequester> list) {
      List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
      for(HITrequester hitRequester : list) {
          result.add(mapHitRequester(hitRequester));
      }
      return result;
  }

  private Map<String, Object> mapHitRequester(HITrequester hitRequester) {
      Map<String, Object> result = new LinkedHashMap<String, Object>();
      result.put("requesterId", hitRequester.getRequesterId());
      result.put("requesterName", hitRequester.getRequesterName());
      result.put("lastActivity", formatDate(hitRequester.getLastActivity()));
      return result;
  }

  private List<Map<String, Object>> mapMarketStatistics(List<MarketStatistics> list) {
      List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
      for(MarketStatistics marketStatistics : list) {
          result.add(mapMarketStatistics(marketStatistics));
      }
      return result;
  }

  private Map<String, Object> mapMarketStatistics(MarketStatistics marketStatistics) {
      Map<String, Object> result = new LinkedHashMap<String, Object>();
      result.put("id", marketStatistics.getId());
      result.put("timestamp", formatDate(marketStatistics.getTimestamp()));
      result.put("hitGroupsAvailable", marketStatistics.getHitGroupsAvailable());
      result.put("hitsAvailable", marketStatistics.getHitsAvailable());
      return result;
  }

  private List<Map<String, Object>> mapTopRequesters(List<TopRequester> list) {
      List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
      for(TopRequester topRequester : list) {
          result.add(mapTopRequester(topRequester));
      }
      return result;
  }

  private Map<String, Object> mapTopRequester(TopRequester topRequester) {
      Map<String, Object> result = new LinkedHashMap<String, Object>();
      result.put("id", topRequester.getId());
      result.put("requesterId", topRequester.getRequesterId());
      result.put("requesterName", topRequester.getRequesterName());
      result.put("hits", topRequester.getHits());
      result.put("reward", topRequester.getReward());
      result.put("projects", topRequester.getProjects());
      result.put("timestamp", formatDate(topRequester.getTimestamp()));
      return result;
  }

  private <T> ListByCursorResult<T> listByCursor(Map<String, Object> params, String cursorString, Class<T> clazz) {
      List<T> entities = new ArrayList<T>();
      Query<T> query = ofy().load().type(clazz).limit(500);

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
