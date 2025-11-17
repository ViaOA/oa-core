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
package com.viaoa.func;

/**
 * Callback interface for receiving string messages. Used by components that
 * generate or collect formatted text and need a simple sink to which messages
 * can be appended. Implementations may accumulate text, forward it to a log,
 * or stream it to an external target.
 */

public interface StringCallback {
	void add(String msg);
}
