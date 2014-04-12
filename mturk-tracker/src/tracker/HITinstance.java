package tracker;

import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class HITinstance {
  
  public static Key generateKeyFromID(String groupId, Date timestamp) {
    return KeyFactory.createKey(HITinstance.class.getSimpleName(), groupId + "_" + timestamp.getTime());
  }
  
  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Key key;
  
  @Persistent
  String groupId;
  
  @Persistent
  Date timestamp; 
  
  @Persistent
  Integer hitsAvailable; 
  
  
}
