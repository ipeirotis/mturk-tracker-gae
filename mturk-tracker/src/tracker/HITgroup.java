package tracker;

import java.util.Date;
import java.util.List;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;


import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.w3c.dom.Document;


@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class HITgroup {
  
  @PrimaryKey
  @Persistent
  String groupId;
  
  @Persistent
  String requesterId;
  
  @Persistent
  String title;
  
  @Persistent
  String description;
  
  @Persistent
  List<String> keywords;
  
  @Persistent
  Date expirationDate;
  
  //Price in cents
  @Persistent
  Integer reward;
  
  //Time in minutes
  @Persistent
  Integer timeAlloted;
  
  @Persistent
  List<String> qualificationsRequired;
  
  
  // We also need to store somewhere the actual task.
  // Sometimes the HIT is an "external" HIT displayed within an iframe
  // and stored in a third party web server
  // Sometimes the HIT is displayed by Amazon MTurk
  // Not clear if we should keep the document as part of the HIT group
  // or store it as a separate entity in the datastore.
  @Persistent
  Document hitContent;
  
}
