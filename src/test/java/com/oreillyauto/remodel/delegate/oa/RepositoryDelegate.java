package com.oreillyauto.remodel.delegate.oa;

import com.oreillyauto.remodel.model.oa.Project;
import com.oreillyauto.remodel.model.oa.Repository;
import com.viaoa.util.OAFile;
import com.viaoa.util.OAString;

public class RepositoryDelegate {

	public static String getFullFileName(Repository program) {
		if (program == null) {
			return null;
		}
		String fn = program.getFileName();
		if (OAString.isEmpty(fn)) {
			return null;
		}
		if (fn.indexOf('/') < 0 && fn.indexOf('\\') < 0) {

			String s = program.getPackageName();
			if (OAString.isNotEmpty(s)) {
				s = OAString.convert(s, ".", "/");
				fn = s + "/" + fn;
			}

			Project project = program.getProject();
			if (project != null) {
				s = project.getCodeDirectory();
				if (OAString.isNotEmpty(s)) {
					fn = s + "/" + fn;
				}
			}
		}
		fn = OAFile.convertFileName(fn);
		return fn;
	}

}
