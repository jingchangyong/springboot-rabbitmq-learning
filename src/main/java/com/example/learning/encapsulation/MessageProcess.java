package com.example.learning.encapsulation;


import com.example.learning.common.DetailRes;

/**
 * Created by littlersmall on 16/5/11.
 */
public interface MessageProcess<T> {
    DetailRes process(T message);
}