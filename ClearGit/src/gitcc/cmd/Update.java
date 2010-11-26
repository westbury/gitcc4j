package gitcc.cmd;

public class Update extends AbstractUpdate {

	@Override
	public void execute() throws Exception {
		singlePass();
	}
}
