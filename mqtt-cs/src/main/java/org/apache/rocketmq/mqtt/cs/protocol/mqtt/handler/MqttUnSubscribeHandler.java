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

package org.apache.rocketmq.mqtt.cs.protocol.mqtt.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribePayload;
import org.apache.rocketmq.mqtt.common.hook.HookResult;
import org.apache.rocketmq.mqtt.common.model.Subscription;
import org.apache.rocketmq.mqtt.common.util.TopicUtils;
import org.apache.rocketmq.mqtt.cs.channel.ChannelCloseFrom;
import org.apache.rocketmq.mqtt.cs.channel.ChannelInfo;
import org.apache.rocketmq.mqtt.cs.channel.ChannelManager;
import org.apache.rocketmq.mqtt.cs.protocol.mqtt.MqttPacketHandler;
import org.apache.rocketmq.mqtt.cs.protocol.mqtt.facotry.MqttMessageFactory;
import org.apache.rocketmq.mqtt.cs.session.loop.SessionLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

@Component
public class MqttUnSubscribeHandler implements MqttPacketHandler<MqttUnsubscribeMessage> {
    private static Logger logger = LoggerFactory.getLogger(MqttUnSubscribeHandler.class);

    @Resource
    private SessionLoop sessionLoop;

    @Resource
    private ChannelManager channelManager;

    @Override
    public boolean preHandler(ChannelHandlerContext ctx, MqttUnsubscribeMessage mqttMessage) {
        return true;
    }

    @Override
    public void doHandler(ChannelHandlerContext ctx, MqttUnsubscribeMessage mqttMessage, HookResult upstreamHookResult) {
        String clientId = ChannelInfo.getClientId(ctx.channel());
        Channel channel = ctx.channel();
        String remark = upstreamHookResult.getRemark();
        if (!upstreamHookResult.isSuccess()) {
            channelManager.closeConnect(channel, ChannelCloseFrom.SERVER, remark);
            return;
        }
        try {
            MqttUnsubscribePayload payload = mqttMessage.payload();
            if (payload.topics() != null && !payload.topics().isEmpty()) {
                Set<Subscription> subscriptions = new HashSet<>();
                for (String topic : payload.topics()) {
                    subscriptions.add(new Subscription(TopicUtils.normalizeTopic(topic)));
                }
                sessionLoop.removeSubscription(ChannelInfo.getId(ctx.channel()), subscriptions);
            }
            int messageId = mqttMessage.variableHeader().messageId();
            channel.writeAndFlush(MqttMessageFactory.buildUnsubAckMessage(messageId));
        } catch (Exception e) {
            logger.error("UnSubscribe:{}", clientId, e);
            channelManager.closeConnect(channel, ChannelCloseFrom.SERVER, "UnSubscribeException");
        }
    }

}
