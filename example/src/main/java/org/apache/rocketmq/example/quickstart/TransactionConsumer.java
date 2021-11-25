package org.apache.rocketmq.example.quickstart;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * @author : wh
 * @date : 2021/11/1 11:26
 * @description:
 */
public class TransactionConsumer {


    public static void start() {
        new Thread(() -> {

            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("test_group");
            consumer.setNamesrvAddr("127.0.0.1");
            try {
                consumer.subscribe("test_topic", "*");
            } catch (MQClientException e) {
                e.printStackTrace();
            }

            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                    // 代表收到订单创建成功
                    // 1. 核销优惠券 2. 扣除礼品卡


                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });

        }).start();
    }

}
