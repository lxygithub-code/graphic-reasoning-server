package com.lee.graphic_reasoning_server.common;
import lombok.Data;

import java.util.List;

@Data
public class PageVO<T> {
    private long total;
    private long pageNum;
    private long pageSize;
    private List<T> records;

    public static <T> PageVO<T> of(long total, long pageNum, long pageSize, List<T> records) {
        PageVO<T> vo = new PageVO<>();
        vo.total = total;
        vo.pageNum = pageNum;
        vo.pageSize = pageSize;
        vo.records = records;
        return vo;
    }
}