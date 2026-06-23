package com.viaoa.graph.api.internal;

import java.lang.reflect.Method;

import com.viaoa.graph.api.internal.objects.OAObjectAnnotationOps;
import com.viaoa.graph.api.internal.objects.OAObjectAutoAddOps;
import com.viaoa.graph.api.internal.objects.OAObjectCacheOps;
import com.viaoa.graph.api.internal.objects.OAObjectCallbackOps;
import com.viaoa.graph.api.internal.objects.OAObjectChangeOps;
import com.viaoa.graph.api.internal.objects.OAObjectCSOps;
import com.viaoa.graph.api.internal.objects.OAObjectDSOps;
import com.viaoa.graph.api.internal.objects.OAObjectDeleteOps;
import com.viaoa.graph.api.internal.objects.OAObjectEnumOps;
import com.viaoa.graph.api.internal.objects.OAObjectEventOps;
import com.viaoa.graph.api.internal.objects.OAObjectFindOps;
import com.viaoa.graph.api.internal.objects.OAObjectGuidOps;
import com.viaoa.graph.api.internal.objects.OAObjectHubOps;
import com.viaoa.graph.api.internal.objects.OAObjectInfoOps;
import com.viaoa.graph.api.internal.objects.OAObjectInitializeOps;
import com.viaoa.graph.api.internal.objects.OAObjectKeyOps;
import com.viaoa.graph.api.internal.objects.OAObjectLockOps;
import com.viaoa.graph.api.internal.objects.OAObjectPropertyOps;
import com.viaoa.graph.api.internal.objects.OAObjectReflectOps;
import com.viaoa.graph.api.internal.objects.OAObjectSaveOps;
import com.viaoa.graph.api.internal.objects.OAObjectSchedulerOps;
import com.viaoa.graph.api.internal.objects.OAObjectSerializeOps;
import com.viaoa.graph.api.internal.objects.OAObjectSiblingOps;
import com.viaoa.graph.api.internal.objects.OAObjectStateOps;
import com.viaoa.graph.api.internal.objects.OAObjectUniqueOps;

public interface ObjectsOps {

	public OAObjectAnnotationOps annotation();

	public OAObjectAutoAddOps autoAdd();
	
	public OAObjectCacheOps cache();

	public OAObjectCallbackOps callbacks();

	public OAObjectChangeOps change();
	
	public OAObjectCSOps cs();

	public OAObjectDeleteOps delete();

	public OAObjectDSOps ds();

	public OAObjectEnumOps enumx();
	
	public OAObjectEventOps event();

	public OAObjectFindOps find();
	
	public OAObjectGuidOps guid();

	public OAObjectHubOps hub();

	public OAObjectInfoOps info();

	public OAObjectInitializeOps initialize();

	public OAObjectKeyOps key();

	public OAObjectLockOps lock();
	
	public OAObjectPropertyOps property();

	public OAObjectReflectOps reflect();
	
	public OAObjectSaveOps save();
	
	public OAObjectSchedulerOps scheduler();

	public OAObjectSerializeOps serialize();
	
	public OAObjectSiblingOps sibling();

	public OAObjectStateOps state();
	
	public OAObjectUniqueOps unique();

}
