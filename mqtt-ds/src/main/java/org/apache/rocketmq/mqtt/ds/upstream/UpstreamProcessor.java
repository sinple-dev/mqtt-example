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

package org.apache.rocketmq.mqtt.ds.upstream;

import io.netty.handler.codec.mqtt.MqttMessage;
import org.apache.rocketmq.mqtt.common.hook.HookResult;
import org.apache.rocketmq.mqtt.common.model.MqttMessageUpContext;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface UpstreamProcessor {
    /**
     * process mqtt upstream packet
     * @param context
     * @param message
     * @return
     */
    CompletableFuture<HookResult> process(MqttMessageUpContext context, MqttMessage message) throws RemotingException, com.alipay.sofa.jraft.error.RemotingException, ExecutionException, InterruptedException;
}
