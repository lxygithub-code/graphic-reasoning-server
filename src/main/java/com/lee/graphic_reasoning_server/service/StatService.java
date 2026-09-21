package com.lee.graphic_reasoning_server.service;

import com.lee.graphic_reasoning_server.vo.StatVO;

import java.util.List;
import java.util.Map;

public interface StatService {
    StatVO overview();
    Map<String, Object> growth(Integer days);
    Map<String, Object> active(Integer days);
    List<Map<String, Object>> categoryDistribution();
}
