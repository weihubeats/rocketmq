/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.client.latency;

import org.apache.rocketmq.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.client.log.ClientLogger;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.logging.InternalLogger;

public class MQFaultStrategy {
    private final static InternalLogger log = ClientLogger.getLog();
    private final LatencyFaultTolerance<String> latencyFaultTolerance = new LatencyFaultToleranceImpl();

    private boolean sendLatencyFaultEnable = false;

    private long[] latencyMax = {50L, 100L, 550L, 1000L, 2000L, 3000L, 15000L};
    private long[] notAvailableDuration = {0L, 0L, 30000L, 60000L, 120000L, 180000L, 600000L};

    public long[] getNotAvailableDuration() {
        return notAvailableDuration;
    }

    public void setNotAvailableDuration(final long[] notAvailableDuration) {
        this.notAvailableDuration = notAvailableDuration;
    }

    public long[] getLatencyMax() {
        return latencyMax;
    }

    public void setLatencyMax(final long[] latencyMax) {
        this.latencyMax = latencyMax;
    }

    public boolean isSendLatencyFaultEnable() {
        return sendLatencyFaultEnable;
    }

    public void setSendLatencyFaultEnable(final boolean sendLatencyFaultEnable) {
        this.sendLatencyFaultEnable = sendLatencyFaultEnable;
    }

    /**
     *
     * @param tpInfo topic相关信息
     * @param lastBrokerName 上一次选择的执行发送消息失败的Broke
     * @return
     */
    public MessageQueue selectOneMessageQueue(final TopicPublishInfo tpInfo, final String lastBrokerName) {
        // 是否启动Broker故障延迟
        /**
         * 如果开启，在随机递增取模的基础上，再过滤掉not available的Broker代理。
         * 所谓的"latencyFaultTolerance"，是指对之前失败的，按一定的时间做退避。
         * 例如，如果上次请求的latency超过550Lms，就退避3000Lms；超过1000L，就退避60000L；
         * 如果关闭，采用随机递增取模的方式选择一个队列（MessageQueue）来发送消息，
         * latencyFaultTolerance机制是实现消息发送高可用的核心关键所在
         */
        if (this.sendLatencyFaultEnable) {
            try {
                // 自增取值 简单的轮训 通过 ThreadLocal 维护
                int index = tpInfo.getSendWhichQueue().incrementAndGet();
                for (int i = 0; i < tpInfo.getMessageQueueList().size(); i++) {
                    // index与当前路由表中的对列总个数取模 简单轮询
                    int pos = Math.abs(index++) % tpInfo.getMessageQueueList().size();
                    if (pos < 0)
                        pos = 0;
                    // 获取到当前对应的待发送队列
                    MessageQueue mq = tpInfo.getMessageQueueList().get(pos);
                    // 校验队列是否可用 实际是检测Broker是否可用
                    if (latencyFaultTolerance.isAvailable(mq.getBrokerName()))
                        return mq;
                }
                // 尝试从规避的Broker中选择一个可用的Broker，如果没有找到，将返回null。
                // 不是最差随机法 根据可用、性能、解冻时间3个维度 排序
                final String notBestBroker = latencyFaultTolerance.pickOneAtLeast();
                // 取broker中的可写队列数
                int writeQueueNums = tpInfo.getQueueIdByBroker(notBestBroker);
                // 如果可写队列数>0,则选取一个队列
                if (writeQueueNums > 0) {
                    final MessageQueue mq = tpInfo.selectOneMessageQueue();
                    if (notBestBroker != null) {
                        mq.setBrokerName(notBestBroker);
                        mq.setQueueId(tpInfo.getSendWhichQueue().incrementAndGet() % writeQueueNums);
                    }
                    return mq;
                } else {
                    // // 可写队列数 <= 0 移除该broker
                    latencyFaultTolerance.remove(notBestBroker);
                }
            } catch (Exception e) {
                log.error("Error occurred when selecting message queue", e);
            }
            // 选择 messageQueue
            return tpInfo.selectOneMessageQueue();
        }

        return tpInfo.selectOneMessageQueue(lastBrokerName);
    }

    public void updateFaultItem(final String brokerName, final long currentLatency, boolean isolation) {
        // 故障
        if (this.sendLatencyFaultEnable) {
            // 如果不可用隔离时间 30s
            long duration = computeNotAvailableDuration(isolation ? 30000 : currentLatency);
            this.latencyFaultTolerance.updateFaultItem(brokerName, currentLatency, duration);
        }
    }

    private long computeNotAvailableDuration(final long currentLatency) {
        for (int i = latencyMax.length - 1; i >= 0; i--) {
            if (currentLatency >= latencyMax[i])
                return this.notAvailableDuration[i];
        }

        return 0;
    }
}
