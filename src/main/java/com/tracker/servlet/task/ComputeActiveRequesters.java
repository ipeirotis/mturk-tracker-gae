package com.tracker.servlet.task;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import com.google.appengine.api.utils.SystemProperty;
import com.tracker.bigquery.BigQueryService;
import com.tracker.bigquery.QueryResult;
import com.tracker.entity.ArrivalCompletions;
import com.tracker.util.SafeDateFormat;

@SuppressWarnings("serial")
public class ComputeActiveRequesters extends HttpServlet {

  private static final String APPLICATION_ID = SystemProperty.applicationId.get();
  private static final String BUCKET = "entities";

  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private static final JsonFactory JSON_FACTORY = new JacksonFactory();
  
  private static final DateFormat dateFormat = SafeDateFormat.forPattern("yyyy-MM-dd HH:mm"); 
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws IOException {

    Calendar dateFrom = Calendar.getInstance();
    dateFrom.setTime(new Date());
    dateFrom.set(Calendar.HOUR_OF_DAY, 0);
    dateFrom.set(Calendar.MINUTE, 0);
    dateFrom.set(Calendar.SECOND, 0);
    dateFrom.set(Calendar.MILLISECOND, 0);
    dateFrom.add(Calendar.HOUR_OF_DAY, -1);

    Calendar dateTo = Calendar.getInstance();
    dateTo.setTime(dateFrom.getTime());
    dateTo.add(Calendar.HOUR_OF_DAY, 1);
    
    AppIdentityCredential appIdentityCredential = new AppIdentityCredential(Collections.singleton(BigqueryScopes.BIGQUERY));
    Bigquery bigquery = new Bigquery.Builder(HTTP_TRANSPORT, JSON_FACTORY, appIdentityCredential)
      .setApplicationName(APPLICATION_ID)
      .build();

    BigQueryService bigQueryService = new BigQueryService(bigquery, APPLICATION_ID, BUCKET);
    QueryResult queryResult = bigQueryService.executeQuery(
            String.format("SELECT requesterId, COUNT(*) AS cnt "
            + "FROM [entities.HITgroup] WHERE firstSeen<%s AND lastSeen>%s GROUP BY requesterId",
            dateFormat.format(dateFrom), dateFormat.format(dateTo)));

    long count = 0;
    for(List<Object> row : queryResult.getData()){
        count += (Long)row.get(1);
    }
    
    if(count > 0) {
        ArrivalCompletions arrivalCompletions = ofy().load().type(ArrivalCompletions.class)
        .filter("from", dateFrom.getTime()).filter("to", dateTo.getTime()).limit(1).first().now();
        if(arrivalCompletions != null) {
            arrivalCompletions.setActiveRequesters(count);
            ofy().save().entity(arrivalCompletions);
        }
    }
  }

}
