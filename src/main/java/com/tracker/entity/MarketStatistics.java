package com.tracker.entity;

import java.util.Date;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
@Index
public class MarketStatistics {
  
  @Id
  private String id;
  private Date timestamp; 
  private Integer hitGroupsAvailable;
  private Integer hitsAvailable;
  
  public MarketStatistics(String id, Date timestamp,
      Integer hitGroupsAvailable, Integer hitsAvailable) {
    this.id = id;
    this.timestamp = timestamp;
    this.hitGroupsAvailable = hitGroupsAvailable;
    this.hitsAvailable = hitsAvailable;
  }
  
  //for objectify
  @SuppressWarnings("unused")
  private MarketStatistics(){}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public Integer getHitGroupsAvailable() {
    return hitGroupsAvailable;
  }

  public void setHitGroupsAvailable(Integer hitGroupsAvailable) {
    this.hitGroupsAvailable = hitGroupsAvailable;
  }

  public Integer getHitsAvailable() {
    return hitsAvailable;
  }

  public void setHitsAvailable(Integer hitsAvailable) {
    this.hitsAvailable = hitsAvailable;
  }
  
}
