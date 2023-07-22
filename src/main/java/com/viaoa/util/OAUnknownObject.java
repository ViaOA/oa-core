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

/** 
    Object used internally to represent unknown value (equal to nothing except itself). 
*/
public class OAUnknownObject implements java.io.Serializable {
    static final long serialVersionUID = 1L;
    public static final OAUnknownObject instance = new OAUnknownObject();
    
    private OAUnknownObject() {
    }
    
    public OAUnknownObject getUnknownObject() {
        return instance;
    }
    
    @Override
    public boolean equals(Object obj) {
        return (obj == instance);
    }
    @Override
    public int hashCode() {
        return 1;
    }
}
