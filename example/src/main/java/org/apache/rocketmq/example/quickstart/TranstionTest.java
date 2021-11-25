package org.apache.rocketmq.example.quickstart;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;

import java.util.concurrent.Executors;

/**
 * @author : wh
 * @date : 2021/11/1 10:45
 * @description:
 */
public class TranstionTest {




    public static void main(String[] args){
        // 1. MQ生产者
        TransactionMQProducer producer = new TransactionMQProducer("please_rename_unique_group_name");
        // 设置回调线程池
        producer.setExecutorService(Executors.newSingleThreadScheduledExecutor());
        // 设置回调函数
        producer.setTransactionListener(new TransactionListenerImpl());
        // 启动生产者
        try {
            producer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }
        // 构造下单成功消息
        Message msg = new Message();
        try{
            // 2. 发送 half消息
            SendResult sendResult = producer.sendMessageInTransaction(msg,null);
        } catch(Exception e) {
            System.out.println("消息发送异常");
        }

    }



}
