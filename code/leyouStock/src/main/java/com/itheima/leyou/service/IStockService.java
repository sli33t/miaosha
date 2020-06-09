package com.itheima.leyou.service;

import java.util.Map;

public interface IStockService {

    /**
     * 取商品列表，为了前端页面展示
     * @return ArrayList，包含多个商品，每个商品是一个Map
     */
    public Map<String, Object> getStockList();

    public Map<String, Object> getStock(String sku_id);

    public Map<String, Object> insertLimitPolicy(Map<String, Object> policyInfo);
}
