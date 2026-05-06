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
 * Describes a single client connected to an {@code OASyncServer}.
 * <p>
 * A {@code ClientInfo} instance is created when a client connects and is
 * updated throughout the lifetime of the connection. It provides:
 * <ul>
 *   <li>connection identity ({@code connectionId}),</li>
 *   <li>lifecycle timestamps ({@code created}, {@code disconnected}),</li>
 *   <li>client host and IP address,</li>
 *   <li>server host/port the client connected to,</li>
 *   <li>user identity fields ({@code userId}, {@code userName}, {@code location}),</li>
 *   <li>runtime statistics (request count and total request time),</li>
 *   <li>client memory usage, version, and remote thread count.</li>
 * </ul>
 *
 * <h2>Runtime Usage</h2>
 * {@code ClientInfo} objects are:
 * <ul>
 *   <li>created by the server during handshake,</li>
 *   <li>updated periodically by the client (via heartbeat/update messages),</li>
 *   <li>read by administrative tools or monitoring dashboards,</li>
 *   <li>finalized when the client disconnects.</li>
 * </ul>
 *
 * <p>
 * This class contains no behavior beyond storage; it is intentionally kept
 * lightweight and serializable for transmission across the remoting layer.
 */
public class ClientInfo implements Serializable{
    private static final long serialVersionUID = 1L;

    /**
     * Connection identifier assigned by the server for this client.
     */
    protected int connectionId = -1;

    /**
     * Timestamp indicating when the client connection was created.
     */
    protected OADateTime created;
    
    /**
     * Timestamp indicating when the client disconnected.
     */
    protected OADateTime disconnected;

    /**
     * Flag indicating whether the client session has been started.
     */
    protected volatile boolean started;
    
    /**
     * Host name of the client machine.
     */
    protected String hostName;
    
    /**
     * IP address of the client machine.
     */
    protected String ipAddress;

    /**
     * Host name of the server the client connected to.
     */
    protected String serverHostName;
    
    /**
     * Port of the server the client connected to.
     */
    protected int serverHostPort;
    
    /**
     * Total number of requests recorded for this client session.
     */
    protected volatile int totalRequests;
    
    /**
     * Accumulated request processing time in nanoseconds.
     */
    protected volatile long totalRequestTime; // nanoseconds

    /**
     * User identifier associated with this client connection.
     */
    protected String userId;
    
    /**
     * User name associated with this client connection.
     */
    protected String userName;
    
    /**
     * Location label associated with this client connection.
     */
    protected String location;
    
    /**
     * Total memory reported by the client runtime.
     */
    protected long totalMemory;
    
    /**
     * Free memory reported by the client runtime.
     */
    protected long freeMemory;
    
    /**
     * Client application version string.
     */
    protected String version;
    
    /**
     * Count of remote threads reported by the client.
     */
    protected int remoteThreadCount;
    
    /**
     * Creates a new, empty {@code ClientInfo} instance.
     */
    public ClientInfo() {
    }
    
    /**
     * Returns the connection creation timestamp.
     *
     * @return the {@code created} timestamp
     */
    public OADateTime getCreated() {
        return created;
    }

    /**
     * Returns the connection creation timestamp.
     *
     * @return the {@code created} timestamp
     */
    public void setCreated(OADateTime newValue) {
        this.created = newValue;
    }
    
    /**
     * Returns the connection creation timestamp.
     *
     * @return the {@code created} timestamp
     */
    public String getIpAddress() {
        return ipAddress;
    }
    /**
     * Sets the IP address of the client.
     *
     * @param newValue the IP address to set
     */
    public void setIpAddress(String newValue) {
        this.ipAddress = newValue;
    }
    
    /**
     * Returns the host name of the client.
     *
     * @return the client host name
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Sets the host name of the client.
     *
     * @param newValue the host name to set
     */
    public void setHostName(String newValue) {
        this.hostName = newValue;
    }
    
    /**
     * Returns the server-assigned connection identifier.
     *
     * @return the connection identifier
     */
    public int getConnectionId() {
        return connectionId;
    }

    /**
     * Sets the server-assigned connection identifier.
     *
     * @param connectionId the connection identifier to set
     */
    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    /**
     * Returns the disconnection timestamp.
     *
     * @return the disconnection timestamp
     */
    public OADateTime getDisconnected() {
        return disconnected;
    }

    /**
     * Sets the disconnection timestamp.
     *
     * @param disconnected the disconnection timestamp to set
     */
    public void setDisconnected(OADateTime disconnected) {
        this.disconnected = disconnected;
    }
    
    /**
     * Returns the total number of requests processed for this client.
     *
     * @return the total request count
     */
    public int getTotalRequests() {
        return totalRequests;
    }

    /**
     * Sets the total number of requests processed for this client.
     *
     * @param totalRequests the total request count to set
     */
    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    /**
     * Increments the total request count by one.
     */
    public void incrementTotalRequests() {
        this.totalRequests++;
    }
    
    /**
     * Returns the accumulated request processing time.
     *
     * @return the total request time in nanoseconds
     */
    public long getTotalRequestTime() {
        return totalRequestTime;
    }
    /**
     * Sets the accumulated request processing time.
     *
     * @param totalRequestTime the total request time in nanoseconds
     */
    public void setTotalRequestTime(long totalRequestTime) {
        this.totalRequestTime = totalRequestTime;
    }

    /**
     * Adds elapsed time to the accumulated request processing time.
     *
     * @param nsTime the elapsed time in nanoseconds
     */
    public void incrementTotalRequestTime(long nsTime) {
        this.totalRequestTime += nsTime;
    }


    /**
     * Returns the server host name the client connected to.
     *
     * @return the server host name
     */
    public String getServerHostName() {
        return serverHostName;
    }

    /**
     * Sets the server host name the client connected to.
     *
     * @param gsmrServerHostName the server host name to set
     */
    public void setServerHostName(String gsmrServerHostName) {
        this.serverHostName = gsmrServerHostName;
    }

    /**
     * Returns the server host port the client connected to.
     *
     * @return the server host port
     */
    public int getServerHostPort() {
        return serverHostPort;
    }
    
    /**
     * Sets the server host port the client connected to.
     *
     * @param gsmrServerHostPort the server host port to set
     */
    public void setServerHostPort(int gsmrServerHostPort) {
        this.serverHostPort = gsmrServerHostPort;
    }

        
    /**
     * Returns the remote thread count reported by the client.
     *
     * @return the remote thread count
     */
    public int getRemoteThreadCount() {
        return remoteThreadCount;
    }

    /**
     * Sets the remote thread count reported by the client.
     *
     * @param remoteThreadCount the remote thread count to set
     */
    public void setRemoteThreadCount(int remoteThreadCount) {
        this.remoteThreadCount = remoteThreadCount;
    }
    
    /**
     * Returns whether the client session has been started.
     *
     * @return {@code true} if the session has started, otherwise {@code false}
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Sets whether the client session has been started.
     *
     * @param started {@code true} to mark the session as started
     */
    public void setStarted(boolean started) {
        this.started = started;
    }

    /**
     * Returns the user identifier associated with this client.
     *
     * @return the user identifier
     */
    public String getUserId() {
        return userId;
    }
    /**
     * Sets the user identifier associated with this client.
     *
     * @param user the user identifier to set
     */
    public void setUserId(String user) {
        this.userId = user;
    }

    /**
     * Returns the user name associated with this client.
     *
     * @return the user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the user name associated with this client.
     *
     * @param user the user name to set
     */
    public void setUserName(String user) {
        this.userName = user;
    }
    
    /**
     * Returns the location associated with this client.
     *
     * @return the location
     */
    public String getLocation() {
        return location;
    }
    /**
     * Sets the location associated with this client.
     *
     * @param loc the location to set
     */
    public void setLocation(String loc) {
        this.location = loc;
    }

    /**
     * Returns the total memory reported by the client.
     *
     * @return the total memory value
     */
    public long getTotalMemory() {
        return totalMemory;
    }
    /**
     * Sets the total memory reported by the client.
     *
     * @param totalMemory the total memory value to set
     */
    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }
    
    /**
     * Returns the free memory reported by the client.
     *
     * @return the free memory value
     */
    public long getFreeMemory() {
        return freeMemory;
    }

    /**
     * Sets the free memory reported by the client.
     *
     * @param freeMemory the free memory value to set
     */
    public void setFreeMemory(long freeMemory) {
        this.freeMemory = freeMemory;
    }

    /**
     * Returns the client application version.
     *
     * @return the version string
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Sets the client application version.
     *
     * @param newValue the version string to set
     */
    public void setVersion(String newValue) {
        this.version = newValue;
    }
}
