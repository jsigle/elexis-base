package ch.elexis.util;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import ch.elexis.data.PersistentObject;
import ch.elexis.status.ElexisStatus;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.JdbcLink.Stm;
import ch.rgw.tools.JdbcLinkException;

public class SqlWithUiRunner {
	static Log log = Log.get("SqlWithUiRunner");
	
	enum SqlStatus {
		NONE, EXECUTE, SUCCESS, FAIL
	};
	
	private List<String> sqlStrings;
	private List<UpdateDbSql> sql;
	private String pluginId;
	
	public SqlWithUiRunner(String[] sql, String pluginId){
		sqlStrings = new ArrayList<String>();
		for (int i = 0; i < sql.length; i++) {
			sqlStrings.add(sql[i]);
		}
		this.pluginId = pluginId;
	}
	
	public boolean runSql(){
		sql = new ArrayList<UpdateDbSql>();
		// create UpdateDbSql objects from input list
		for (String sqlString : sqlStrings) {
			sql.add(new UpdateDbSql(sqlString));
		}
		// run the update with progress in the UI Thread if a Display is available ...
		if (isDisplayAvailable()) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run(){
					Shell parent = null;
					try {
						parent = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					} catch (IllegalStateException e) {
						// the workbench has not been created yet ... create a dummy Shell on the
						// display
						parent = new Shell(Display.getDefault());
					} catch (NullPointerException e) {
						// the workbench has not been created yet ... create a dummy Shell on the
						// display
						parent = new Shell(Display.getDefault());
					}
					ProgressMonitorDialog dialog = new ProgressMonitorDialog(parent);
					try {
						dialog.run(true, false, new IRunnableWithProgress() {
							@Override
							public void run(IProgressMonitor monitor){
								monitor.beginTask("Running DB Script", sql.size());
								for (UpdateDbSql update : sql) {
									monitor.subTask(update.getSql());
									update.run();
									// worked increates the monitor, the values is added to the
									// existing ones
									monitor.worked(1);
								}
								monitor.done();
							}
						});
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		} else {
			// this code should only be reached during tests!
			for (UpdateDbSql update : sql) {
				update.run();
			}
		}
		// determine if all updates were successful
		for (UpdateDbSql update : sql) {
			if (update.getStatus() == SqlStatus.FAIL)
				return false;
		}
		return true;
	}
	
	/**
	 * Class holding information about one sql and its execution
	 * 
	 * @author thomas
	 */
	protected class UpdateDbSql implements Runnable {
		private String sql;
		private SqlStatus status;
		
		protected UpdateDbSql(String sql){
			this.sql = sql;
			status = SqlStatus.NONE;
		}
		
		@Override
		public void run(){
			JdbcLink link = null;
			Stm statement = null;
			try {
				link = PersistentObject.getConnection();
				statement = link.getStatement();
				setStatus(SqlStatus.EXECUTE);
				// do not use execScript method here as it will catch the exceptions
				ByteArrayInputStream scriptStream =
					new ByteArrayInputStream(this.sql.getBytes("UTF-8"));
				String sqlString;
				while ((sqlString = JdbcLink.readStatement(scriptStream)) != null) {
					try {
						statement.exec(link.translateFlavor(sqlString));
					} catch (JdbcLinkException e) {
						setStatus(SqlStatus.FAIL);
						try {
							StatusManager.getManager().handle(
								new ElexisStatus(ElexisStatus.ERROR, pluginId,
									ElexisStatus.CODE_NONE, "Error " + e.getMessage()
										+ " during db update", e));
						} catch (AssertionFailedException appnotinit) {
							log.log(e, "Error " + e.getMessage() + " during db update", Log.ERRORS);
						}
					}
				}
			} catch (UnsupportedEncodingException e) {
				setStatus(SqlStatus.FAIL);
				try {
					StatusManager.getManager().handle(
						new ElexisStatus(ElexisStatus.ERROR, pluginId, ElexisStatus.CODE_NONE,
							"Error " + e.getMessage() + " during db update", e));
				} catch (AssertionFailedException appnotinit) {
					log.log(e, "Error " + e.getMessage() + " during db update", Log.ERRORS);
				}
				return;
			} finally {
				if (link != null && statement != null)
					link.releaseStatement(statement);
			}
			if (getStatus() == SqlStatus.EXECUTE)
				setStatus(SqlStatus.SUCCESS);
		}
		
		public void setStatus(SqlStatus status){
			this.status = status;
		}
		
		public SqlStatus getStatus(){
			return status;
		}
		
		public String getSql(){
			return sql;
		}
	}
	
	protected boolean isDisplayAvailable(){
		try {
			Class.forName("org.eclipse.swt.widgets.Display");
		} catch (ClassNotFoundException e) {
			return false;
		} catch (NoClassDefFoundError e) {
			return false;
		}
		if (Display.getDefault() == null)
			return false;
		else
			return true;
	}
}
