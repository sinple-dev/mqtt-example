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

package org.apache.rocketmq.mqtt.meta.raft.processor;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.mqtt.common.meta.MetaConstants;
import org.apache.rocketmq.mqtt.common.model.consistency.ReadRequest;
import org.apache.rocketmq.mqtt.common.model.consistency.Response;
import org.apache.rocketmq.mqtt.common.model.consistency.WriteRequest;
import org.apache.rocketmq.mqtt.common.util.StatUtil;
import org.apache.rocketmq.mqtt.meta.raft.MqttRaftServer;
import org.apache.rocketmq.mqtt.meta.raft.MqttStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.mqtt.common.meta.MetaConstants.CATEGORY_WILL_MSG;
import static org.apache.rocketmq.mqtt.common.meta.MetaConstants.WILL_REQ_READ_END_KEY;
import static org.apache.rocketmq.mqtt.common.meta.MetaConstants.WILL_REQ_READ_GET;
import static org.apache.rocketmq.mqtt.common.meta.MetaConstants.WILL_REQ_READ_SCAN;
import static org.apache.rocketmq.mqtt.common.meta.MetaConstants.WILL_REQ_READ_SCAN_NUM;
import static org.apache.rocketmq.mqtt.common.meta.MetaConstants.WILL_REQ_READ_START_KEY;
import static org.apache.rocketmq.mqtt.common.meta.MetaConstants.WILL_REQ_WRITE_COMPARE_AND_PUT;
import static org.apache.rocketmq.mqtt.common.meta.MetaConstants.WILL_REQ_WRITE_DELETE;
import static org.apache.rocketmq.mqtt.common.meta.MetaConstants.WILL_REQ_WRITE_EXPECT_VALUE;
import static org.apache.rocketmq.mqtt.common.meta.MetaConstants.WILL_REQ_WRITE_PUT;

public class WillMsgStateProcessor extends StateProcessor {
    private static Logger logger = LoggerFactory.getLogger(WillMsgStateProcessor.class);
    private MqttRaftServer server;

    public WillMsgStateProcessor(MqttRaftServer server) {
        this.server = server;
    }

    @Override
    public Response onReadRequest(ReadRequest request) throws Exception {
        long start = System.currentTimeMillis();
        try {
            MqttStateMachine sm = server.getMqttStateMachine(request.getGroup());
            if (sm == null) {
                logger.error("Fail to process will ReadRequest , Not Found SM for {}", request.getGroup());
                return null;
            }
            String operation = request.getOperation();
            String key = request.getKey();
            if (WILL_REQ_READ_GET.equals(operation)) {
                return get(sm.getRocksDBEngine(), key.getBytes());
            } else if (WILL_REQ_READ_SCAN.equals(operation)) {
                String startKey = request.getExtDataMap().get(WILL_REQ_READ_START_KEY);
                String endKey = request.getExtDataMap().get(WILL_REQ_READ_END_KEY);
                String scanNumStr = request.getExtDataMap().get(WILL_REQ_READ_SCAN_NUM);
                long scanNum = server.getMetaConf().getScanNum();
                if (StringUtils.isNotBlank(scanNumStr)) {
                    scanNum = Long.parseLong(scanNumStr);
                }
                return scan(sm.getRocksDBEngine(), startKey.getBytes(), endKey.getBytes(), scanNum);
            }
        } catch (Throwable e) {
            if (request.getKey() == null) {
                logger.error("Fail to delete, startKey {}, endKey {}", request.getExtDataMap().get("startKey"), request.getExtDataMap().get("endKey"), e);
            } else {
                logger.error("Fail to process will ReadRequest, k {}", request.getKey(), e);
            }
            throw e;
        } finally {
            StatUtil.addInvoke("WillRead", System.currentTimeMillis() - start);
        }

        return null;
    }

    @Override
    public Response onWriteRequest(WriteRequest log) throws Exception {
        long start = System.currentTimeMillis();
        try {
            MqttStateMachine sm = server.getMqttStateMachine(log.getGroup());
            if (sm == null) {
                logger.error("Fail to process will WriteRequest , Not Found SM for {}", log.getGroup());
                return null;
            }
            String operation = log.getOperation();
            String key = log.getKey();
            byte[] value = log.getData().toByteArray();

            if (WILL_REQ_WRITE_PUT.equals(operation)) {
                return put(sm.getRocksDBEngine(), key.getBytes(), value);
            } else if (WILL_REQ_WRITE_DELETE.equals(operation)) {
                return delete(sm.getRocksDBEngine(), key.getBytes());
            } else if (WILL_REQ_WRITE_COMPARE_AND_PUT.equals(operation)) {
                String expectValue = log.getExtDataMap().get(WILL_REQ_WRITE_EXPECT_VALUE);
                if (MetaConstants.NOT_FOUND.equals(expectValue)) {
                    return compareAndPut(sm.getRocksDBEngine(), key.getBytes(), null, value);
                }
                return compareAndPut(sm.getRocksDBEngine(), key.getBytes(), log.getExtDataMap().get("expectValue").getBytes(), value);
            }
        } catch (Throwable e) {
            logger.error("Fail to process will WriteRequest, k {}", log.getKey(), e);
            throw e;
        } finally {
            StatUtil.addInvoke("WillWrite", System.currentTimeMillis() - start);
        }
        return null;
    }

    @Override
    public String groupCategory() {
        return CATEGORY_WILL_MSG;
    }

}
