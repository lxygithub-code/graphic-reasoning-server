package com.lee.graphic_reasoning_server.common;

public class UserContext {
    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    public static void set(Long userId) { HOLDER.set(userId); }
    public static Long get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
}