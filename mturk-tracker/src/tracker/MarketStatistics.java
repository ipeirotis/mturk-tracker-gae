package tracker;

import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * This object keeps track of the overall statistics of the market over time
 * 
 * Every time we fetch a page from MTurk, we keep track of the reported numbers
 * "XXXXXX HITs available now" and the "All HITs 1-10 of YYYYY Results"
 * 
 * XXXXX is the hitsAvailable
 * YYYYY is the hitGroupsAvailable
 * 
 * @author Panos
 *
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class MarketStatistics {
  
  @PrimaryKey
  @Persistent
  Date timestamp; 
  
  @Persistent
  Integer hitGroupsAvailable;
  
  @Persistent
  Integer hitsAvailable;
  

  
  
  
}
