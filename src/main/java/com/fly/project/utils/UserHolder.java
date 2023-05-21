package com.fly.project.utils;


public class UserHolder {
    private static final ThreadLocal<String> tl = new ThreadLocal<>();

    public static void saveUser(String userId){
        tl.set(userId);
    }

    public static String getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
