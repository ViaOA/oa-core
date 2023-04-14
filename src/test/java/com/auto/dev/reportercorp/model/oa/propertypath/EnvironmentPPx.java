package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.Environment;

public class EnvironmentPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public EnvironmentPPx(String name) {
		this(null, name);
	}

	public EnvironmentPPx(PPxInterface parent, String name) {
		String s = null;
		if (parent != null) {
			s = parent.toString();
		}
		if (s == null) {
			s = "";
		}
		if (name != null && name.length() > 0) {
			if (s.length() > 0 && name.charAt(0) != ':') {
				s += ".";
			}
			s += name;
		}
		pp = s;
	}

	public EnvironmentSnapshotPPx environmentSnapshots() {
		EnvironmentSnapshotPPx ppx = new EnvironmentSnapshotPPx(this, Environment.P_EnvironmentSnapshots);
		return ppx;
	}

	public ReporterCorpPPx reporterCorps() {
		ReporterCorpPPx ppx = new ReporterCorpPPx(this, Environment.P_ReporterCorps);
		return ppx;
	}

	public String id() {
		return pp + "." + Environment.P_Id;
	}

	public String created() {
		return pp + "." + Environment.P_Created;
	}

	public String type() {
		return pp + "." + Environment.P_Type;
	}

	public String name() {
		return pp + "." + Environment.P_Name;
	}

	public String nodeCount() {
		return pp + "." + Environment.P_NodeCount;
	}

	public String loadBalanceUrl() {
		return pp + "." + Environment.P_LoadBalanceUrl;
	}

	public String templateUrl() {
		return pp + "." + Environment.P_TemplateUrl;
	}

	public String jdbcUrl() {
		return pp + "." + Environment.P_JdbcUrl;
	}

	public String color() {
		return pp + "." + Environment.P_Color;
	}

	public String isProduction() {
		return pp + "." + Environment.P_IsProduction;
	}

	public String currentRuntimeEnvironment() {
		return pp + "." + Environment.P_CurrentRuntimeEnvironment;
	}

	public String updateReporterCorps() {
		return pp + ".updateReporterCorps";
	}

	public String pauseAll() {
		return pp + ".pauseAll";
	}

	public String unpauseAll() {
		return pp + ".unpauseAll";
	}

	public String clearCaches() {
		return pp + ".clearCaches";
	}

	public String createSnapshot() {
		return pp + ".createSnapshot";
	}

	public String setDefaults() {
		return pp + ".setDefaults";
	}

	public EnvironmentPPx nonProdFilter() {
		EnvironmentPPx ppx = new EnvironmentPPx(this, ":nonProd()");
		return ppx;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
