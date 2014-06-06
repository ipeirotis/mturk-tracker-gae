package com.tracker.ofy;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.tracker.entity.ArrivalCompletions;
import com.tracker.entity.HITcontent;
import com.tracker.entity.HITgroup;
import com.tracker.entity.HITinstance;
import com.tracker.entity.MarketStatistics;
import com.tracker.entity.TopRequester;

public class OfyService {
	static {
        register(HITgroup.class);
        register(HITcontent.class);
        register(HITinstance.class);
        register(MarketStatistics.class);
        register(ArrivalCompletions.class);
        register(TopRequester.class);
    }

    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }
    
    public static void register(Class<?> clazz){
    	factory().register(clazz);
    }
}
