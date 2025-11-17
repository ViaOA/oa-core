/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
package com.viaoa.sync.remote;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;

/**
 * Callback interface implemented by the client and invoked by the server to
 * deliver out-of-band notifications.
 * <p>
 * Methods are invoked outside of the normal sync queue, using direct socket
 * writes. Typical uses include:
 * <ul>
 *   <li>terminating the client connection with a message,</li>
 *   <li>simple connectivity pings,</li>
 *   <li>dumping server thread stacks for diagnostics.</li>
 * </ul>
 *
 * <p>
 * All methods are routed through the multiplexer using non-queued or
 * low-latency semantics to avoid ordering delays relative to ordinary sync
 * messages.
 */

@OARemoteInterface()
public interface RemoteClientCallbackInterface {
    
    @OARemoteMethod(noReturnValue=true, timeoutSeconds=2, dontUseQueue=true)
    void stop(String title, String msg);
    
    @OARemoteMethod(dontUseQueue=true)
    String ping(String msg);
    
    @OARemoteMethod(dontUseQueue=true)
    public String performThreadDump(String msg);

}
