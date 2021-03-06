/**
 * Automates concurrent and parallel processing through a single real socket connection.
 * <p>
 * Socket API for multiple "virtual" sockets to use a single physical socket.
 * <p>
 * Works just like a socket and server socket, but allows them to share a real socket. This is a good solution for load balancing solutions
 * that work through firewalls, and proxies. Includes solutions for throttling, "chunking"/fairness.
 * <p>
 * This allows a single "real" socket connection from a client to a server, and then getting many "virtual" socket connections through the
 * real socket.<br>
 * The virtual socket supports the full socket interface, so it's no different then using a real socket.
 * <p>
 * The server uses a server socket for allowing client socket "real" connections. The server sets up server sockets and gives them each a
 * name. Then the client is able to create new "virtual" socket connections by using the correct server socket name. Everything else is the
 * same as using the socket api.
 * <p>
 * The shared "real" socket from the client to the server uses chunking and throttling to make the sharing fair and even. There is support
 * for "compression on the wire" that is configured when a server socket is created.
 * <p>
 * There is support for SSL/TLS for the real connection or for individual sockets, configured by the server socket. Multiplexer socket that
 * allows multiple virtual sockets be used with one real socket.
 * <p>
 *
 * @author vvia
 */
package com.viaoa.comm.multiplexer;