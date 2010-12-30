package gitcc.git;

import gitcc.cc.CCCommit;
import gitcc.config.User;

import java.io.File;
import java.util.Date;
import java.util.List;

public interface Git {

	String HEAD = "HEAD";

	void add(String file);

	void remove(String file);

	void commit(CCCommit commit, User user);

	String getBranch();

	void rebase(String upstream, String branch);

	/**
	 * Upon seeing a merge commit only the first parent commit is followed. This
	 * option can give a better overview when viewing the evolution of a
	 * particular topic branch, because merges into a topic branch tend to be
	 * only about adjusting to updated upstream from time to time, and this
	 * option allows you to ignore the individual commits brought in to your
	 * history by such a merge.
	 * 
	 * @param range
	 *            <since>..<until> Show only commits between the named two
	 *            commits. When either <since> or <until> is omitted, it
	 *            defaults to HEAD, i.e. the tip of the current branch. For a
	 *            more complete list of ways to spell <since> and <until>, see
	 *            "SPECIFYING REVISIONS" section in git-rev-parse(1)
	 * @return list of commits in chronological order
	 */
	List<GitCommit> log(String range);

	List<GitCommit> logAllDateOrderOne();

	List<FileStatus> getStatuses(GitCommit c);

	/**
	 * Provides the content of an object in the repository which is of type "blob".
	 * 
	 * @param sha the sha of the commit
	 * @param file the path of the file
	 * @return the contents of the file
	 */
	byte[] catFile(String sha, String file);

	File getRoot();

	Date getCommitDate(String cc);

	void branch(String branch);

	void branchForce(String branch, String id);

	void checkout(String branch);

	void checkoutForce(String branch);

	/**
	 * finds best common ancestor(s) between two commits to use in a three-way
	 * merge. One common ancestor is better than another common ancestor if the
	 * latter is an ancestor of the former. A common ancestor that does not have
	 * any better common ancestor is a best common ancestor, i.e. a merge base.
	 * <P>
	 * Note that there can be more than one merge base for a pair of commits.
	 * If there is more than one merge base then this method returns an arbitrary
	 * one.
	 *  
	 * @param commit1
	 * @param commit2
	 * @return
	 */
	String mergeBase(String commit1, String commit2);

	/**
	 * Computes the object ID value for an object with "blob" type with the
	 * contents of the given file (which can be outside of the work tree). This
	 * is used to update the index without modifying files in the work tree.
	 * 
	 * @param file
	 * @return its object ID
	 */
	String hashObject(String file);

	/**
	 * @param sha the sha of the commit
	 * @param file the path of the file
	 * @return the object ID
	 */
	String getBlob(String sha, String file);

	String getId(String treeish);

	void setConfig(String name, String value);

	String getConfig(String name);

	void merge(String remote);

	void mergeStrategyOurs(String branch);

	void gc();

	String diffTree(String commit1, String commit2);

	void resetHard(String treeish);

	void pullRebase(String remote);

	void push(String remote);

	void symolicRef(String name, String ref);
}