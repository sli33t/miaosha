package com.itheima.leyou.queue;

import com.itheima.leyou.service.IStorageService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StorageQueue {

    @Autowired
    private IStorageService iStorageService;

    @RabbitListener(queues = "storage_queue")
    public void insertStorage(String msg){
        //1、接收到消息并输出
        System.out.println("storage_queue接收消息："+msg);

        //2、调用IStorageService的方法， insertStorage
        Map<String, Object> resultMap = iStorageService.insertStorage(msg, 0, 1);

        //3、如果执行失败，输出失败信息
        if (!(Boolean) resultMap.get("result")){
            System.out.println("storage_queue执行失败："+resultMap.get("msg").toString());

            ///写入数据库日志表
        }else {
            //4、如果执行成功，输出成功信息
            System.out.println("storage_queue执行成功!");
        }
    }
}
