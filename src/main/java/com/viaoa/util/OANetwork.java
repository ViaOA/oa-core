/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.util;

import java.net.InetAddress;
import java.net.NetworkInterface;

public class OANetwork {

    public static void findAllServers() throws Exception {
        InetAddress localhost = InetAddress.getLocalHost();

        byte[] ip = localhost.getAddress();
        
        for (int i = 210; i <= 254; i++) {
            ip[3] = (byte) i;

            System.out.println(i+") ");
            
            InetAddress address = InetAddress.getByAddress(ip);

            String s = address.getHostAddress();
            System.out.println("  "+address);
            
            if (address.isReachable(250)) {
                // machine is turned on and can be pinged
                System.out.println("  reachable using ping");
                continue;
            }
            
            System.out.println("  checking reverse DNS lookup");
            String s2 = address.getHostName();
            if (!s.equals(s2)) {
                // machine is known in a DNS lookup
                System.out.println("  reachable as "+address.getHostName());
            }
            else {
                System.out.println("  not reachable");
                // the host address and host name are equal, meaning the host name could not be resolved
            }
        }
    }
    
    
    //return current client mac address
    protected static String macAddress;
    public static String getMACAddress() throws Exception {
        if (macAddress != null) return macAddress;
        InetAddress ip;
        StringBuilder sb = new StringBuilder(32);

        ip = InetAddress.getLocalHost();
        NetworkInterface network = NetworkInterface.getByInetAddress(ip);
        byte[] mac = network.getHardwareAddress();

        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
        }
        macAddress = sb.toString();
        return macAddress;
    }
    
    
    public static void main(String[] args) throws Exception {
        findAllServers();
    }
    
}
