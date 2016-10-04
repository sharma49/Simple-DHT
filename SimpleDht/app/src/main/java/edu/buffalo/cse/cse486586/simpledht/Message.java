package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/** A simple chord message. */
public class Message implements Serializable {
    public int myPort;
    public String type;
    public int pred;
    public int succ;
    public ConcurrentHashMap<String, String> h;
    ArrayList<Integer> arr;
    public String key;
    public String value;
    public int origin;
}