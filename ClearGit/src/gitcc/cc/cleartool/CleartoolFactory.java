package gitcc.cc.cleartool;

import gitcc.ClearcaseEnvironmentNotSetup;
import gitcc.ClearcaseFactory;
import gitcc.cc.Clearcase;
import gitcc.cc.exec.ClearcaseExec;
import gitcc.config.Config;

public class CleartoolFactory implements ClearcaseFactory {

	@Override
	public Clearcase createClearcase(Config config) throws ClearcaseEnvironmentNotSetup {
		if (!isCleartoolAvailable(config)) {
			throw new ClearcaseEnvironmentNotSetup("executing 'cleartool' command fails");
		}

		if (config.isUCM()) {
			return new gitcc.cc.exec.UCMExec();
		}
		return new gitcc.cc.exec.ClearcaseExec();
	}

	/**
	 * @return true if the cleartool command line tool is available,
	 * 			false if an attempt to run the command fails
	 */
	private boolean isCleartoolAvailable(Config config) {
		try {
			Runtime.getRuntime().exec(config.getCleartool()).destroy();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
