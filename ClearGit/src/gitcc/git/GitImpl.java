package gitcc.git;

import gitcc.cc.CCCommit;
import gitcc.config.User;
import gitcc.exec.Exec;
import gitcc.util.ExecException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GitImpl extends Exec implements Git {

	private static final String ISO_DATE = "yyyy-MM-dd'T'HH:mm:ss";
	private static final String LOG_FORMAT = "%H%x01%ae%x01%s%n%b";
	private static final String EMPTY = "<empty message>";

	private final GitUtil util = new GitUtil();

	public GitImpl(File root) {
		setCmd("git", "--no-pager");
		setRoot(root);
	}

	@Override
	public void add(String file) {
		exec("add", "--", file);
	}

	@Override
	public void remove(String file) {
		exec("rm", "-r", "--", file);
	}

	@Override
	public void commit(CCCommit commit, User user) {
		SimpleDateFormat format = new SimpleDateFormat(ISO_DATE);
		String date = format.format(commit.getDate());
		String name = user.getName();
		String email = user.getEmail();
		String[] env = new String[] { "GIT_AUTHOR_DATE=" + date,
				"GIT_COMMITTER_DATE=" + date, "GIT_AUTHOR_NAME=" + name,
				"GIT_COMMITTER_NAME=" + name, "GIT_AUTHOR_EMAIL=" + email,
				"GIT_COMMITTER_EMAIL=" + email, };
		try {
			String message = commit.getMessage();
			message = !message.trim().isEmpty() ? message : EMPTY;
			exec(env, "commit", "-m", message);
		} catch (ExecException e) {
			if (e.getMessage().contains("nothing to commit"))
				return;
			throw e;
		}
	}

	@Override
	public String getBranch() {
		return util.parseBranch(exec("branch"));
	}

	@Override
	public void rebase(String upstream, String branch) {
		exec("rebase", upstream, branch);
	}

	@Override
	public List<GitCommit> log(String range) {
		String result = exec("log", "-z", "--first-parent", "--reverse",
				"--pretty=format:" + LOG_FORMAT, range);
		return util.parseLog(result);
	}

	@Override
	public List<GitCommit> logAllDateOrderOne() {
		return util.parseLog(exec("log", "-z", "--all", "--date-order", "-n",
				"1", "--pretty=format:" + LOG_FORMAT));
	}

	@Override
	public List<FileStatus> getStatuses(GitCommit c) {
		String id = c.getId();
		String range = id + "^.." + id;
		String result = exec("diff", "--name-status", "-M", "-z",
				"--ignore-submodules", range);
		return util.parseDiff(result);
	}

	@Override
	public byte[] catFile(String sha, String file) {
		String blob = getBlob(sha, file);
		return _exec("cat-file", "blob", blob);
	}

	@Override
	public Date getCommitDate(String cc) {
		String result = exec("log", "-n", "1", "--pretty=format:%at", cc);
		if (result.trim().length() == 0) {
			return null;
		}
		return util.parseDate(result + "000");
	}

	@Override
	public void branch(String branch) {
		exec("branch", branch);
	}

	@Override
	public void branchForce(String branch, String id) {
		exec("branch", "-f", branch, id);
	}

	@Override
	public void checkout(String branch) {
		exec("checkout", branch);
	}

	@Override
	public void checkoutForce(String branch) {
		exec("checkout", "-f", branch);
	}

	@Override
	public String getBlob(String sha, String file) {
		return exec("ls-tree", "-z", sha, file).split(" ")[2].split("\t")[0];
	}

	@Override
	public String hashObject(String file) {
		return exec("hash-object", file);
	}

	@Override
	public String mergeBase(String commit1, String commit2) {
		return exec("merge-base", commit1, commit2);
	}

	@Override
	public void merge(String remote) {
		exec("merge", remote);
	}

	@Override
	public void mergeStrategyOurs(String branch) {
		exec("merge", "--strategy=ours", "--no-commit", branch);
	}

	@Override
	public String getId(String treeish) {
		return exec("log", "-n", "1", "--pretty=format:%H", treeish);
	}

	@Override
	public void setConfig(String name, String value) {
		exec("config", name, value);
	}

	@Override
	public String getConfig(String name) {
		return exec("config", name);
	}

	@Override
	public void gc() {
		exec("gc");
	}

	@Override
	public String diffTree(String commit1, String commit2) {
		return exec("diff-tree", commit1, commit2);
	}

	@Override
	public void resetHard(String treeish) {
		exec("reset", "--hard", treeish);
	}

	@Override
	public void pullRebase(String remote) {
		exec("pull", "--rebase", remote);
	}

	@Override
	public void push(String remote) {
		exec("push", remote);
	}

	@Override
	public void symolicRef(String name, String ref) {
		exec("symbolic-ref", name, ref);
	}
}
