package com.nyasha.store.utils;

import com.nyasha.store.entities.User;

public class UserNode {
    User user;
    UserNode left;
    UserNode right;

    public UserNode(User user) {
        this.user = user;
        this.left = this.right = null;
    }
}