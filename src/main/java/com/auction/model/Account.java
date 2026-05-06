package com.auction.model;

public abstract class Account {
    protected String id;
    protected String username;
    protected String password;
    protected String role;// Thêm mới

    public Account(String id, String username, String password , String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }



    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getId() { return id; }


    public abstract String displayRole();
}

