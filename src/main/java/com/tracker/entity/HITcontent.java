package com.tracker.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class HITcontent {

  @Id
  private String groupId;
  private String content;

  public HITcontent(String groupId, String content) {
    this.groupId = groupId;
    this.content = content;
  }

  //for objectify
  @SuppressWarnings("unused")
  private HITcontent(){}

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
      this.groupId = groupId;
  }

  public String getContent() {
      return content;
  }

  public void setContent(String content) {
      this.content = content;
  }

}
