package gitcc.cmd;

import gitcc.Log;
import gitcc.util.EmailUtil;

/**
 * At the moment this is a simple loop of checkin + rebase + sleep.
 * <p>
 * At any stage if something goes wrong, most importantly a merge conflict, we
 * should instantly try to send for help and wait. Don't try to get smart.
 * <p>
 * The important part occurs for each rebase. We have to consider the
 * possibility that someone will decide to push at exactly the same time. My
 * solution at the moment is to use receive.denyCurrentBranch to (hopefully)
 * stop this happening. The other alternative might be not let git do _any_
 * merges and instead try to push to Clearcase and then rebase. I figure that
 * this shouldn't happen too often either way and we don't really want to have
 * to constantly tend to the daemon at every minor conflict.
 */
public class Daemon extends AbstractUpdate {

	private EmailUtil email;

	@Override
	public void init() {
		super.init();
		email = new EmailUtil(config);
	}

	@Override
	public void execute() throws Exception {
		while (true) {
			try {
				singlePass();
			} catch (Exception e) {
				e.printStackTrace();
				email.send(e);
				System.exit(1);
			}
			Log.info("Sleeping...");
			Thread.sleep(config.getSleep());
		}
	}
}
