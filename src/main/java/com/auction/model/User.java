package com.auction.model;

public abstract class User implements Entity {
    protected String id;
    protected String username;
    protected String password;
    public User(String id, String username,String password){
        this.id = id;
        this.username = username;
        this.password = password;
    }
    public String getUsername(){
        return username;
    }
    public void setUsername(String username){
        this.username = username;
    }
    public String getPassword(){
        return password;
    }
    public void setPassword(){
        this.password = password;
    }
    public String getId(){
        return id;
    }
//    public boolean login(String inputPassword){
//        return this.password.equals(inputPassword);
//
//    }
//    public boolean register(){
//        //lưu user mới vào database thông qua lớp DAO(Data Access Object)
//        System.out.println("Đăng kí tài khoản thành công cho user: "+ this.username);
//           return true;
//    }
    public abstract String displayRole();
}
