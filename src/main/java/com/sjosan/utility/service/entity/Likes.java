package com.sjosan.utility.service.entity;

import java.io.Serializable;

public class Likes implements Serializable{

	private static final long serialVersionUID = 1L;

	long id;
	String accountID;
	int contentID;
	int categoryID;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getAccountID() {
		return accountID;
	}
	public void setAccountID(String accountID) {
		this.accountID = accountID;
	}
	public int getContentID() {
		return contentID;
	}
	public void setContentID(int contentID) {
		this.contentID = contentID;
	}
	public int getCategoryID() {
		return categoryID;
	}
	public void setCategoryID(int categoryID) {
		this.categoryID = categoryID;
	}
	
	
	
	
}
