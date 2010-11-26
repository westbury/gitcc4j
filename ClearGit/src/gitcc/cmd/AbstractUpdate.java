package gitcc.cmd;

import gitcc.config.Config;
import gitcc.config.User;
import gitcc.git.GitCommit;
import gitcc.git.GitUtil;
import gitcc.util.ExecException;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Base class for both the Update command and the Daemon command.
 */
public abstract class AbstractUpdate extends Command {

	private static final String SINCE = "gitcc.since";

	private long lastRebase;

	@Override
	public void init() {
		super.init();
		git.checkout(Config.DEFAULT_MASTER);
		git.setConfig("receive.denyCurrentBranch", "true");
		// git.setConfig("receive.denyNonFastForwards", "true");
	}

	/**
	 * Will only happen if our merge went bad and we haven't fixed it.
	 */
	protected boolean sanityCheck(String b1, String b2) {
		String cc = git.getId(b1);
		String base = git.mergeBase(b1, b2);
		return base.equals(cc);
	}

	protected void singlePass() throws Exception {
		if (getDeliverLock().exists())
			throw new RuntimeException("Deliver in progress.");
		pull();
		if (!sanityCheck(config.getCC(), config.getBranch())) {
			String error = "Repository is in a bad state. Wake me up when you fix it.";
			throw new RuntimeException(error);
		}
		checkin();
		doRebase();
	}

	private void doRebase() throws Exception {
		long time = new Date().getTime();
		if (time - lastRebase > config.getRebaseTime()) {
			rebase();
			lastRebase = time;
		}
	}

	private void checkin() throws Exception {
		exec(new Checkin() {
			@Override
			protected void checkin(List<GitCommit> log) throws Exception {
				// Only checkin one at a time so we can change users
				for (List<GitCommit> l : GitUtil.splitLogByUser(log)) {
					GitCommit c = l.get(0);
					User user = config.getUserByEmail(c.getAuthor());
					if (user == null)
						throw new RuntimeException("User not found: "
								+ c.getAuthor());
					cc = cc.cloneForUser(user);
					// Swap back to integration user
					cc.sync();
					super.checkin(l);
				}
			}

			@Override
			protected void deliver() {
				File file = getDeliverLock();
				try {
					file.createNewFile();
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
				for (int i = 0; i < 2; i++) {
					try {
						super.deliver();
						file.delete();
						return;
					} catch (RuntimeException e) {
						if (i == 1)
							throw e;
						cc.rebase();
					}
				}
			}
		});
	}

	private void rebase() throws Exception {
		git.checkout(config.getCC());
		exec(new Rebase() {

			private long since;

			@Override
			protected void doRebase() {
				String branch_cc = config.getCC();
				try {
					rebase(branch_cc);
					String branch = config.getBranch();
					if (sanityCheck(branch, branch_cc)) {
						git.checkout(branch);
						git.merge(branch_cc);
						try {
							push();
						} catch (ExecException e) {
							System.out.println(e.getMessage());
							return;
						}
					}
					git.setConfig(SINCE, Long.toString(since));
					git.gc();
				} finally {
					git.checkoutForce(branch_cc);
				}
			}

			/**
			 * ClearCase may have a slightly different version of events if a
			 * file was modified and then deleted. By comparing the tree we can
			 * avoid unnecessary rebase errors.
			 */
			private void rebase(String branch_cc) {
				String ci = config.getCI();
				if (git.diffTree(ci, branch_cc).length() == 0) {
					git.resetHard(ci);
				} else {
					git.rebase(ci, branch_cc);
				}
			}

			@Override
			protected Date getSince() {
				Date lastSince = null;
				try {
					lastSince = new Date(Long.parseLong(git.getConfig(SINCE)));
				} catch (Exception e) {
					// Ignore
				}
				since = new Date().getTime();
				return lastSince != null ? lastSince : super.getSince();
			}
		});
	}

	private void pull() {
		if (config.getRemote() != null) {
			git.checkout(config.getBranch());
			git.pullRebase(config.getRemote());
		}
	}

	private void push() {
		if (config.getRemote() != null) {
			git.checkout(config.getBranch());
			git.push(config.getRemote());
		}
	}

	private File getDeliverLock() {
		return new File(git.getRoot(), ".git/deliver");
	}

	private void exec(Command cmd) throws Exception {
		init(cmd);
		cmd.init();
		cmd.execute();
	}
}
