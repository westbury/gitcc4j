package gitcc.cc;

import java.util.Arrays;

public class CCVersion {

	private final String version;

	public CCVersion(String version) {
		this.version = version;
	}

	public String getBranch() {
		String[] branches = getBranches();
		return branches[branches.length - 1];
	}

	public String[] getBranches() {
		// TODO: figure out correct separator a better way.
		String[] branches = version.split("/");
		if (branches.length == 1) {
			// Try again with Windows separator
			branches = version.split("\\\\");
		}
		return Arrays.copyOfRange(branches, 1, branches.length - 1);
	}

	public String getFullVersion() {
		return version;
	}

	@Override
	public boolean equals(Object obj) {
		return ((CCVersion) obj).version.equals(version);
	}
}
