package org.apache.rocketmq.example.quickstart;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;

import java.util.Objects;

/**
 * @author : wh
 * @date : 2021/11/1 10:59
 * @description:
 */
public class TransactionListenerImpl implements TransactionListener {

    private static final InternalLogger log = InternalLoggerFactory.getLogger("TransactionListener");


    //如果half消息发送成功了 调用这个函数，执行本地事务
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        //如果本地事务执行成功，返回commit
        // 生成订单信息
        try {
            createOrder();
            // 本地事务执行成功， 返回 commit
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            //如果本地事务执行失败，回滚所有执行过的操作 标记half 消息 无效
            log.info(e.getMessage());
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }



    }

    /**
     * 因为各种网络原因没有 commit or rollback half消息
     * @param msg Check message
     * @return
     */
    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {

        // 查询订单是否已生成
        Integer orderId = getOrderId();
        if (Objects.isNull(orderId)) {
            return LocalTransactionState.ROLLBACK_MESSAGE;
        } else {
            return LocalTransactionState.COMMIT_MESSAGE;
        }
    }

    /**
     * 生成订单
     */
    public void createOrder() {

    }

    /**
     * 获取订单状态
     * @return
     */
    public Integer getOrderId() {
        return 1;

    }

}
