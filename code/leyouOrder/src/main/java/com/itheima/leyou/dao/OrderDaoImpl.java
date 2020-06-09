package com.itheima.leyou.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class OrderDaoImpl implements IOrderDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> insertOrder(Map<String, Object> orderInfo) {
        Map<String, Object> resultMap = new HashMap<String, Object>();

        //1、创建写入主表的SQL
        String sql = "insert into tb_order (order_id, total_fee, actual_fee, post_fee, payment_type, user_id, status, create_time) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?)";

        //2、执行这个SQL
        boolean result = jdbcTemplate.update(sql, orderInfo.get("order_id"), orderInfo.get("total_fee"), orderInfo.get("actual_fee"),
                orderInfo.get("post_fee"), orderInfo.get("payment_type"), orderInfo.get("user_id"), orderInfo.get("status"),
                orderInfo.get("create_time"))==1;

        if (!result){
            resultMap.put("result", false);
            resultMap.put("msg", "写入订单主表时失败！");
            return resultMap;
        }

        //3、创建写入明细表SQL
        sql = "INSERT INTO tb_order_detail (order_id, sku_id, num, title, own_spec, price, image, create_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        //4、执行
        result = jdbcTemplate.update(sql, orderInfo.get("order_id"), orderInfo.get("sku_id"), orderInfo.get("num"),
                orderInfo.get("title"), orderInfo.get("own_spec"), orderInfo.get("price"), orderInfo.get("image"),
                orderInfo.get("create_time"))==1;
        if (!result){
            resultMap.put("result", false);
            resultMap.put("msg", "写入订单明细表时失败！");
            return resultMap;
        }

        //5、返回
        resultMap.put("result", true);
        resultMap.put("msg", "");
        return resultMap;
    }

    public Map<String, Object> updateOrderStatus(String order_id) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String sql = "update tb_order set status = 2 where order_id = ?";
        boolean reult = jdbcTemplate.update(sql, order_id)==1;

        if (!reult){
            resultMap.put("result", false);
            resultMap.put("msg", "更新订单状态时失败！");
            return resultMap;
        }

        resultMap.put("result", true);
        resultMap.put("msg", "");
        return resultMap;
    }


}
