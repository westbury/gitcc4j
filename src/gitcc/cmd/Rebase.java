package gitcc.cmd;

import gitcc.cc.CCCommit;
import gitcc.cc.CCFile;
import gitcc.cc.CCFile.Status;
import gitcc.util.ExecException;
import gitcc.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class Rebase extends Command {

	@Override
	public void execute() throws Exception {
		cc.rebase();
		String branch = config.getBranch();
		boolean normal = branch != null;
		Date since = normal ? git.getCommitDate(config.getCC()) : null;
		Collection<CCCommit> commits = cc.getHistory(since);
		if (commits.isEmpty())
			return;
		// TODO Git fast import
		if (normal)
			git.checkout(config.getCC());
		for (CCCommit c : commits) {
			handleFiles(c.getFiles(), c.getFiles());
			git.commit(c, config.getUser(c.getAuthor()));
		}
		if (normal) {
			git.rebase(config.getCI(), config.getCC());
			git.rebase(config.getCC(), branch);
		} else {
			git.branch(config.getCC());
		}
		git.tag(config.getCI(), config.getCC());
	}

	private void handleFiles(List<CCFile> all, List<CCFile> files)
			throws Exception {
		for (CCFile f : files) {
			if (f.getStatus() == Status.Directory) {
				handleFiles(all, cc.diffPred(f));
			} else if (f.getStatus() == Status.Added) {
				add(all, f);
			} else {
				remove(f);
			}
		}
	}

	private void add(List<CCFile> all, CCFile f) throws Exception {
		File newFile = getFile(all, f);
		if (newFile != null) {
			File dest = new File(git.getRoot(), f.getFile());
			copyFile(newFile, dest);
			try {
				git.add(f.getFile());
			} catch (ExecException e) {
				if (e.getMessage().contains("The following paths are ignored"))
					return;
				throw e;
			}
		}
	}

	private File getFile(List<CCFile> all, CCFile f) {
		File newFile;
		if (!f.hasVersion()) {
			newFile = handleRename(all, f);
		} else {
			newFile = cc.get(f);
		}
		return newFile;
	}

	private void copyFile(File newFile, File dest) throws IOException,
			FileNotFoundException {
		if (dest.exists() && !dest.delete())
			throw new RuntimeException("Could not delete file: " + dest);
		dest.getParentFile().mkdirs();
		if (!newFile.renameTo(dest)) {
			try {
				IOUtils.copy(new FileInputStream(newFile),
						new FileOutputStream(dest));
			} finally {
				newFile.delete();
			}
		}
	}

	private void remove(CCFile f) {
		if (new File(git.getRoot(), f.getFile()).exists())
			git.remove(f.getFile());
	}

	private File handleRename(List<CCFile> all, CCFile f) {
		for (CCFile file : all) {
			if (file.getFile().equals(f.getFile())) {
				return null;
			}
		}

		// This is the lazy approach, but 9 times out of 10 it'll be fine.
		// The probably is that the git history will have a slight anomaly.
		// The important thing is that the working tree will be correct by the
		// end of this rebase.
		File newFile = cc.toFile(f.getFile());
		return newFile.exists() ? newFile : null;
	}
}
