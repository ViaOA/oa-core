package com.viaoa.oa.api.internal;

import com.viaoa.oa.api.internal.objects.OAObjectAnnotationOps;
import com.viaoa.oa.api.internal.objects.OAObjectAutoAddOps;
import com.viaoa.oa.api.internal.objects.OAObjectCSOps;
import com.viaoa.oa.api.internal.objects.OAObjectCacheOps;
import com.viaoa.oa.api.internal.objects.OAObjectRulesOps;
import com.viaoa.oa.api.internal.objects.OAObjectChangeOps;
import com.viaoa.oa.api.internal.objects.OAObjectDSOps;
import com.viaoa.oa.api.internal.objects.OAObjectDeleteOps;
import com.viaoa.oa.api.internal.objects.OAObjectEnumOps;
import com.viaoa.oa.api.internal.objects.OAObjectEventOps;
import com.viaoa.oa.api.internal.objects.OAObjectFindOps;
import com.viaoa.oa.api.internal.objects.OAObjectGuidOps;
import com.viaoa.oa.api.internal.objects.OAObjectHubOps;
import com.viaoa.oa.api.internal.objects.OAObjectInfoOps;
import com.viaoa.oa.api.internal.objects.OAObjectInitializeOps;
import com.viaoa.oa.api.internal.objects.OAObjectKeyOps;
import com.viaoa.oa.api.internal.objects.OAObjectLockOps;
import com.viaoa.oa.api.internal.objects.OAObjectPropertyOps;
import com.viaoa.oa.api.internal.objects.OAObjectReflectOps;
import com.viaoa.oa.api.internal.objects.OAObjectSaveOps;
import com.viaoa.oa.api.internal.objects.OAObjectSchedulerOps;
import com.viaoa.oa.api.internal.objects.OAObjectSerializeOps;
import com.viaoa.oa.api.internal.objects.OAObjectSiblingOps;
import com.viaoa.oa.api.internal.objects.OAObjectStateOps;
import com.viaoa.oa.api.internal.objects.OAObjectUniqueOps;

public interface ObjectsOps {

	public OAObjectAnnotationOps annotation();

	public OAObjectAutoAddOps autoAdd();
	
	public OAObjectCacheOps cache();

	public OAObjectRulesOps rules();

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
