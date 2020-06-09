package com.itheima.leyou.dao;

import java.util.Map;

public interface IOrderDao {


    Map<String,Object> insertOrder(Map<String, Object> orderInfo);

    Map<String,Object> updateOrderStatus(String order_id);
}
