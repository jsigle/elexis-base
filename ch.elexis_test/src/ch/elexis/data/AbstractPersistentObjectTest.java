package ch.elexis.data;

import static org.junit.Assert.assertNotNull;

import org.eclipse.ui.statushandlers.StatusManager;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import ch.elexis.Hub;
import ch.elexis.ResourceManager;
import ch.elexis.preferences.PreferenceInitializer;
import ch.rgw.io.InMemorySettings;
import ch.rgw.tools.JdbcLink;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
	Hub.class, PreferenceInitializer.class, JdbcLink.class, StatusManager.class
})
public abstract class AbstractPersistentObjectTest {
	
	@BeforeClass
	public static void oneTimeSetUp(){
		Hub.localCfg = new InMemorySettings();
	}
	
	// create a JdbcLink with an initialized db for elexis
	// the creation script is taken from the rsc directory
	// of the host plugin when running a Plugin-Test
	protected JdbcLink initDB(){
		ResourceManager rsc = ResourceManager.getInstance();
		String pluginPath = rsc.getResourceLocationByName("/createDB.script");
		int end = pluginPath.lastIndexOf('/');
		end = pluginPath.lastIndexOf('/', end - 1);
		pluginPath = pluginPath.substring(0, end);
		
		PowerMockito.mockStatic(Hub.class);
		PowerMockito.when(Hub.getBasePath()).thenReturn(pluginPath);
		PowerMockito.when(Hub.getCfgVariant()).thenReturn("default");
		
		PowerMockito.mockStatic(PreferenceInitializer.class);
		PowerMockito.when(PreferenceInitializer.getDefaultDBPath()).thenReturn(pluginPath);
		
		JdbcLink link = new JdbcLink("org.h2.Driver", "jdbc:h2:mem:test_mem", "hsql");
		assertNotNull(link);
		link.connect("", "");
		PersistentObject.connect(link);
		return link;
	}
}
