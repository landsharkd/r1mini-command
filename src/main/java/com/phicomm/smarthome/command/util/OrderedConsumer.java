package com.phicomm.smarthome.command.util;

import java.util.List;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * @author qiangbin.wei
 *
 *         2017年10月31日
 */
public class OrderedConsumer {
    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("example_group_name_1");
        consumer.setNamesrvAddr("172.31.34.237:9876");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_TIMESTAMP);
        // 设定开始消费时间，默认是半个钟前
        String consumeTimestamp = UtilAll.timeMillisToHumanString3(System.currentTimeMillis());
        consumer.setConsumeTimestamp(consumeTimestamp);
        consumer.subscribe("TopicTest", "TagA");

        consumer.registerMessageListener(new MessageListenerOrderly() {

            @Override
            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
                //                context.setAutoCommit(false);
                if (msgs == null || msgs.isEmpty()) {
                    return ConsumeOrderlyStatus.SUCCESS;
                }

                for (MessageExt m : msgs) {
                    if (m == null || m.getBody() == null) {
                        break;
                    }
                    System.out.println(new String(m.getBody()));
                }

                return ConsumeOrderlyStatus.SUCCESS;

            }
        });

        consumer.start();

        System.out.printf("Consumer Started.%n");
    }
}