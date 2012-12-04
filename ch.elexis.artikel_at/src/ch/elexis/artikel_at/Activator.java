package ch.elexis.artikel_at;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import ch.elexis.Hub;
import ch.elexis.artikel_at.data.Artikel_AT_Cache;
import ch.rgw.io.Settings;

public class Activator extends AbstractUIPlugin {
	
	public Activator(){}
	
	/*
	 * This activators sole purpose is to initialize the HashMap caches for the VidalLabelProvider
	 * 
	 * @author Marco Descher
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception{
		Settings globalCfg = Hub.globalCfg;
		if (globalCfg != null) {
			if (Hub.globalCfg.get(PreferenceConstants.ARTIKEL_AT_CACHEUPDATE_TIME, "invalid")
				.equalsIgnoreCase("invalid")) {
				Artikel_AT_Cache.updateCache();
			}
		}
	}
	
	@Override
	public void stop(BundleContext context) throws Exception{}
}
