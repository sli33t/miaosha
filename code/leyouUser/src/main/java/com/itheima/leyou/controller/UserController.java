package com.itheima.leyou.controller;

import com.alibaba.fastjson.JSON;
import com.itheima.leyou.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
public class UserController {

    @Autowired
    private IUserService iUserService;

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public Map<String, Object> login(String username, String password, HttpServletRequest request){
        //1、取service的查询方法
        Map<String, Object> resultMap = iUserService.getUser(username, password);

        //2、如果没有查出来，取service的写入方法
        if (!(Boolean) resultMap.get("result")){
            resultMap = iUserService.insertUser(username, password);

            if (!(Boolean) resultMap.get("result")){
                return resultMap;
            }
        }

        //3、写入session
        HttpSession httpSession = request.getSession();
        String user = JSON.toJSONString(resultMap);
        httpSession.setAttribute("user", user);

        //4、返回正常信息
        return resultMap;
    }
}