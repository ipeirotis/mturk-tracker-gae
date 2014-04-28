package com.tracker.servlet;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Builder;
import com.tracker.entity.HITgroup;
import com.tracker.entity.MarketStatistics;

@SuppressWarnings("serial")
public class PullLatestTasksServlet extends HttpServlet {
  
  private static final Logger logger = Logger.getLogger(PullLatestTasksServlet.class.getName());
  private static final String URL = "https://www.mturk.com/mturk/sorthits?"
      + "searchSpec=HITGroupSearch%23T%231%2310%23-1%23T%23%21%23%21LastUpdatedTime%210%21%23%21"
      + "&selectedSearchType=hitgroups&searchWords=&sortType=LastUpdatedTime%3A1&%2Fsort.x=14&%2Fsort.y=9";
  
  private static final String PREVIEW_URL = "https://www.mturk.com/mturk/preview?groupId=";
  private static final DateFormat df = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
  private static final NumberFormat cf = NumberFormat.getCurrencyInstance(Locale.US);
  
  private static final Pattern timePattern = Pattern.compile("\\d+ \\w+");      
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    
    String schedule = req.getParameter("schedule");
    
    if("true".equals(schedule)){
      schedule();
      return;
    } else {
      try {
        loadAndParse();
      } catch (Exception e) {
        logger.severe("Error parsing page: " + e.getMessage());
      }
    }
  }
  
  private void loadAndParse() throws Exception{
    //InputStream in = PullLatestTasksServlet.class.getClassLoader().getResourceAsStream("Amazon.htm");
    //String html = IOUtils.toString(in);
    //Document doc = Jsoup.parse(html);
    Document doc = Jsoup.connect(URL).get();
    
    //market statistics
    String availableHitsText = doc.select("span:matchesOwn(available now+)").first().child(0).text();
    Integer availableHits = Integer.parseInt(availableHitsText.substring(0, 
        availableHitsText.indexOf(" ")).trim().replaceAll(",", ""));
    
    String availableGroupsText = doc.select("td:matchesOwn([\\d-]+ of \\d+ Results+)").text();
    Integer availableGroups = Integer.parseInt(availableGroupsText.substring(
        availableGroupsText.indexOf("of")+2, availableGroupsText.indexOf("Results")).trim());
    
    //HITgroup
    Elements titleElements = doc.select("a.capsulelink");
    Elements groupElements = doc.select("a:matchesOwn(View a HIT in this group+)");
    Elements requesterElements = doc.select("a:matchesOwn(Requester+)");
    Elements expirationDateElements = doc.select("a:matchesOwn(HIT Expiration Date+)");
    Elements timeAllotedElements = doc.select("a:matchesOwn(Time Allotted+)");
    Elements rewardElements = doc.select("a:matchesOwn(Reward+)");
    Elements descriptionElements = doc.select("a:matchesOwn(Description+)");
    Elements keywordElements = doc.select("a:matchesOwn(Keywords+)");
    Elements qualificationElements = doc.select("a:matchesOwn(Qualifications Required+)");
    
    int numberOfRows = titleElements.size();
    
    List<HITgroup> list = new ArrayList<HITgroup>();
    
    for(int i=0; i < numberOfRows; i++){
      String groupId = getQueryParamValue(groupElements.get(i).attr("href"), "groupId");
      if(groupId == null){
        continue;
      }
      
      String date = expirationDateElements.get(i).parent().nextElementSibling().text();
      Date expirationDate = df.parse(date.substring(0, date.indexOf(" (")-1));
      
      //update HITgroup if expirationDate were changed
      HITgroup existingGroup = ofy().load().type(HITgroup.class).id(groupId).now();
      if(existingGroup != null){
        if(!existingGroup.getExpirationDate().equals(expirationDate)){
          existingGroup.setExpirationDate(expirationDate);
          ofy().save().entity(existingGroup);
        } 
        continue;
      }

      String title = titleElements.get(i).text();
      String requesterId = getQueryParamValue(
          requesterElements.get(i).parent().nextElementSibling().child(0).attr("href"), "requesterId");
      String timeAlloted = timeAllotedElements.get(i).parent().nextElementSibling().text();
      Number reward = cf.parse(rewardElements.get(i).parent().nextElementSibling().child(0).text());
      String description = descriptionElements.get(i).parent().nextElementSibling().text();
      
      //keywords
      List<String> keywords = new ArrayList<String>();
      Elements keywordLinks = keywordElements.get(i).parent().nextElementSibling().getElementsByTag("a");
      for(int j = 0; j<keywordLinks.size(); j++){
        keywords.add(keywordLinks.get(j).text());
      }

      //qualifications
      List<String> qualifications = new ArrayList<String>();
      Element qValues = qualificationElements.get(i).parent().nextElementSibling();

      if(qValues != null){
        Elements qElements = qValues.getElementsByTag("a");
        for(Element qElement : qElements){
          qualifications.add(qElement.text());
        }
      }

      String hitContent = loadExternalHitContent(groupId);
      
      //create new HITgroup
      HITgroup hitGroup = new HITgroup(groupId, requesterId, title,
          description, keywords, expirationDate,
          (int)(100*reward.floatValue()), parseTime(timeAlloted), qualifications, 
          hitContent, new Date(), new Date());
      list.add(hitGroup);
    }
    
    ofy().save().entities(list);
    
    MarketStatistics statistics = new MarketStatistics(new Date(), availableGroups, availableHits);
    ofy().save().entity(statistics);
  }
  
  private void schedule(){
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(Builder
        .withUrl("/pullLatestTasks")
        .etaMillis(System.currentTimeMillis())
        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(1))
        .method(TaskOptions.Method.GET));
  }
  
  private String loadExternalHitContent(String groupId){
    try {
      Document preview;
      preview = Jsoup.connect(PREVIEW_URL + groupId).timeout(30000).get();

      Elements iframes = preview.select("iframe[name=ExternalQuestionIFrame]");
      Element iframe = iframes.first();
      if(iframe != null){
        Document content = Jsoup.connect(iframes.attr("src")).timeout(30000).get();
        return content.html();
      }
    } catch (Exception e) {
      logger.warning("Unable to load external content: " + e.getMessage());
    }
    return null;
  }
  
  private int parseTime(String str){
    Matcher matcher = timePattern.matcher(str);
    int result = 0;

    while (matcher.find()) {
      String[] pairs = matcher.group().split(" ");
      Integer val = Integer.parseInt(pairs[0]);
      String unit = pairs[1];
      if("hour".equals(unit) || "hours".equals(unit)){
        result += val*60*60;
      } else if("minute".equals(unit) || "minutes".equals(unit)){
        result += val*60;
      } else if("second".equals(unit) || "seconds".equals(unit)){
        result += val;
      }
    }

    return result;
  }
  
  private String getQueryParamValue(String url, String param){
    if(url == null || url.isEmpty()) {
      return null;
    }
    
    int queryStart = url.indexOf('?');
    if(queryStart > 0){
      String queryString = url.substring(queryStart + 1);
      String[] pairs = queryString.split("&");
      for (String pair : pairs) {
        int idx = pair.indexOf("=");
        String paramName = pair.substring(0, idx);

        if(paramName.equals(param)){
          return pair.substring(idx + 1);
        }
      }
    }
    
    return null;
  }
}
