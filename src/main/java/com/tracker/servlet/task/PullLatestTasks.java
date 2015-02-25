package com.tracker.servlet.task;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchServiceFactory;
import com.tracker.entity.HITcontent;
import com.tracker.entity.HITgroup;
import com.tracker.entity.HITinstance;
import com.tracker.entity.HITrequester;
import com.tracker.entity.MarketStatistics;
import com.tracker.util.SafeCurrencyFormat;
import com.tracker.util.SafeDateFormat;

@SuppressWarnings("serial")
public class PullLatestTasks extends HttpServlet {
  
  private static final Logger logger = Logger.getLogger(PullLatestTasks.class.getName());
  private static final String URL = "https://www.mturk.com/mturk/viewhits?"
      + "&searchSpec=HITGroupSearch%23T%238%2310%23-1%23T%23%21%23%21LastUpdatedTime%211%21%23%21"
      + "&selectedSearchType=hitgroups" 
      + "&searchWords=";
  
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
  
  private static final Set<String> sortTypes = new HashSet<String>(Arrays.asList("LastUpdatedTime", "NumHITs", 
          "Reward", "LatestExpiration", "Title", "AssignmentDurationInSeconds"));
  private static final String DEFAULT_SORT_TYPE = "LastUpdatedTime";
  private static final String DEFAULT_SORT_DIRECTION = "1";
  
  private static final String PREVIEW_URL = "https://www.mturk.com/mturk/preview?groupId=";
  private static final DateFormat df = SafeDateFormat.forPattern("MMM dd, yyyy");
  private static final NumberFormat cf = SafeCurrencyFormat.forLocale(Locale.US);
  
  private static final Pattern timePattern = Pattern.compile("\\d+ \\w+");      
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    String pageNumberParam = req.getParameter("pageNumber");
    Integer pageNumber = (pageNumberParam == null) ? 1 : Integer.valueOf(pageNumberParam);
    if (pageNumber>20) {
      logger.log(Level.WARNING, "MTurk does not return pages above 20 without sign-in.");
      return;
    }
    
    String sortType = req.getParameter("sortType");
    if(sortType == null || !sortTypes.contains(sortType)){
        sortType = DEFAULT_SORT_TYPE;
    }
    
    String sortDirection = req.getParameter("sortDirection");
    if(sortDirection == null || !"0".equals(sortDirection) || !"1".equals(sortDirection)){
        sortDirection = DEFAULT_SORT_DIRECTION;
    }
    
    String url = URL + "&pageNumber=" + pageNumber.toString()
            + "&sortType=" + URLEncoder.encode(sortType + ":" + sortDirection, "UTF-8");

    try {
        loadAndParse(url, pageNumber==1 && sortType == DEFAULT_SORT_TYPE && sortDirection == DEFAULT_SORT_DIRECTION );
    } catch (Exception e) {
        logger.log(Level.SEVERE, "Error parsing page with URL: "+url, e);
    }
  }
  
  private void loadAndParse(String url, boolean fetchStatistics) throws Exception {
    Document doc = Jsoup.connect(url).get();
  	Date now = new Date();
    List<HITgroup> hitGroups = new ArrayList<HITgroup>();
    List<HITcontent> hitContents = new ArrayList<HITcontent>();
    List<HITrequester> hitRequesters = new ArrayList<HITrequester>();
    List<HITinstance> hitInstances = new ArrayList<HITinstance>();
    
    //market statistics
    if (fetchStatistics) {
      MarketStatistics statistics = extractMarketStatistics(doc);
      statistics.setTimestamp(now);
      ofy().save().entity(statistics);
    }

    Elements rows = getHitRows(doc);
    
    if(rows == null){
        logger.log(Level.WARNING, "HITgroups are not found");
        return;
    }
    Iterator<Element> rowsIterator = rows.iterator();
    String groupId = null;
    String date = null;
    
    while(rowsIterator.hasNext()) {
        try{
            Element row = rowsIterator.next();
            Element titleElement = row.select("a.capsulelink").first();
            Element groupElement = row.select("a:matchesOwn(View a HIT in this group+)").first();
            Element requesterElement = row.select("a:matchesOwn(Requester+)").first();
            Element expirationDateElement = row.select("a:matchesOwn(HIT Expiration Date+)").first();
            Element timeAllotedElement = row.select("a:matchesOwn(Time Allotted+)").first();
            Element rewardElement = row.select("a:matchesOwn(Reward:+)").first();
            Element hitsAvailableElement = row.select("a:matchesOwn(HITs Available:+)").first();
            Element descriptionElement = row.select("a:matchesOwn(Description+)").first();
            Element keywordElement = row.select("a:matchesOwn(Keywords+)").first();
            Element qualificationElement = row.select("a:matchesOwn(Qualifications Required+)").first();

            groupId = getQueryParamValue(groupElement.attr("href"), "groupId");
            if(groupId == null){
                continue;
            }

            date = expirationDateElement.parent().nextElementSibling().text();
            Date expirationDate = df.parse(date.substring(0, date.indexOf(" (")-1));
            Integer hitsAvailable = Integer.parseInt(hitsAvailableElement.parent().nextElementSibling().text());
            Number reward = cf.parse(rewardElement.parent().nextElementSibling().child(0).text());
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

            String title = titleElement.text();
            String requesterId = getQueryParamValue(
                    requesterElement.parent().nextElementSibling().child(0).attr("href"), "requesterId");
            String requesterName = requesterElement.parent().nextElementSibling().child(0).text();
            String timeAlloted = timeAllotedElement.parent().nextElementSibling().text();
            String description = descriptionElement.parent().nextElementSibling().text();

            //keywords
            List<String> keywords = getKeywords(keywordElement);

            //qualifications
            List<String> qualifications = getQualifications(qualificationElement);

            String content = loadHitContent(groupId);

            //create new HITgroup
            HITgroup hitGroup = new HITgroup(groupId, requesterId, title,
                    description, keywords, expirationDate, rewardValue, 
                    parseTime(timeAlloted), qualifications, now, now);
            hitGroups.add(hitGroup);

            //create new HITrequester if HIT group is not exists,
            //requester will be updated if he already exists
            if(existingGroup == null){
                HITrequester requester = new HITrequester(requesterId, requesterName, new Date());
                hitRequesters.add(requester);
            }

            //create new HITcontent
            HITcontent hitContent = new HITcontent(groupId, content);
            addToIndex(hitGroup, requesterId, requesterName, content);
            hitContents.add(hitContent);

            // Create a new (the first) HITinstance
            HITinstance hitinstance = new HITinstance(groupId, now, hitsAvailable, hitsAvailable, rewardAvailable, rewardAvailable);
            hitInstances.add(hitinstance);
        } catch(Exception e){
            logger.log(Level.SEVERE, "Error parsing HITgroup row", e);
        }
    }
    
    if(hitGroups.size() != 0 && hitInstances.size() != 0){
        ofy().save().entities(hitGroups);
        ofy().save().entities(hitContents);
        ofy().save().entities(hitRequesters);
        ofy().save().entities(hitInstances);
    }
  }
  
  private void addToIndex(HITgroup hitGroup, String requesterId, String requesterName, String content){
      Index index = SearchServiceFactory.getSearchService()
              .getIndex(IndexSpec.newBuilder().setName("hit_group_index"));

      com.google.appengine.api.search.Document doc = com.google.appengine.api.search.Document.newBuilder()
                  .addField(Field.newBuilder().setName("requesterId").setText(requesterId))
                  .addField(Field.newBuilder().setName("requesterName").setText(requesterName))
                  .addField(Field.newBuilder().setName("title").setText(hitGroup.getTitle()))
                  .addField(Field.newBuilder().setName("description").setText(hitGroup.getDescription()))
                  .addField(Field.newBuilder().setName("hitContent").setText(optimizeContentForIndex(content)))
                  .addField(Field.newBuilder().setName("keywords").setText(StringUtils.join(hitGroup.getKeywords(), ", ")))
                  .addField(Field.newBuilder().setName("qualifications").setText(StringUtils.join(hitGroup.getQualificationsRequired(), ", ")))
                  .addField(Field.newBuilder().setName("reward").setText(String.valueOf(hitGroup.getReward())))
                  .addField(Field.newBuilder().setName("timeAllotted").setText(String.valueOf(hitGroup.getTimeAlloted())))
                  .setId(hitGroup.getGroupId())
                  .build();
      index.put(doc);
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
  
  private Elements getHitRows(Document doc){
    Elements tables = doc.select("table[cellspacing=5][width=100%]");
    for(Element table : tables){
        Elements rows = table.select("> tbody > tr");
        if(rows.size() == 10){
            return rows;
        }
    }

    return null;
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
