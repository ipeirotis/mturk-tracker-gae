package com.tracker.entity;

import java.io.Serializable;
import java.util.Date;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
@SuppressWarnings("serial")
public class ArrivalCompletions implements Serializable{

	@Id
	private Long id;
	@Index
	private Date from;
	@Index
	private Date to;
	//private Integer hitGroupsAvailable;
	private Double hitGroupsAvailableUI;
	private Long hitGroupsArrived;
	private Long hitGroupsCompleted;
	//private Double hitsAvailable;
	private Double hitsAvailableUI;
	private Long hitsArrived;
	private Long hitsCompleted;
	//private Double rewardsAvailable;
	private Long rewardsArrived;
	private Long rewardsCompleted;
	private Integer length;

	public ArrivalCompletions(Date from, Date to) {
		this.from = from;
		this.to = to;
	}

	// for objectify
	@SuppressWarnings("unused")
	private ArrivalCompletions() {
	}

	public Double getHitGroupsAvailableUI() {
	    return hitGroupsAvailableUI;
	}

	public void setHitGroupsAvailableUI(Double hitGroupsAvailableUI) {
	    this.hitGroupsAvailableUI = hitGroupsAvailableUI;
	}


	public Double getHitsAvailableUI() {
	    return hitsAvailableUI;
	}

	public void setHitsAvailableUI(Double hitsAvailableUI) {
	    this.hitsAvailableUI = hitsAvailableUI;
	}


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

	public Long getHitGroupsArrived() {
		return hitGroupsArrived;
	}

	public void setHitGroupsArrived(Long hitGroupsArrived) {
		this.hitGroupsArrived = hitGroupsArrived;
	}

	public Long getHitGroupsCompleted() {
		return hitGroupsCompleted;
	}

	public void setHitGroupsCompleted(Long hitGroupsCompleted) {
		this.hitGroupsCompleted = hitGroupsCompleted;
	}

	public Long getHitsArrived() {
		return hitsArrived;
	}

	public void setHitsArrived(Long hitsArrived) {
		this.hitsArrived = hitsArrived;
	}

	public Long getHitsCompleted() {
		return hitsCompleted;
	}

	public void setHitsCompleted(Long hitsCompleted) {
		this.hitsCompleted = hitsCompleted;
	}

	public Long getRewardsArrived() {
		return rewardsArrived;
	}

	public void setRewardsArrived(Long rewardsArrived) {
		this.rewardsArrived = rewardsArrived;
	}

	public Long getRewardsCompleted() {
		return rewardsCompleted;
	}

	public void setRewardsCompleted(Long rewardsCompleted) {
		this.rewardsCompleted = rewardsCompleted;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

}
