package com.itheima.leyou.dao;

import java.util.ArrayList;
import java.util.Map;

public interface IStockDao {

    /**
     * 取商品列表，为了前端页面展示
     * @return ArrayList，包含多个商品，每个商品是一个Map
     */
    public ArrayList<Map<String, Object>> getStockList();

    /**
     * 取商品详情，为了前端页面展示
     * @param sku_id
     * @return ArrayList，包含一个商品，这个商品是一个Map
     */
    public ArrayList<Map<String, Object>> getStock(String sku_id);

    public boolean insertLimitPolicy(Map<String, Object> policyInfo);
}
