package com.itheima.leyou.dao;

import java.util.ArrayList;
import java.util.Map;

public interface IUserDao {

    ArrayList<Map<String, Object>> getUser(String username, String password);

    int insertUser(String username, String password);
}
