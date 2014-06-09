package com.tracker.entity;

import java.util.Date;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
public class HITrequester {

    @Id
    private String requesterId;
    private String requesterName;
    @Index
    private Date lastActivity;

    //for objectify
    @SuppressWarnings("unused")
    private HITrequester(){}

    public HITrequester(String requesterId, String requesterName, Date lastActivity) {
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.lastActivity = lastActivity;
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

    public Date getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Date lastActivity) {
        this.lastActivity = lastActivity;
    }

}
