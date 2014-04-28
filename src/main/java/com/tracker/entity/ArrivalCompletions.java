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
	private Long hitGroupsArrived;
	private Long hitGroupsCompleted;
	private Long hitsArrived;
	private Long hitsCompleted;
	private Long rewardsArrived;
	private Long rewardsCompleted;
	private Integer length;

	public ArrivalCompletions(Date from, Date to, Long hitGroupsArrived,
			Long hitGroupsCompleted, Long hitsArrived, Long hitsCompleted,
			Long rewardsArrived, Long rewardsCompleted, Integer length) {
		this.from = from;
		this.to = to;
		this.hitGroupsArrived = hitGroupsArrived;
		this.hitGroupsCompleted = hitGroupsCompleted;
		this.hitsArrived = hitsArrived;
		this.hitsCompleted = hitsCompleted;
		this.rewardsArrived = rewardsArrived;
		this.rewardsCompleted = rewardsCompleted;
		this.length = length;
	}

	// for objectify
	@SuppressWarnings("unused")
	private ArrivalCompletions() {
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
