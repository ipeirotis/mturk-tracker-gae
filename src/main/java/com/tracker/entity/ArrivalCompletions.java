package com.tracker.entity;

import java.util.Date;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
public class ArrivalCompletions {

  @Id
  private Long id;
  @Index
  private Date from;
  @Index
  private Date to;
  private Integer hitGroupsArrived;
  private Integer hitGroupsCompleted;
  private Integer hitsArrived;
  private Integer hitsCompleted;
  private Integer rewardsArrived;
  private Integer rewardsCompleted;
  
  public ArrivalCompletions(Date from, Date to,
      Integer hitGroupsArrived, Integer hitGroupsCompleted,
      Integer hitsArrived, Integer hitsCompleted, Integer rewardsArrived,
      Integer rewardsCompleted) {
    this.from = from;
    this.to = to;
    this.hitGroupsArrived = hitGroupsArrived;
    this.hitGroupsCompleted = hitGroupsCompleted;
    this.hitsArrived = hitsArrived;
    this.hitsCompleted = hitsCompleted;
    this.rewardsArrived = rewardsArrived;
    this.rewardsCompleted = rewardsCompleted;
  }
  
  //for objectify
  @SuppressWarnings("unused")
  private ArrivalCompletions(){}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Date getFrom() {
    return from;
  }

  public void setFrom(Date from) {
    this.from = from;
  }

  public Date getTo() {
    return to;
  }

  public void setTo(Date to) {
    this.to = to;
  }

  public Integer getHitGroupsArrived() {
    return hitGroupsArrived;
  }

  public void setHitGroupsArrived(Integer hitGroupsArrived) {
    this.hitGroupsArrived = hitGroupsArrived;
  }

  public Integer getHitGroupsCompleted() {
    return hitGroupsCompleted;
  }

  public void setHitGroupsCompleted(Integer hitGroupsCompleted) {
    this.hitGroupsCompleted = hitGroupsCompleted;
  }

  public Integer getHitsArrived() {
    return hitsArrived;
  }

  public void setHitsArrived(Integer hitsArrived) {
    this.hitsArrived = hitsArrived;
  }

  public Integer getHitsCompleted() {
    return hitsCompleted;
  }

  public void setHitsCompleted(Integer hitsCompleted) {
    this.hitsCompleted = hitsCompleted;
  }

  public Integer getRewardsArrived() {
    return rewardsArrived;
  }

  public void setRewardsArrived(Integer rewardsArrived) {
    this.rewardsArrived = rewardsArrived;
  }

  public Integer getRewardsCompleted() {
    return rewardsCompleted;
  }

  public void setRewardsCompleted(Integer rewardsCompleted) {
    this.rewardsCompleted = rewardsCompleted;
  }

  
}
