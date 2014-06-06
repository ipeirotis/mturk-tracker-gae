package com.tracker.entity;

import java.util.Date;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
public class TopRequester {

    @Id
    private String id;
    @Index
    private String requesterId;
    private String requesterName;
    @Index
    private Integer hits;
    @Index
    private Integer reward; //Price in cents
    private Integer projects;
    @Index
    private Date timestamp;

    //for objectify
    @SuppressWarnings("unused")
    private TopRequester(){}

    public TopRequester(String requesterId,
            String requesterName, Integer hits, Integer reward,
            Integer projects, Date timestamp) {
        this.id = timestamp.getTime() + "_" + requesterId;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.hits = hits;
        this.reward = reward;
        this.projects = projects;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setGroupId(String id) {
        this.id = id;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public Integer getHits() {
        return hits;
    }

    public void setHits(Integer hits) {
        this.hits = hits;
    }

    public Integer getReward() {
        return reward;
    }

    public void setReward(Integer reward) {
        this.reward = reward;
    }

    public Integer getProjects() {
        return projects;
    }

    public void setProjects(Integer projects) {
        this.projects = projects;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    
}
