package com.tracker.entity;

import java.util.Date;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
public class HITinstance {

  @Id
  private String id;
  @Index
  private String groupId;
  @Index
  private Date timestamp; 
  private Integer hitsAvailable;
  private Integer hitsDiff;
  private Integer rewardsAvailable;
  private Integer rewardDiff;
  
  public HITinstance(String groupId, Date timestamp, Integer hitsAvailable, 
      Integer hitsDiff, Integer rewardsAvailable, Integer rewardDiff) {
    this.id = groupId + "_" + timestamp.getTime();
    this.groupId = groupId;
    this.timestamp = timestamp;
    this.hitsAvailable = hitsAvailable;
    this.hitsDiff = hitsDiff;
    this.rewardsAvailable = rewardsAvailable;
    this.rewardDiff = rewardDiff;
  }
  
  //for objectify
  @SuppressWarnings("unused")
  private HITinstance(){}

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public Integer getHitsAvailable() {
    return hitsAvailable;
  }

  public void setHitsAvailable(Integer hitsAvailable) {
    this.hitsAvailable = hitsAvailable;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Integer getHitsDiff() {
    return hitsDiff;
  }

  public void setHitsDiff(Integer hitsDiff) {
    this.hitsDiff = hitsDiff;
  }

  public Integer getRewardDiff() {
    return rewardDiff;
  }

  public void setRewardDiff(Integer rewardDiff) {
	  this.rewardDiff = rewardDiff;
  }

  public Integer getRewardsAvailable() {
	  return rewardsAvailable;
  }

  public void setRewardsAvailable(Integer rewardsAvailable) {
	  this.rewardsAvailable = rewardsAvailable;
  } 
  
}
