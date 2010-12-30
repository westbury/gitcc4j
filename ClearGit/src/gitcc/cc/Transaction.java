package gitcc.cc;

import gitcc.Common;
import gitcc.git.FileStatus;
import gitcc.git.GitCommit;
import gitcc.util.CheckinException;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Transaction extends Common {

	protected Set<String> checkouts = new LinkedHashSet<String>();
	protected List<String> dirs = new ArrayList<String>();
	private final String id;
	private final String commit;
	private final List<FileStatus> statuses;
	private String activity;
	private String mergeBase;

	public Transaction(GitCommit commit, List<FileStatus> statuses) {
		this.id = commit.getId();
		this.commit = commit.getMessage();
		this.statuses = statuses;
	}

	public void process() {
		activity = cc.mkact(GitCommit.getSubject(commit));
		mergeBase = git.mergeBase(config.getCI(), config.getBranch());
		try {
			phase1();
			mkdirs();
			phase2();
		} catch (RuntimeException e) {
			try {
				rollback();
			} catch (RuntimeException ex) {
				ex.printStackTrace();
			}
			throw e;
		}
		commit();
	}

	private void phase1() {
		for (FileStatus s : statuses) {
			String file = s.getFile();
			switch (s.getStatus()) {
			case Added:
				stageDir(file);
				break;
			case Deleted:
				stageDir(file, false);
				break;
			case Renamed:
				stage(s.getOldFile());
				stageDir(s.getOldFile());
				stageDir(file);
				break;
			case Modified:
				stage(s.getFile());
				break;
			}
		}
	}

	private void stage(String file) {
		if (checkout(file)) {
			String hash = git.hashObject(cc.toFile(file).getAbsolutePath());
			String blob = git.getBlob(mergeBase, file);
			if (!hash.equals(blob)) {
				String s = "File has been modified: %s. Try rebasing.";
				throw new CheckinException(String.format(s, file));
			}
		}
	}

	private boolean checkout(String oldFile) {
		if (checkouts.contains(oldFile))
			return false;
		try {
			cc.checkout(oldFile);
		} catch (RuntimeException e) {
			String message = e.getMessage();
			if (message.contains("Version discordance detected")
					|| message.contains("is already checked out")) {
				cc.uncheckout(oldFile);
				cc.checkout(oldFile);
			} else {
				throw e;
			}
		}
		checkouts.add(oldFile);
		return true;
	}

	private void stageDir(String f) {
		stageDir(f, true);
	}

	private void stageDir(String f, boolean add) {
		File dir = new File(f).getParentFile();
		if (add) {
			while (dir != null && !cc.exists(dir.getPath())) {
				dirs.add(dir.getPath());
				dir = dir.getParentFile();
			}
		}
		checkout(dir == null ? "." : dir.getPath());
	}

	private void rollback() {
		for (String f : reverseCheckouts()) {
			cc.uncheckout(f);
		}
		cc.rmact(activity);
	}

	/**
	 * Need to reverse, otherwise we will have problems with newly added folders
	 * + files.
	 */
	private Iterable<String> reverseCheckouts() {
		List<String> reverse = new ArrayList<String>(checkouts);
		Collections.reverse(reverse);
		return reverse;
	}

	private void phase2() {
		for (FileStatus s : statuses) {
			switch (s.getStatus()) {
			case Added:
				write(s);
				cc.add(s.getFile(), commit);
				break;
			case Deleted:
				cc.delete(s.getFile());
				break;
			case Renamed:
				String oldFile = s.getOldFile();
				String file = s.getFile();
				cc.move(oldFile, file);
				checkouts.remove(oldFile);
				checkouts.add(file);
				write(s);
				break;
			case Modified:
				write(s);
				break;
			}
		}
	}

	private void mkdirs() {
		Collections.reverse(dirs);
		for (String dir : dirs) {
			if (!cc.exists(dir)) {
				cc.mkdir(dir);
				checkouts.add(dir);
			}
		}
	}

	private void commit() {
		for (String f : checkouts) {
			cc.checkin(f, commit);
		}
	}

	private void write(FileStatus s) {
		byte[] bytes = git.catFile(id, s.getFile());
		File file = cc.toFile(s.getFile());
		try {
			FileOutputStream writer = new FileOutputStream(file);
			writer.write(bytes);
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
