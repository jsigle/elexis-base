package ch.elexis.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import ch.elexis.Hub;
import ch.elexis.ResourceManager;
import ch.elexis.data.PersistentObject;
import ch.elexis.preferences.PreferenceInitializer;
import ch.rgw.io.InMemorySettings;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.VersionInfo;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
	Hub.class, PreferenceInitializer.class
})
public class Test_DBUpdate {
	
	private JdbcLink link;
	
	@BeforeClass
	public static void oneTimeSetUp(){
		Hub.localCfg = new InMemorySettings();
	}
	
	@Before
	public void setUp(){
		ResourceManager rsc = ResourceManager.getInstance();
		String pluginPath = rsc.getResourceLocationByName("/old/dummy.txt");
		int end = pluginPath.lastIndexOf('/');
		end = pluginPath.lastIndexOf('/', end - 1);
		pluginPath = pluginPath.substring(0, end);
		
		PowerMockito.mockStatic(Hub.class);
		PowerMockito.when(Hub.getBasePath()).thenReturn(pluginPath);
		PowerMockito.when(Hub.getCfgVariant()).thenReturn("default");
		PowerMockito.when(Hub.getActiveShell()).thenReturn(null);
		
		PowerMockito.mockStatic(PreferenceInitializer.class);
		PowerMockito.when(PreferenceInitializer.getDefaultDBPath()).thenReturn(pluginPath);
		// currently only test with in mem h2 db
		// TODO extend test environment for other db systems
		link = JdbcLink.createH2Link("mem:test_mem");
		
		assertNotNull(link);
		link.connect("", "");
		PersistentObject.connect(link);
	}
	
	@After
	public void tearDown(){
		link.exec("DROP ALL OBJECTS");
		link.disconnect();
	}
	
	@Test
	public void testDoUpdate(){
		String olddbv = Hub.globalCfg.get("dbversion", null);
		VersionInfo oldvi = new VersionInfo(olddbv);
		DBUpdate.doUpdate();
		String newdbv = Hub.globalCfg.get("dbversion", null);
		VersionInfo newvi = new VersionInfo(newdbv);
		assertFalse(oldvi.isEqual(newvi));
		assertTrue(oldvi.isOlder(newvi));
		assertTrue(newvi.isNewer(oldvi));
	}
}
