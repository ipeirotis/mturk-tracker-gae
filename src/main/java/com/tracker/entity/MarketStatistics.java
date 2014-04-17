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
  private Long id;
  private Date timestamp; 
  private Integer hitGroupsAvailable;
  private Integer hitsAvailable;
  
  public MarketStatistics(Date timestamp, Integer hitGroupsAvailable, Integer hitsAvailable){
    this.timestamp = timestamp;
    this.hitGroupsAvailable = hitGroupsAvailable;
    this.hitsAvailable = hitsAvailable;
  }
  
  //for objectify
  @SuppressWarnings("unused")
  private MarketStatistics(){}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
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
