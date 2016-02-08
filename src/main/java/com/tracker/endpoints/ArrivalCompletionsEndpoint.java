package com.tracker.endpoints;

import static com.tracker.ofy.OfyService.ofy;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceException;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.tracker.entity.ArrivalCompletions;
import com.tracker.util.SafeDateFormat;

@Api(name = "mturk", description = "The API for mturk-tracker", version = "v1")
public class ArrivalCompletionsEndpoint {
    private MemcacheService memcacheService = MemcacheServiceFactory
            .getMemcacheService();
    private DateFormat formatter = SafeDateFormat.forPattern("MM/dd/yyyy");

    @SuppressWarnings("unchecked")
    @ApiMethod(name = "arrivalCompletions.list", path = "arrivalCompletions/list", httpMethod = HttpMethod.GET)
    public Map<String, List<ArrivalCompletions>> list(
            @Named("from") String from, @Named("to") String to)
            throws ParseException {

        Calendar dateFrom = Calendar.getInstance();
        dateFrom.setTime(formatter.parse(from));
        dateFrom.set(Calendar.HOUR_OF_DAY, 0);
        dateFrom.set(Calendar.MINUTE, 0);
        dateFrom.set(Calendar.SECOND, 0);
        dateFrom.set(Calendar.MILLISECOND, 0);

        Calendar dateTo = Calendar.getInstance();
        dateTo.setTime(formatter.parse(to));
        dateTo.set(Calendar.HOUR_OF_DAY, 23);
        dateTo.set(Calendar.MINUTE, 59);
        dateTo.set(Calendar.SECOND, 59);
        dateTo.set(Calendar.MILLISECOND, 999);

        String memcacheKey = "arrival_completions_" + dateFrom.getTime() + "_"
                + dateTo.getTime();

        List<ArrivalCompletions> list = null;
        Map<Date, ArrivalCompletions> dailyMap = new HashMap<Date, ArrivalCompletions>();
        Map<String, List<ArrivalCompletions>> result = new HashMap<String, List<ArrivalCompletions>>();

        if (memcacheService.contains(memcacheKey)) {
            list = (List<ArrivalCompletions>) memcacheService.get(memcacheKey);
        } else {
            list = ofy().load().type(ArrivalCompletions.class)
                    .filter("from >=", dateFrom.getTime())
                    .filter("from <=", dateTo.getTime()).list();
            try {
                memcacheService.put(memcacheKey, list);
            } catch (MemcacheServiceException e) {
            }
        }

        if (list != null) {
            for (ArrivalCompletions ac : list) {
                Calendar d = Calendar.getInstance();
                d.setTime(ac.getFrom());
                d.set(Calendar.HOUR_OF_DAY, 0);
                d.set(Calendar.MINUTE, 0);
                d.set(Calendar.SECOND, 0);
                d.set(Calendar.MILLISECOND, 0);

                if (dailyMap.containsKey(d.getTime())) {
                    ArrivalCompletions existing = dailyMap.get(d.getTime());
                    existing.setHitGroupsArrived(inc(
                            existing.getHitGroupsArrived(),
                            ac.getHitGroupsArrived()));
                    existing.setHitGroupsAvailableUI(inc(
                            existing.getHitGroupsAvailableUI(),
                            ac.getHitGroupsAvailableUI()));
                    existing.setHitGroupsCompleted(inc(
                            existing.getHitGroupsCompleted(),
                            ac.getHitGroupsCompleted()));
                    existing.setHitsArrived(inc(existing.getHitsArrived(),
                            ac.getHitsArrived()));
                    existing.setHitsAvailableUI(inc(
                            existing.getHitsAvailableUI(),
                            ac.getHitsAvailableUI()));
                    existing.setHitsCompleted(inc(existing.getHitsCompleted(),
                            ac.getHitsCompleted()));
                    existing.setRewardsArrived(inc(
                            existing.getRewardsArrived(),
                            ac.getRewardsArrived()));
                    existing.setRewardsCompleted(inc(
                            existing.getRewardsCompleted(),
                            ac.getRewardsCompleted()));
                } else {
                    ArrivalCompletions initial = new ArrivalCompletions(
                            d.getTime(), d.getTime());
                    initial.setHitGroupsArrived(ac.getHitGroupsArrived());
                    initial.setHitGroupsAvailableUI(ac
                            .getHitGroupsAvailableUI());
                    initial.setHitGroupsCompleted(ac.getHitGroupsCompleted());
                    initial.setHitsArrived(ac.getHitsArrived());
                    initial.setHitsAvailableUI(ac.getHitsAvailableUI());
                    initial.setHitsCompleted(ac.getHitsCompleted());
                    initial.setRewardsArrived(ac.getRewardsArrived());
                    initial.setRewardsCompleted(ac.getRewardsCompleted());
                    dailyMap.put(d.getTime(), initial);
                }
            }
        }
        result.put("hourly", list);
        result.put("daily",
                new ArrayList<ArrivalCompletions>(dailyMap.values()));
        return result;
    }

    private Long inc(Long v1, Long v2) {
        Long result = 0L;
        if (v1 != null) {
            result += v1;
        }
        if (v2 != null) {
            result += v2;
        }
        return result;
    }

    private Double inc(Double v1, Double v2) {
        Double result = 0d;
        if (v1 != null) {
            result += v1;
        }
        if (v2 != null) {
            result += v2;
        }
        return result;
    }
}
