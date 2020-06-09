package com.itheima.leyou.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itheima.leyou.dao.IStockDao;
import jdk.jfr.events.ExceptionThrownEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class StockServiceImpl implements IStockService{

    @Autowired
    private IStockDao iStockDao;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 取商品列表，为了前端页面展示
     * @return ArrayList，包含多个商品，每个商品是一个Map
     */
    public Map<String, Object> getStockList(){
        Map<String, Object> resultMap = new HashMap<String, Object>();

        //1、取dao层的方法
        ArrayList<Map<String, Object>> list = iStockDao.getStockList();

        //2、如果没取出数据，返回错误信息
        if (list==null||list.size()==0){
            resultMap.put("result", false);
            resultMap.put("msg", "没有取出来数据！");
            return resultMap;
        }

        resultMap = getLimitPolicy(list);

        //4、如果取出来，返回正常信息
        resultMap.put("sku_list", list);
//        resultMap.put("result", true);
//        resultMap.put("msg", "");
        return resultMap;
    }


    public Map<String, Object> getStock(String sku_id){
        Map<String, Object> resultMap = new HashMap<String, Object>();

        //1、判断传入的参数
        if (sku_id==null||sku_id.equals("")){
            resultMap.put("result", false);
            resultMap.put("msg", "前端传入的什么东东？");
            return resultMap;
        }

        //2、取自dao层的方法
        ArrayList<Map<String, Object>> list = iStockDao.getStock(sku_id);

        //3、如果没有取出数据，返回错误信息
        if (list==null||list.size()==0){
            resultMap.put("result", false);
            resultMap.put("msg", "没有取出来数据！");
            return resultMap;
        }

        resultMap = getLimitPolicy(list);

        //4、返回正常信息
        resultMap.put("sku", list);
//        resultMap.put("result", true);
//        resultMap.put("msg", "");
        return resultMap;
    }


    @Transactional
    public Map<String, Object> insertLimitPolicy(Map<String, Object> policyInfo){
        Map<String, Object> resultMap = new HashMap<String, Object>();

        //1、传入参数判断
        if (policyInfo==null||policyInfo.isEmpty()){
            resultMap.put("result", false);
            resultMap.put("msg", "前端传入的什么东东？");
            return resultMap;
        }

        //2、写入数据库，调dao层的方法
        boolean result = iStockDao.insertLimitPolicy(policyInfo);

        //3、如果写入失败，返回错误信息
        if (!result){
            resultMap.put("result", false);
            resultMap.put("msg", "数据库没有写入成功！");
            return resultMap;
        }

        //4、写入redis
        // 有效期，  11:00-11:10   10分钟，7分钟   end_time - 当前时间（取自时间服务器）
        // 政策写入redis ，  商品写入redis
        long diff = 0;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String now = restTemplate.getForObject("http://leyou-time-server/getTime", String.class);
        try {
            Date end_time = simpleDateFormat.parse(policyInfo.get("end_time").toString());
            Date now_time = simpleDateFormat.parse(now);

            diff = (end_time.getTime() - now_time.getTime()) / 1000;

            if (diff<0){
                resultMap.put("result", false);
                resultMap.put("msg", "结束时间不能小于当前时间！");
                return resultMap;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            String policy = JSON.toJSONString(policyInfo);
            stringRedisTemplate.opsForValue().set("LIMIT_POLICY_"+policyInfo.get("sku_id").toString(), policy, diff, TimeUnit.SECONDS);

            ArrayList<Map<String, Object>> list = iStockDao.getStock(policyInfo.get("sku_id").toString());
            String sku = JSON.toJSONString(list.get(0));
            stringRedisTemplate.opsForValue().set("SKU_"+policyInfo.get("sku_id").toString(), sku, diff, TimeUnit.SECONDS);
        }catch (Exception e){
            resultMap.put("result", false);
            resultMap.put("msg", "写入redis失败！");
            return resultMap;
        }


        //5、返回正常信息
        resultMap.put("result", true);
        resultMap.put("msg", "写入政策成功！");
        return resultMap;
    }


    private Map<String, Object> getLimitPolicy(ArrayList<Map<String, Object>> list){
        Map<String, Object> resultMap = new HashMap<String, Object>();


        //3、取redis政策
        //循环list
        for (Map<String, Object> skuMap: list){
            //3.1 取政策
            String policy = stringRedisTemplate.opsForValue().get("LIMIT_POLICY_"+skuMap.get("sku_id").toString());

            //3.2 没有取到政策直接跳过，能取到政策就赋值
            if (policy!=null&&!policy.equals("")){
                //3.3 判断开始时间<=当前时间，当前时间<=结束时间
                // limitQuanty   limitPrice  limitBeginTime   limitEndTime

                Map<String, Object> policyInfo = JSONObject.parseObject(policy, Map.class);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String now = restTemplate.getForObject("http://leyou-time-server/getTime", String.class);
                try {
                    Date begin_time = simpleDateFormat.parse(policyInfo.get("begin_time").toString());
                    Date end_time = simpleDateFormat.parse(policyInfo.get("end_time").toString());
                    Date now_time = simpleDateFormat.parse(now);

                    if (begin_time.getTime()<=now_time.getTime() && now_time.getTime() <= end_time.getTime()){
                        skuMap.put("limitQuanty", policyInfo.get("quanty"));
                        skuMap.put("limitPrice", policyInfo.get("price"));
                        skuMap.put("limitBeginTime", policyInfo.get("begin_time"));
                        skuMap.put("limitEndTime", policyInfo.get("end_time"));
                        skuMap.put("nowTime", now);
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }

        }

        resultMap.put("result", true);
        resultMap.put("msg", "");
        return resultMap;
    }
}
