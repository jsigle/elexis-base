package ch.elexis.util;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;

import ch.elexis.text.ITextPlugin.ICallback;
import ch.elexis.views.TextView;
import ch.rgw.tools.StringTool;
import ch.elexis.util.IStatusMonitorCallback;

//It would be nice to monitor user input - and save preferrably, 
//when a pause of e.g. 10 secs after the last input has occured,
//or - if no pause of that size has occured - e.g. after a maximum delay of 5 min after the first unsaved input.
//So that users can usually type without interruption/delay by automatic saving,
//and still the maximum amount of unsaved work/time is limited.
//That would effectively move a regularly timed auto save action closer to the last user input, when the user pauses. 
//This optimal? behaviour is, however,
//at least approached to a usable state by calling save after a fixed time and only really performing a save if isModified(). 
//TODO: Warum heisst der TextContainer in RezeptBlatt.java text; und in TextView.java textContainer???


public class StatusMonitor implements Runnable {
	/*
	 * 201306171151js
	 * A status monitoring method that I want to call regularly.
	 * It shall:
	 * (a) check whether the TextView view has focus, but is not enabled, and should thus be enabled.
	 * This needs to be done from outside, as merely accessing the needed structures below wb on a sufficiently
	 * low level will cause the ModicifacionListener already added to NOAText to stall the NOAText plugin.
	 * (b) Regularly call the storeToByteArray() method - which can access the doc.isModified(), and which I have
	 * modified to skip the actual storing when isModified() is false.
	 * It would be better if this storing was triggered a short time after the last keystroke,
	 * rather than in a regular interval. But at the moment, I can't see how isModified() could get the information
	 * up here, or how we could look down for that in another way (apart from solutions requiring major construction
	 * work, which I may attempt later on).   
	 */
	//I assume that ONE instance of TextView handles ONE document at time,
	//so it may contain ONE statusMonitorThread monitoring exactly this document.
	//Sadly, I need to define statusMonitorThread on this level - furhter down, like in NOAText or in TextContainer will probably NOT do.
	//Because the storeToByteArray() is called from here, and its result forwarded to Brief.save() from here... -
	//no module further down the road can see both the source and target of that operation,
	//so none can trigger an automatic save action.
	//TODO: This also means that I need to copy that functionality - if I want to have it there -
	//in (probably) RezepteBlatt.java, ... and others from ch.elexis.views.
	//For now: in TextView.java, RezepteBlatt.java
	public static Thread statusMonitorThread = null;
	
	static class MonitoredDocument {
		public boolean					isValid = false;
		public String					docURL = null;
		public ICallback				docSaveHandler = null;
		public IStatusMonitorCallback	docShowViewHandler = null;
		public boolean					docIsModified = false;
		public long 					timestampOfLastChangeOfIsModified = 0;
		public long 					timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent = 0;
		
		public MonitoredDocument() {
		}
	}
	
	//We might rather implement this as a linked list, but I don't want to try that now.
	//And 10 documents at the same time should suffice.
	//As long as we handle only a few documents, we need not keep track of how many are stored
	//nor need we ensure that all used slots are kept at the beginning of the array.
	
	public static MonitoredDocument[] monitoredDocuments = new MonitoredDocument[10];

	/*
	 * Add and initialize an entry for a document to be monitored, if it is not already there, and if there is a free slot.
	 * 
	 * @ param: String newURL = The URL of the document to be monitored. Hopefully, both TextView.java and NOAText.java can obtain this.
	 * 
	 * @ return: Currently none. Later on, might return some status information - at least three outcomes could happen. 
	 */
	public static void addMonitorEntry(String newURL,  ICallback newSaveHandler, IStatusMonitorCallback newShowViewHandler) {
    	System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() begin");
    	System.out.println("js com.jsigle.noa/StatusMonitor.java supplied newURL == " + newURL);
    	
		if (StringTool.isNothing(newURL)) { 
			System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() StringToo.isNothing(newURL) -> nop; early return");
			return; 
		}
		
		
		//If the array is not initialized, then do that.
		if (monitoredDocuments[0] == null) {
			for (int i = 0; i<monitoredDocuments.length; i++) {
				monitoredDocuments[i] = new MonitoredDocument();
			}
		}
		
		//Just some debugging output:
		if ( monitoredDocuments[0] == null) { System.out.println("xyz: WARNING: monitoredDocuments[0] == null!"); return;}
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() DEBUG: monitoredDocuments[0].isValid == " + monitoredDocuments[0].isValid);
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() DEBUG: monitoredDocuments[0].docURL == " + monitoredDocuments[0].docURL);
		
		//If a valid entry already exists for the same URL, then return
		for (MonitoredDocument entry : monitoredDocuments) {
			if (entry.isValid) {
				if (entry.docURL.equals(newURL)) { 
					System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() An entry for the same newURL already exists -> early return");
					return; 
				}
			}
		}

		//If a free slot exists, then add the new URL to that slot and initialize all entries
		for (MonitoredDocument entry : monitoredDocuments) {
			if (!entry.isValid) {
				System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() About to add the new entry in slot x of the monitoring list...");
				
				entry.docURL=new String(newURL);
				entry.docSaveHandler=newSaveHandler;
				entry.docShowViewHandler=newShowViewHandler;
				entry.timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent = System.currentTimeMillis();
				entry.timestampOfLastChangeOfIsModified = entry.timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent;
				entry.docIsModified = false;
				entry.isValid = true;
				
				System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() newURL == " + newURL);
				System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() entry.docURL == " + entry.docURL);
				
				//If the statusMonitorThread is not yet running, then start it!
				if (statusMonitorThread == null) {
					System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() About to create and start the monitoring thread...");
					statusMonitorThread = new Thread(new StatusMonitor()); 
					statusMonitorThread.start();
				} else {
					System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() The status monitor thread is already running.");
				}
				
				System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() About to return normally.");
				return;
			}
		}

		/*
		//If a free slot exists, then add the new URL to that slot and initialize all entries
		//We cannot use the for (entry : monitoredDocuments) loop here,
		//because that will apparently supply copies of the original entries - 
		//modifications to these would not persist.
		//UPDATE: DOCH, DAS GEHT DURCHAUS. ES IST OFFENBAR PASS EIN BY ADDRESS.  
		for (int i = 0; i<monitoredDocuments.length; i++) {
			if (!monitoredDocuments[i].isValid) {
				System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() About to add the new entry in slot "+i+" of the monitoring list...");
				monitoredDocuments[i].docURL=new String(newURL);
				System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() newURL == " + newURL);
				System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() monitoredDocuments["+i+"].docURL == " + monitoredDocuments[i].docURL);
				monitoredDocuments[i].timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent = System.currentTimeMillis();
				monitoredDocuments[i].timestampOfLastChangeOfIsModified = monitoredDocuments[i].timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent;
				monitoredDocuments[i].docIsModified = false;
				monitoredDocuments[i].isValid = true;
				
				//If the statusMonitorThread is not yet running, then start it!
				if (statusMonitorThread == null) {
					System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() About to create and start the monitoring thread...");
					statusMonitorThread = new Thread(new StatusMonitor()); 
					statusMonitorThread.start();
				} else {
					System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() The status monitor thread is already running.");
				}
				
				System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() About to return normally.");
				return;
			}
		}
		*/

		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");		
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: No monitoring slot was available any more. -> return");		   
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: ToDo: Please increase the number of slots in StatusMonitor.java!");		
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: Note: Disabling the win seems to be caught reliably.");		
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: So we could add another status variable re-set by the TextView.activation(false)/(true) methods, to avoid multiple unnecessary calls to this routine here.");		
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: It is visible after all; the cursor will briefly change from Textcursor to Generic Mouse Pointer.");		
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: Or is there any method to ask for the currently highlighted view?");		
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");		
	}

	/*
	 * Remove an entry for a document to be monitored, if it is there.
	 * If the last entry is removed, then stop the monitoring thread.
	 * 
	 * @ param: String newURL = The URL of the document to be monitored. Hopefully, both TextView.java and NOAText.java can obtain this.
	 * 
	 * @ return: Currently none. Later on, might return some status information - at least three outcomes could happen. 
	 */
	public static void removeMonitorEntry(String newURL) {
    	System.out.println("js com.jsigle.noa/StatusMonitor.java removeMonitorEntry() begin");
    	System.out.println("js com.jsigle.noa/StatusMonitor.java supplied newURL == " + newURL);
    	
		if (StringTool.isNothing(newURL)) { 
			System.out.println("js com.jsigle.noa/StatusMonitor.java removeMonitorEntry() StringToo.isNothing(newURL) -> nop; early return");
			return; 
		}
		
		//If a valid entry already exists for the same URL, then remove it.
		//Also, count all (remaining) valid entries.
		int numValidEntries = 0;
		//We cannot use the for (entry : monitoredDocuments) loop here,
		//because that will apparently supply copies of the original entries - 
		//modifications to these would not persist.  
		for (MonitoredDocument entry : monitoredDocuments) {
			if (entry.isValid) {
					numValidEntries = numValidEntries + 1; 
					if (entry.docURL.equals(newURL)) { 
						System.out.println("js com.jsigle.noa/StatusMonitor.java removeMonitorEntry(): removing entry");
						entry.isValid = false;
						entry.docURL = null; 
						entry.docSaveHandler = null;		//The static array will stay in existence, so nulling content		
						entry.docShowViewHandler = null;	//might allow objects containing callbacks to be disposed of.
						numValidEntries = numValidEntries - 1; 
					}
			}
		}	
		
		//If no valid entries remain, stop the monitoring thread.
		if (numValidEntries == 0) {
			System.out.println("js com.jsigle.noa/StatusMonitor.java removeMonitorEntry() No valid entries remain. Stopping the monitoring thread.");
			statusMonitorThread.interrupt();
			statusMonitorThread = null;			
		}
	}
	
	/*
	 * Update an entry for a supplied document with the supplied isModified() status, and current timestamps.
	 * 
	 * @ param: String newURL = The URL of the document to be updated. Hopefully, both TextView.java and NOAText.java can obtain this.
	 * 
	 * @ return: Currently none. Later on, might return some status information - at least three outcomes could happen. 
	 */
	public static void updateMonitorEntry(String newURL, boolean newIsModified) {
    	System.out.println("js com.jsigle.noa/StatusMonitor.java updateMonitorEntry() begin");
    	System.out.println("js com.jsigle.noa/StatusMonitor.java supplied newURL == " + newURL);
    	
		if (StringTool.isNothing(newURL)) { 
			System.out.println("js com.jsigle.noa/StatusMonitor.java updateMonitorEntry() StringToo.isNothing(newURL) -> nop; early return");
			return; 
		}
		
		//If a valid entry already exists for the same URL, then update it.
		//We cannot use the for (entry : monitoredDocuments) loop here,
		//because that will apparently supply copies of the original entries - 
		//modifications to these would not persist.  
		for (MonitoredDocument entry : monitoredDocuments) {
			if (entry.isValid) { 
				if (entry.docURL.equals(newURL) ) { 
					System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() Matching entry found; updating...");
					entry.timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent = System.currentTimeMillis();
					if ( entry.docIsModified != newIsModified ) {
						entry.docIsModified = newIsModified;
						entry.timestampOfLastChangeOfIsModified = entry.timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent;
					}
					return; 
				}
			}
		}
		
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");		
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: A matching entry was not found -> return");		   
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: ToDo: Please add entries from other sources, RezeptBlatt.java etc.");		
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: ToDo: Use the actual Document URL instead of just TextView, if desired,");		
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: ToDo: or change the (otherwise misleading) variable names in StatusMonitor.java");		
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() WARNING: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");		
	}

	/*
	 * The actual statusMonitorThread.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
    	System.out.println("js com.jsigle.noa/StatusMonitor.java run() begin");
    	    	
		while (true) {
			System.out.println("js com.jsigle.noa/StatusMonitor.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - about to do NOAText statusMonitoring work...");
			
			//Process all monitoredDocuments...
			int i=0;
			for (MonitoredDocument entry : monitoredDocuments) {
				if (entry.isValid) {
					System.out.println("js com.jsigle.noa/StatusMonitor.java - Processing slot "+i+" with docURL == " + entry.docURL);
					
					//entry.docURL = newURL;
					//entry.timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent = System.currentTimeMillis();
					//entry.timestampOfLastChangeOfIsModified = entry.timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent;
					//entry.docIsModified = false;
					//entry.isValid = true;

					long timeSinceLastIsModifiedChange = System.currentTimeMillis() - entry.timestampOfLastChangeOfIsModified;
					long timeSinceLastDocumentModifyListenerReactOnUnspecifiedEvent = System.currentTimeMillis() - entry.timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent;;
					
					System.out.println("js com.jsigle.noa/StatusMonitor.java - statusMonitorisModified / statusMonitorLastIsModifiedChange / statusMonitorLastDocumentModifyListenerReactOnUnspecifiedEvent");
					System.out.println("js com.jsigle.noa/StatusMonitor.java - "
						+ entry.docIsModified 
						+ " / " + entry.timestampOfLastChangeOfIsModified + " (" + timeSinceLastIsModifiedChange/1000 + " secs ago)" 
					    + " / " + entry.timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent + " (" + timeSinceLastDocumentModifyListenerReactOnUnspecifiedEvent/1000 + " secs ago)");					
							
					try {					
						
						//--------------------------------------------------------------------------------
						//If the document has just been modified, then activate the respective view.
						//As de-activation a text-plugin-served Window traditionally triggers saving in Elexis,
						//but activation of text-plugin-served Windows does not work if a user clicks directly into the text,
						//this helps to ensure that user edited content is finally saved. E.g. when they load another document
						//before the new regular time-intervall/isModified-based saving routine would kick in.
						//--------------------------------------------------------------------------------
						
						if (entry.docIsModified && (timeSinceLastDocumentModifyListenerReactOnUnspecifiedEvent < 1000) ) {
							if (entry.docURL.equals("TextView")) {
								entry.docShowViewHandler.showView();
							}
						} // if entry.isModified, <2 secs ago 
						
						//--------------------------------------
						//Ensure the monitored document is saved,
						//either at regular maximum intervals - even if the user is still modifying along,
						//or, after the user has stopped typing/modifying for some time,
						//    the longer the time since the last saving; the shorter need that pause after the last modification be.
						//--------------------------------------
						

						//if the document has been modified
						if (entry.docIsModified) {											
							//save unconditionally after 300 seconds
							if (     (timeSinceLastIsModifiedChange >= 300000)
							//or conditionally: if the last unsaved modification is >= 20 sec old, and the user has stopped typing for 15 seconds
								|| ( (timeSinceLastIsModifiedChange >=  20000) && (timeSinceLastDocumentModifyListenerReactOnUnspecifiedEvent >= 15000) )
							//or conditionally: if the last unsaved modification is >= 2 minute old, and the user has stopped typing for 10 seconds
								|| ( (timeSinceLastIsModifiedChange >= 120000) && (timeSinceLastDocumentModifyListenerReactOnUnspecifiedEvent >= 10000) )
							//or conditionally: if the last unsaved modification is >= 3 minutes old, and the user has stopped typing for 5 seconds
								|| ( (timeSinceLastIsModifiedChange >= 180000) && (timeSinceLastDocumentModifyListenerReactOnUnspecifiedEvent >=  5000) )
							//or conditionally: if the last unsaved modification is >= 4 minutes old, and the user has stopped typing for 3 seconds
								|| ( (timeSinceLastIsModifiedChange >= 240000) && (timeSinceLastDocumentModifyListenerReactOnUnspecifiedEvent >=  3000) )
								) {
								System.out.println("js com.jsigle.noa/StatusMonitor.java - run() - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");								
								System.out.println("js com.jsigle.noa/StatusMonitor.java - run() - WE SHOULD TRY TO SAVE THIS DOCUMENT NOW!");								
								System.out.println("js com.jsigle.noa/StatusMonitor.java - run() - PLEASE IMPLEMENT THIS!");								
								System.out.println("js com.jsigle.noa/StatusMonitor.java - run() - Done: TextView.java");								
								System.out.println("js com.jsigle.noa/StatusMonitor.java - run() - ToDo: RezeptBlatt.java et al.");								
								System.out.println("js com.jsigle.noa/StatusMonitor.java - run() - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");								

								if (entry.docURL.equals("TextView")) {
									entry.docSaveHandler.save();
								}
								
								//Wieso geht hier save() nicht, im Gegensatz zu RezeptBlatt.java? Siehe die unterschiedliche Einbettung in Klassen etc.
								//save();
								//actBrief.save(txt.getPlugin().storeToByteArray(), txt.getPlugin().getMimeType());
							}				
						}
						
						//Put the running thread to sleep for a while.
						Thread.sleep(1000);
					} catch (InterruptedException irEx) {
						//if the thread is interrupted, then return from it; i.e. stop running it.
						return;
					}

				} // if entry.isValid
				i = i + 1;
			} // for entry
		} // while true
	} // public void run() 
}
