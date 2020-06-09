package com.itheima.leyou.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itheima.leyou.dao.IOrderDao;
import com.sun.xml.internal.ws.spi.db.DatabindingException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderServiceImpl implements IOrderService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private IOrderDao iOrderDao;

    public Map<String, Object> createOrder(String sku_id, String user_id){

        Map<String, Object> resultMap = new HashMap<String, Object>();

        //1、传入参数的判断
        if (sku_id==null||sku_id.equals("")){
            resultMap.put("result", false);
            resultMap.put("msg", "传入的参数有误！");
            return resultMap;
        }

        if (user_id==null||user_id.equals("")){
            resultMap.put("result", false);
            resultMap.put("msg", "会员必须登录才能购买！");
            return resultMap;
        }

        String order_id = String.valueOf(System.currentTimeMillis());

        //2、取秒杀政策
        String policy = stringRedisTemplate.opsForValue().get("LIMIT_POLICY_"+sku_id);

        if (policy!=null&&!policy.equals("")){
            //3、判断政策时间，开始时间<=当前时间，当前时间<=结束时间
            Map<String, Object> policyInfo = JSONObject.parseObject(policy, Map.class);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String now = restTemplate.getForObject("http://leyou-time-server/getTime", String.class);
            try {
                Date begin_time = simpleDateFormat.parse(policyInfo.get("begin_time").toString());
                Date end_time = simpleDateFormat.parse(policyInfo.get("end_time").toString());
                Date now_time = simpleDateFormat.parse(now);

                if (begin_time.getTime() <= now_time.getTime() && now_time.getTime() <= end_time.getTime()){

                    long limitQuanty = Long.parseLong(policyInfo.get("quanty").toString());

                    //4、redis计数，限制购买数量
                    // +1+1+1   --- 999  +1  +1   1-10000相加
                    if (stringRedisTemplate.opsForValue().increment("SKU_QUANTY_"+sku_id, 1) <= limitQuanty){
                        //5、写入订单队列，并且写入redis

                        // tb_order: order_id, total_fee, actual_fee, post_fee, payment_type, user_id, status, create_time
                        // tb_order_detail: order_id, sku_id, num, title, own_spec, price, image, create_time
                        // tb_sku: sku_id, title, images, stock, price, indexes, own_spec

                        //1000人

                        String sku = stringRedisTemplate.opsForValue().get("SKU_"+sku_id);
                        Map<String, Object> skuMap = JSONObject.parseObject(sku, Map.class);

                        //创建订单的Map
                        Map<String, Object> orderMap = new HashMap<String, Object>();
                        orderMap.put("order_id", order_id);
                        orderMap.put("total_fee", skuMap.get("price"));
                        orderMap.put("actual_fee", policyInfo.get("price"));

                        orderMap.put("post_fee", 0);
                        orderMap.put("payment_type", 0);
                        orderMap.put("user_id", user_id);
                        orderMap.put("status", 1);
                        orderMap.put("create_time", now);

                        orderMap.put("sku_id", sku_id);
                        orderMap.put("num", 1);
                        orderMap.put("title", skuMap.get("title"));
                        orderMap.put("own_spec", skuMap.get("own_spec"));
                        orderMap.put("price", policyInfo.get("price"));
                        orderMap.put("image", skuMap.get("images"));

                        String order = JSON.toJSONString(orderMap);
                        try {
                            amqpTemplate.convertAndSend("order_queue", order);
                            stringRedisTemplate.opsForValue().set("ORDER_"+order_id, order);
                        }catch (Exception e){
                            resultMap.put("result", false);
                            resultMap.put("msg", "队列写入失败了，你真的点儿背！"+e.getMessage());
                            return resultMap;
                        }

                    }else {
                        //6、如果超出购买数量，返回商品已经售完
                        resultMap.put("result", false);
                        resultMap.put("msg", "商品已经售完，踢回去了3亿9！");
                        return resultMap;
                    }
                }else {
                    //7、时间判断有误，返回活动已经过期
                    resultMap.put("result", false);
                    resultMap.put("msg", "活动已经过期！");
                    return resultMap;
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }else {
            //8、当没有取出政策的时候，返回活动已经过期
            resultMap.put("result", false);
            resultMap.put("msg", "活动已经过期！");
            return resultMap;
        }

        //9、返回正常信息，包含order_id
        resultMap.put("order_id", order_id);
        resultMap.put("result", true);
        resultMap.put("msg", "订单创建成功！");
        return resultMap;
    }

    public Map<String, Object> insertOrder(Map<String, Object> orderInfo) {
        return iOrderDao.insertOrder(orderInfo);
    }



    public Map<String, Object> getOrder(String order_id){
        String order = stringRedisTemplate.opsForValue().get("ORDER_"+order_id);
        return JSONObject.parseObject(order, Map.class);
    }



    public Map<String, Object> payOrder(String order_id, String sku_id){
        Map<String, Object> resultMap = new HashMap<String, Object>();

        try {
            amqpTemplate.convertAndSend("order_status_queue", order_id);
            amqpTemplate.convertAndSend("storage_queue", sku_id);
        }catch (Exception e){
            resultMap.put("result", false);
            resultMap.put("msg", "写入队列失败！");
            return resultMap;
        }

        resultMap.put("result", true);
        resultMap.put("msg", "");
        return resultMap;
    }

    public Map<String, Object> updateOrderStatus(String order_id) {
        return iOrderDao.updateOrderStatus(order_id);
    }
}
