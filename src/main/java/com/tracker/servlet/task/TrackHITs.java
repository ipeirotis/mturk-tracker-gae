package com.tracker.servlet.task;

import static com.tracker.ofy.OfyService.ofy;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.tracker.entity.HITgroup;
import com.tracker.entity.HITinstance;

@SuppressWarnings("serial")
public class TrackHITs extends HttpServlet {
  private static final Logger logger = Logger.getLogger(TrackHITs.class.getName());
  private static final String PREVIEW_URL = "https://www.mturk.com/mturk/preview?groupId=";
  private static final String NOT_AVAILABLE_ALERT = "There are no more available HITs in this group";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      try{
          loadAndParse(req.getParameter("groupId"));
      }catch(Exception e){
          logger.log(Level.SEVERE, "Error tracking HITgroup, id= " + req.getParameter("groupId"), e);
      }
  }

  private void loadAndParse(String groupId) throws IOException {
    HITgroup group = ofy().load().type(HITgroup.class).id(groupId).now();
    HITinstance recentInstance = getRecentInstance(group.getGroupId());
    Integer recentHITsAvailable = (recentInstance == null) ? 0 : recentInstance.getHitsAvailable();
    Integer recentRewardsAvailable = recentHITsAvailable * group.getReward();

    Document doc = Jsoup.connect(PREVIEW_URL + group.getGroupId()).get();
    Element alertboxHeader = doc.getElementById("alertboxHeader");

    if (alertboxHeader != null && alertboxHeader.text().startsWith(NOT_AVAILABLE_ALERT)) {
      // We cannot find the HIT anymore. So we set the "active" flag for the
      // HITgroup to false and create a final HIT instance that shows that we have 0 HITs
      // available (this is necessary to have correct values for the ArrivalCompletions
      // computation)
      group.setActive(false);
      ofy().save().entity(group);

      Integer hitsDiff = 0 - recentHITsAvailable;
      Integer rewardsDiff = 0 - recentRewardsAvailable;
      HITinstance hitinstance = new HITinstance(group.getGroupId(), new Date(), 0, hitsDiff, 0, rewardsDiff);
      ofy().save().entity(hitinstance);
    } else {
      // We found the HIT on the system, so we update the lastSeen variable for
      // the HITgroup
      group.setLastSeen(new Date());
      ofy().save().entity(group);

      Element hitElement = doc.select("a:matchesOwn(HITs Available:+)").first();
      String hits = hitElement.parent().nextElementSibling().text();
      Integer iHits = Integer.parseInt(hits);

      Integer hitsDiff = iHits - recentHITsAvailable;
      Integer rewardsDiff = hitsDiff * group.getReward();

      if (hitsDiff != 0) {
        // We only save a new HIT instance if there is a change compared to the
        // prior HITinstance
        HITinstance hitinstance = new HITinstance(group.getGroupId(), new Date(), iHits, hitsDiff, group.getReward() * iHits,
            rewardsDiff);
        ofy().save().entity(hitinstance);
      }

    }
  }

  private HITinstance getRecentInstance(String groupId) {
    return ofy().load().type(HITinstance.class).filter("groupId", groupId).order("-timestamp").limit(1).first().now();
  }

}
