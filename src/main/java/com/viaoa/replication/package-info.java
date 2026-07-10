/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Durable replication support for distributing OA sync messages between a replication master and replication clients.
 * <p>
 * Replication captures OA runtime sync messages, assigns master/client sequence numbers, writes transaction-log records,
 * and replays messages across reconnect boundaries. It builds on the sync and remote-multiplexer packages but owns the
 * durable catch-up behavior needed for eventually consistent OAObject and Hub state.
 * </p>
 * <p>
 * {@link com.viaoa.replication.OAReplicationMaster} receives client-originated messages and forwards master messages to
 * registered clients. {@link com.viaoa.replication.OAReplicationClient} connects to a master, sends local messages, and
 * applies messages received from the master. {@link com.viaoa.replication.OAReplTLog} is the serialized transaction-log
 * record used by both sides.
 * </p>
 *
 * @see com.viaoa.replication.OAReplicationBase
 * @see com.viaoa.replication.OAReplicationMaster
 * @see com.viaoa.replication.OAReplicationClient
 * @see com.viaoa.replication.OAReplTLog
 */
package com.viaoa.replication;
