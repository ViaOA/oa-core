package test.hifive.model.delegate;

import com.viaoa.datasource.jdbc.db.Database;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

import test.hifive.model.oa.Ecard;

public class OAObjectAnnotationDelegate {

	public static void update(Database database, Class<? extends OAObject>[] classes) throws Exception {
		((OAObjectService) OARuntime.graph(Ecard.class).objects()).getOAObjectDatabaseService().update(database, classes);
	}

}
