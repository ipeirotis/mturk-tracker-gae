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
import java.util.logging.Level;
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
import com.tracker.entity.HITinstance;
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
        logger.log(Level.SEVERE, "Error parsing page", e);
      }
    }
  }
  
  private void loadAndParse() throws Exception{
    
    Date now = new Date();
    Document doc = Jsoup.connect(URL).get();
    
    //market statistics
    MarketStatistics statistics = extractMarketStatistics(doc);
    statistics.setTimestamp(now);
    ofy().save().entity(statistics);
    
    //HITgroup
    Elements titleElements = doc.select("a.capsulelink");
    Elements groupElements = doc.select("a:matchesOwn(View a HIT in this group+)");
    Elements requesterElements = doc.select("a:matchesOwn(Requester+)");
    Elements expirationDateElements = doc.select("a:matchesOwn(HIT Expiration Date+)");
    Elements timeAllotedElements = doc.select("a:matchesOwn(Time Allotted+)");
    Elements rewardElements = doc.select("a:matchesOwn(Reward:+)");
    Elements hitsAvailableElements = doc.select("a:matchesOwn(HITs Available:+)");
    Elements descriptionElements = doc.select("a:matchesOwn(Description+)");
    Elements keywordElements = doc.select("a:matchesOwn(Keywords+)");
    Elements qualificationElements = doc.select("a:matchesOwn(Qualifications Required+)");
    
    int numberOfRows = titleElements.size();
    
    assert(groupElements.size() == numberOfRows);
    assert(requesterElements.size() == numberOfRows);
    assert(expirationDateElements.size() == numberOfRows);
    assert(timeAllotedElements.size() == numberOfRows);
    assert(rewardElements.size() == numberOfRows);
    assert(hitsAvailableElements.size() == numberOfRows);
    assert(descriptionElements.size() == numberOfRows);
    assert(keywordElements.size() == numberOfRows);
    assert(qualificationElements.size() == numberOfRows);
    
    
    
    List<HITgroup> hitGroups = new ArrayList<HITgroup>(numberOfRows);
    List<HITinstance> hitInstances = new ArrayList<HITinstance>(numberOfRows);
    
    for(int i=0; i < numberOfRows; i++){
      String groupId = getQueryParamValue(groupElements.get(i).attr("href"), "groupId");
      if(groupId == null){
        continue;
      }
      
      String date = expirationDateElements.get(i).parent().nextElementSibling().text();
      Date expirationDate = df.parse(date.substring(0, date.indexOf(" (")-1));
      Integer hitsAvailable = Integer.parseInt(hitsAvailableElements.get(i).parent().nextElementSibling().text());
      Number reward = cf.parse(rewardElements.get(i).parent().nextElementSibling().child(0).text());
      Integer rewardValue = Math.round(100*reward.floatValue());
      Integer rewardAvailable = rewardValue*hitsAvailable;
      
      //check existing HITgroup
      HITgroup existingGroup = ofy().load().type(HITgroup.class).id(groupId).now();
      if(existingGroup != null){
        // If we already have a HITgroup, we just update the lastSeen page
        existingGroup.setLastSeen(now);
        
        // If the group was inactive, we revive it and create a new HITinstance
          if(existingGroup.isActive()==false) {
            existingGroup.setActive(true);
            existingGroup.setExpirationDate(expirationDate);
            
            HITinstance hitinstance = new HITinstance(groupId, new Date(), hitsAvailable, hitsAvailable, rewardAvailable, rewardAvailable);
            hitInstances.add(hitinstance);
          }
          
          hitGroups.add(existingGroup);
          continue;
      }

      String title = titleElements.get(i).text();
      String requesterId = getQueryParamValue(
          requesterElements.get(i).parent().nextElementSibling().child(0).attr("href"), "requesterId");
      String requesterName = requesterElements.get(i).parent().nextElementSibling().child(0).text();
      String timeAlloted = timeAllotedElements.get(i).parent().nextElementSibling().text();
      String description = descriptionElements.get(i).parent().nextElementSibling().text();
      
      //keywords
      List<String> keywords = getKeywords(keywordElements.get(i));

      //qualifications
      List<String> qualifications = getQualifications(qualificationElements.get(i));
      
      String hitContent = loadHitContent(groupId);
      
      
      //create new HITgroup 
      HITgroup hitGroup = new HITgroup(groupId, requesterId, requesterName, title,
          description, keywords, expirationDate, rewardValue, 
          parseTime(timeAlloted), qualifications, hitContent, now, now);
      hitGroups.add(hitGroup);
      
      // Create a new (the first) HITinstance
     
      HITinstance hitinstance = new HITinstance(groupId, now, hitsAvailable, hitsAvailable, rewardAvailable, rewardAvailable);
      hitInstances.add(hitinstance);
    }
    
    ofy().save().entities(hitGroups);
    ofy().save().entities(hitInstances);
    

  }
  
  private List<String> getKeywords(Element keywordElement) {
    //keywords
    List<String> keywords = new ArrayList<String>();
    Elements keywordLinks = keywordElement.parent().nextElementSibling().getElementsByTag("a");
    for(int j = 0; j<keywordLinks.size(); j++){
      String keyword = keywordLinks.get(j).text();
      keywords.add(keyword);
    }
    return keywords;
    
  }
  
  private List<String> getQualifications(Element qualificationElement) {
    //qualifications
    List<String> qualifications = new ArrayList<String>();
    Element qValues = qualificationElement.parent().nextElementSibling();

    if(qValues != null){
      Elements qElements = qValues.getElementsByTag("a");
      for(Element qElement : qElements){
        qualifications.add(qElement.text());
      }
    }
    return qualifications;
    
  }
  


  private MarketStatistics extractMarketStatistics(Document doc) {
    String availableHitsText = doc.select("span:matchesOwn(available now+)").first().child(0).text();
    Integer availableHits = Integer.parseInt(availableHitsText.substring(0, 
        availableHitsText.indexOf(" ")).trim().replaceAll(",", ""));
    
    String availableGroupsText = doc.select("td:matchesOwn([\\d-]+ of \\d+ Results+)").text();
    Integer availableGroups = Integer.parseInt(availableGroupsText.substring(
        availableGroupsText.indexOf("of")+2, availableGroupsText.indexOf("Results")).trim());
    
    MarketStatistics statistics = new MarketStatistics(new Date(), availableGroups, availableHits);
    return statistics;
  }
  
  private void schedule(){
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(Builder
        .withUrl("/pullLatestTasks")
        .etaMillis(System.currentTimeMillis())
        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(1))
        .method(TaskOptions.Method.GET));
  }
  
  private String loadHitContent(String groupId){
    try {
      Document preview;
      preview = Jsoup.connect(PREVIEW_URL + groupId).timeout(30000).get();
      
      Element internalContentElement = preview.getElementById("hit-wrapper");
      if(internalContentElement != null){
        return internalContentElement.html();
      }

      Elements iframes = preview.select("iframe[name=ExternalQuestionIFrame]");
      Element iframe = iframes.first();
      if(iframe != null){
        Document content = Jsoup.connect(iframes.attr("src")).timeout(30000).get();
        return content.html();
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Unable to load external content", e);
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
