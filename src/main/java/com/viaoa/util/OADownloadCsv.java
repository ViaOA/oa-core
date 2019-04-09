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

import java.util.ArrayList;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Create csv data from a Hub and properies.
 * 
 * todo:  allow for Many, and create multiple lines per hub object.
 * 
 * @author vvia
 */
public abstract class OADownloadCsv<F extends OAObject> {
    protected Hub<F> hub;
    private ArrayList<MyProperty> alProperty = new ArrayList<>();
    
    public OADownloadCsv(Hub<F> hub) {
        this.hub = hub;
    }

    protected static class MyProperty {
        String title;
        String propPath;
        OAPropertyPath pp;
    }

    public void addProperty(String title, String propPath) {
        MyProperty mp = new MyProperty();
        mp.title = title;
        mp.propPath = propPath;
        mp.pp = new OAPropertyPath(hub.getObjectClass(), propPath);
        
        mp.pp.getLinkInfos();
        alProperty.add(mp);
    }
    
    public void download() {
        writeHeading();
        for (F obj : hub) {
            writeData(obj);
        }
    }
    
    protected void writeHeading() {
        String txt = "";
        for (MyProperty mp : alProperty) {
            txt = OAString.csv(txt, mp.title);
        }
        onWriteLine(txt);
    }
    
    protected void writeData(F obj) {
        String txt = "";
        for (MyProperty mp : alProperty) {
            Object val = mp.pp.getValue(obj);
            txt = OAString.csv(txt, val);
        }
        onWriteLine(txt);
    }
    
    protected abstract void onWriteLine(String txt);
    
}
