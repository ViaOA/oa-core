package test.xice.tsam.model.delegate;

import com.viaoa.datasource.jdbc.db.Database;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import test.xice.tsac.model.oa.GSMRClient;

public class OAObjectAnnotationDelegate {

	public static void update(Database database, Class<? extends OAObject>[] classes) throws Exception {
		((OAObjectService) OARuntime.graph(GSMRClient.class).objects()).getOAObjectDatabaseService().update(database, classes);
	}

}
