package com.tracker.entity;

import java.util.Date;
import java.util.List;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
public class HITgroup {

  @Id
  private String groupId;
  @Index
  private String requesterId;
  private String title;
  private String description;
  private List<String> keywords;
  @Index
  private Date expirationDate;
  private Integer reward; //Price in cents
  private Integer timeAlloted; //Time in seconds
  private List<String> qualificationsRequired;
  private String hitContent;
  @Index
  private Date firstSeen;
  @Index
  private Date lastSeen;
  @Index
  private boolean active = true;
  
  public HITgroup(String groupId, String requesterId, String title,
      String description, List<String> keywords, Date expirationDate,
      Integer reward, Integer timeAlloted, List<String> qualificationsRequired, 
      String hitContent, Date firstSeen, Date lastSeen) {
    this.groupId = groupId;
    this.requesterId = requesterId;
    this.title = title;
    this.description = description;
    this.keywords = keywords;
    this.expirationDate = expirationDate;
    this.reward = reward;
    this.timeAlloted = timeAlloted;
    this.qualificationsRequired = qualificationsRequired;
    this.hitContent = hitContent;
    this.firstSeen = firstSeen;
    this.lastSeen = lastSeen;
  }
  
  //for objectify
  @SuppressWarnings("unused")
  private HITgroup(){}

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getRequesterId() {
    return requesterId;
  }

  public void setRequesterId(String requesterId) {
    this.requesterId = requesterId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getKeywords() {
    return keywords;
  }

  public void setKeywords(List<String> keywords) {
    this.keywords = keywords;
  }

  public Date getExpirationDate() {
    return expirationDate;
  }

  public void setExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
  }

  public Integer getReward() {
    return reward;
  }

  public void setReward(Integer reward) {
    this.reward = reward;
  }

  public Integer getTimeAlloted() {
    return timeAlloted;
  }

  public void setTimeAlloted(Integer timeAlloted) {
    this.timeAlloted = timeAlloted;
  }

  public List<String> getQualificationsRequired() {
    return qualificationsRequired;
  }

  public void setQualificationsRequired(List<String> qualificationsRequired) {
    this.qualificationsRequired = qualificationsRequired;
  }

  public String getHitContent() {
    return hitContent;
  }

  public void setHitContent(String hitContent) {
    this.hitContent = hitContent;
  }

  public Date getFirstSeen() {
    return firstSeen;
  }

  public void setFirstSeen(Date firstSeen) {
    this.firstSeen = firstSeen;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Date getLastSeen() {
    return lastSeen;
  }

  public void setLastSeen(Date lastSeen) {
    this.lastSeen = lastSeen;
  }

}
