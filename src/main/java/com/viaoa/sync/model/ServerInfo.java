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
package com.viaoa.sync.model;

import java.io.Serializable;

import com.viaoa.datetime.OADateTime;

/**
 * Describes a running instance of an {@code OASyncServer}.
 * <p>
 * A {@code ServerInfo} object is typically created when the server starts and
 * remains available for diagnostics, discovery, and administration. It
 * contains:
 * <ul>
 *   <li>creation timestamp,</li>
 *   <li>server host and IP address,</li>
 *   <li>server version string,</li>
 *   <li>whether server discovery is enabled,</li>
 *   <li>whether the server has been started or suspended.</li>
 * </ul>
 *
 * <h2>Lifecycle Flags</h2>
 * <ul>
 *   <li>{@code started} indicates whether the server initialization sequence
 *       has completed.</li>
 *   <li>{@code suspended} allows administrative tools to temporarily suppress
 *       processing of certain operations.</li>
 * </ul>
 *
 * <p>
 * Like {@link ClientInfo}, this class is a pure data container used across
 * the sync subsystem for reporting and monitoring.
 */
public class ServerInfo implements Serializable{
    private static final long serialVersionUID = 1L;
     
    /**
     * Timestamp indicating when the server instance was created.
     */
    protected OADateTime created;
    
    /**
     * Host name of the server.
     */
    protected String hostName;

    /**
     * IP address of the server.
     */
    protected String ipAddress;
    
    /**
     * Version string of the server software.
     */
    protected String version;
    
    /**
     * Flag indicating whether server discovery is enabled.
     */
    protected boolean discoveryEnabled;
    
    /**
     * Flag indicating whether the server has been started.
     */
    private volatile boolean started;

    /**
     * Flag indicating whether the server is currently suspended.
     */
    private volatile boolean suspended;

    
    /**
     * Creates a new {@code ServerInfo} instance.
     */
    public ServerInfo() {
    }

    /**
     * Returns the server creation timestamp.
     *
     * @return the {@code created} timestamp
     */
    public OADateTime getCreated() {
        return created;
    }

    /**
     * Sets the server creation timestamp.
     *
     * @param newValue the creation timestamp to set
     */
    public void setCreated(OADateTime newValue) {
        this.created = newValue;
    }
    
    /**
     * Returns the server IP address.
     *
     * @return the server IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Sets the server IP address.
     *
     * @param newValue the IP address to set
     */
    public void setIpAddress(String newValue) {
        this.ipAddress = newValue;
    }
    
    /**
     * Returns the server host name.
     *
     * @return the server host name
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Sets the server host name.
     *
     * @param newValue the host name to set
     */
    public void setHostName(String newValue) {
        this.hostName = newValue;
    }
    
    /**
     * Returns the server version string.
     *
     * @return the version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the server version string.
     *
     * @param newValue the version string to set
     */
    public void setVersion(String newValue) {
        this.version = newValue;
    }
    
    /**
     * Returns whether the server has been started.
     *
     * @return {@code true} if the server has started, otherwise {@code false}
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Sets whether the server has been started.
     *
     * @param started {@code true} to mark the server as started
     */
    public void setStarted(boolean started) {
        this.started = started;
    }

    /**
     * Returns whether the server is currently suspended.
     *
     * @return {@code true} if the server is suspended, otherwise {@code false}
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Sets whether the server is currently suspended.
     *
     * @param suspended {@code true} to suspend the server, {@code false} to resume it
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }
    
    /**
     * Returns whether server discovery is enabled.
     *
     * @return {@code true} if discovery is enabled, otherwise {@code false}
     */
    public boolean isDiscoveryEnabled() {
        return discoveryEnabled;
    }

    /**
     * Sets whether server discovery is enabled.
     *
     * @param b {@code true} to enable discovery, {@code false} to disable it
     */
    public void setDiscoveryEnabled(boolean b) {
        this.discoveryEnabled = b;
    }
}
