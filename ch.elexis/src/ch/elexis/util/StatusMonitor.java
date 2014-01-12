/*******************************************************************************
 * Copyright (c) 2013 Joerg Sigle www.jsigle.com
 * All rights reserved.
 *
 * Contributors:
 *    J. Sigle   - initial implementation
 * 
 *******************************************************************************/

package ch.elexis.util;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;

import ch.elexis.data.Brief;
import ch.elexis.text.ITextPlugin;
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
		public boolean					isActive = false;			//ToDo: MaybeRemoveThis. It may be easily understandable - but probably is a redundant representation of (docPlugin != null).
		public Brief					docDoc = null;														//Identifier of this entry usable from TextView(etc.) - TextContainer - maybe Brief			
		public ICallback				docSaveHandler = null;												//Callback method implemented in, and using methods available to in TextView(etc.)
		public IStatusMonitorCallback	docShowViewHandler = null;											//Callback method implemented in, and using methods available to in TextView(etc.)
		public boolean					docIsModified = false;												//Updated from the isModified() handler from NOAtext
		public long 					timestampOfLastChangeOfIsModified = 0;								//Updated from the isModified() handler from NOAtext 
		public long 					timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent = 0;	//Updated from the isModified() handler from NOAtext
	
		private MonitoredDocument() {}		
	}
	
	//We might rather implement this as a linked list, but I don't want to try that now.
	//
	//At any given time, one slot may be typically used per document *type*
	//that is concurrently displayed in one and only one view concurrently available for that document type.
	//
	//To my knowledge, all of the respective modules like TextView, RezeptBlatt, AUFZeugnis etc. provide only one view each,
	//so that most often, only 1 - 3 slots will be used; I'm unsure whether 10 concurrently used slots can even be reached.
	//I leave a few extra slots as security margin and future use. 
	//
	//When the first text document is about to be displayed/edited,
	//the external office program is connected to (office).
	//
	//Each specialized document handler class (TextView, RezeptBlatt, AUFZeugnis...) 
	//provides its own view (panel/frame?),
	//for which a new instance of (e.g.) noatext_jsl is instantiated when the user shows the view,
	//or when the first document (actBrief) is created or loaded therein,
	//
	//Each document handler class (TextView, RezeptBlatt, AUFZeugnis...) contains
	//an object text or txt, that includes txt.plugin or text.plugin, which is implemented
	//by an instance of the actually used one (NOAText$x)
	//
	//
	//This is true for textplugins that display text documents in frames; I am unsure how other implementations may behave.
	//
	//As long as we handle only a few documents, we need not keep track of how many are stored
	//nor need we ensure that all used slots are kept at the beginning of the array etc.
	
	private static int maxNumMonitorableDocumentViews = 20;
	public static MonitoredDocument[] monitoredDocuments = new MonitoredDocument[maxNumMonitorableDocumentViews];
	//TODO: Simply supplying a (suitable) Constructor for the class StatusMonitor will NOT ensure
	//that entries for MonitoredDocument[] are allocated before use. Although "the runtime system should call static constructors in time...",
	//apparently it doesn't. So, for now, I supply a specifically named initialization method, a flag, and call it before the array is used.
	
	static private boolean	monitoredDocumentEntriesAllocated = false;
	static private void	monitoredDocumentAllocateEntries() {
		//If the array entries are not allocated yet, then do that.
    	System.out.println("js com.jsigle.noa/StatusMonitor.java StatusMonitor(): allocating entries in monitoredDocuments...");		
		for (int i = 0; i<monitoredDocuments.length; i++) {
			monitoredDocuments[i] = new MonitoredDocument();
		}
		monitoredDocumentEntriesAllocated = true;
	}

	/*
	 * Add and activate an entry for a document to be monitored, if it is not already there, and if there is a free slot.
	 * Also ensure that entries within the array have been allocated first.
	 * If it was the first entry to be activated, also start the actual status monitoring thread.
	 * 
	 * @ param: Brief suppliedDoc = The Brief doc to be monitored. Hopefully, as of 20130625js, NOAText has received a public variable where its clients can store a reference to that as well.
	 * 
	 * @ return: Currently none. Later on, might return some status information - at least three outcomes could happen. 
	 */
	public static void addMonitorEntry(Brief suppliedDoc, ICallback suppliedSaveHandler, IStatusMonitorCallback suppliedShowViewHandler) {
    	System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() begin");   	

    	if (suppliedDoc == null) { 
			System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() suppliedDoc == null) -> nop; early return");
			return; 
		}
    	System.out.println("js com.jsigle.noa/StatusMonitor.java supplied suppliedDoc == " + suppliedDoc.toString());		

    	//Ensure that array entries are allocated before use
    	if ( !monitoredDocumentEntriesAllocated ) { monitoredDocumentAllocateEntries(); }
    	
		//Just some debugging output:
		if ( monitoredDocuments[0] == null) { System.out.println("xyz: WARNING: monitoredDocuments[0] == null!"); return;}
		System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() DEBUG: monitoredDocuments[0].isActive == " + monitoredDocuments[0].isActive);
		if (monitoredDocuments[0].docDoc == null)	{ System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() DEBUG: monitoredDocuments[0].docDoc == null"); }
		else										{ System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() DEBUG: monitoredDocuments[0].docDoc == " + monitoredDocuments[0].docDoc.toString()); }
		
		//If an entry already exists for the same Plugin, then return
		//This entry needs NOT be active yet, because it might have been prepared and not activated.
		//If we would remove an entry, we would usually sett all its fields to null, to differentiate it from a prepared, but yet inactive one.
		for (MonitoredDocument entry : monitoredDocuments) {
			if (entry.docDoc == suppliedDoc) { 
				System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() An entry for the same suppliedDoc already exists -> early return");
				return; 
			}
		}

		//If a free (i.e.: NOT yet prepared && NOT yet activated) slot exists, then add the new Plugin to that slot and initialize all entries
		for (MonitoredDocument entry : monitoredDocuments) {
			if ( (!entry.isActive) ) {
				System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() About to add the new entry in slot x of the monitoring list...");
				
				entry.docDoc=suppliedDoc;				//I want another pointer to the existing doc; new space needs not be allocated.
				entry.docSaveHandler=suppliedSaveHandler;
				entry.docShowViewHandler=suppliedShowViewHandler;
				entry.timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent = System.currentTimeMillis();
				entry.timestampOfLastChangeOfIsModified = entry.timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent;
				entry.docIsModified = false;
				entry.isActive = true;
				
				//System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() suppliedDoc == " + suppliedDoc.toString());
				System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() entry.docDoc == " + entry.docDoc.toString());
				System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() entry.isActive == " + entry.isActive);

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
		//If a free slot exists, then add the new Plugin to that slot and initialize all entries
		//Due to a problem I observed, I thought we could not use a
		//  for (entry : monitoredDocuments)
		//loop, because that might apparently supply copies of the original entries - 
		//and modifications to these would not persist.
		//But alas - the observed problem had another reason.
		//for (entry : monitoredDocuments) would really use pass by reference, and we can actually use it.
		//Nevertheless, the temporarily used alternative loop and its accesses are kept here:  
		for (int i = 0; i<monitoredDocuments.length; i++) {
			if (!monitoredDocuments[i].isActive) {
				System.out.println("js com.jsigle.noa/StatusMonitor.java addMonitorEntry() About to add the new entry in slot "+i+" of the monitoring list...");
				monitoredDocuments[i].docPlugin=new String(suppliedDoc);
				...
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
	 * If this was the last active entry, then activate the actual monitoring thread.
	 * 
	 * @ param: ITextPlugin suppliedDoc = The Plugin of the document to be monitored. Hopefully, both TextView.java and NOAText.java can obtain this.
	 * 
	 * @ return: Currently none. Later on, might return some status information - at least three outcomes could happen. 
	 */
	public static void removeMonitorEntry(Brief suppliedDoc) {
    	System.out.println("js com.jsigle.noa/StatusMonitor.java removeMonitorEntry() begin");
    	
		if (suppliedDoc == null) { 
			System.out.println("js com.jsigle.noa/StatusMonitor.java removeMonitorEntry() WARNING: suppliedDoc == null) -> nop; early return");
			return; 
		}
    	System.out.println("js com.jsigle.noa/StatusMonitor.java supplied suppliedDoc == " + suppliedDoc.toString());

    	//Ensure that array entries are allocated before use - if not, we won't find anything to remove.
    	if ( !monitoredDocumentEntriesAllocated ) {
			System.out.println("js com.jsigle.noa/StatusMonitor.java removeMonitorEntry() WARNING: array entries have not been initialized yet. -> nop; early return.");
    		return;
    	}
		
		//If an entry already exists for the same Plugin, then deactivate it and reset its data fields to null.
		//Please note: This loop does not break afterwards, because it also counts all remaining active entries.
		//As a side effect, it would disable multiple entries referencing the same entry.docDoc (which should never happen to exist).
		//This simple implementation is well suited for small arrays.
		int numActiveEntries = 0;
		for (MonitoredDocument entry : monitoredDocuments) {
			if (entry.docDoc == suppliedDoc) { 
				System.out.println("js com.jsigle.noa/StatusMonitor.java removeMonitorEntry(): removing entry");
				entry.isActive = false;
				//The remainder would not be needed so urgently, but better keep our memory clean, than leak information...
				entry.docDoc = null;
				entry.docSaveHandler = null;		//The static array will stay in existence, so nulling content		
				entry.docShowViewHandler = null;	//might allow objects containing callbacks to be disposed of.
				entry.docIsModified = false;											//Updated from the isModified() handler from NOAtext
				entry.timestampOfLastChangeOfIsModified = 0;							//Updated from the isModified() handler from NOAtext 
				entry.timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent = 0;	//Updated from the isModified() handler from NOAtext
			}

			if (entry.isActive) {
				numActiveEntries = numActiveEntries + 1;
			}
		}	
		
		//If no active entries remain, stop the monitoring thread.
		if (numActiveEntries == 0) {
			System.out.println("js com.jsigle.noa/StatusMonitor.java removeMonitorEntry() No active entries remain. Stopping the monitoring thread.");
			statusMonitorThread.interrupt();
			statusMonitorThread = null;			
		}

		System.out.println("js com.jsigle.noa/StatusMonitor.java removeMonitorEntry() end");
	}
	
	/*
	 * Update an entry for a supplied document with the supplied isModified() status, and current timestamps.
	 * This is usually called from the NOAText isModified() handler.
	 * This caller does NOT know entry.docDoc (i.e. the actBrief which it works for), but (hopefully) the filename of the document??? Or maybe not even that... :-)  
	 * 
	 * @ param: ITextPlugin suppliedDoc = The Plugin of the document to be updated. Hopefully, both TextView.java and NOAText.java can obtain this.
	 * 
	 * @ return: Currently none. Later on, might return some status information - at least three outcomes could happen. 
	 */
	public static void updateMonitorEntry(Brief suppliedDoc, boolean suppliedIsModified) {
    	System.out.println("js com.jsigle.noa/StatusMonitor.java updateMonitorEntry() begin");
    	
		if (suppliedDoc == null) { 
			System.out.println("js com.jsigle.noa/StatusMonitor.java updateMonitorEntry() StringToo.isNothing(suppliedDoc) -> nop; early return");
			return; 
		}
    	System.out.println("js com.jsigle.noa/StatusMonitor.java supplied suppliedDoc == " + suppliedDoc.toString());
		
		//If an active entry already exists for the same Brief suppliedDoc, then update it.
		for (MonitoredDocument entry : monitoredDocuments) {
			if ( (entry.isActive) && (entry.docDoc == suppliedDoc) ) {
				System.out.println("js com.jsigle.noa/StatusMonitor.java updateMonitorEntry() Matching entry found; updating...");
				entry.timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent = System.currentTimeMillis();
				if (entry.docIsModified != suppliedIsModified) {
					entry.docIsModified = suppliedIsModified;
					entry.timestampOfLastChangeOfIsModified = entry.timestampOfLastDocumentModifyListenerReactOnUnspecifiedEvent;
				}
				System.out.println("js com.jsigle.noa/StatusMonitor.java updateMonitorEntry() ...and about to return");
				return; 
			}
		}
		
		System.out.println("js com.jsigle.noa/StatusMonitor.java updateMonitorEntry() WARNING: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");		
		System.out.println("js com.jsigle.noa/StatusMonitor.java updateMonitorEntry() WARNING: A matching entry was not found.");		   
		System.out.println("js com.jsigle.noa/StatusMonitor.java updateMonitorEntry() WARNING: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");		
	}

	/*
	 * The actual statusMonitorThread.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public static long statusMonitoringIntervall = 1000; 
	public void run() {
    	System.out.println("js com.jsigle.noa/StatusMonitor.java run() [The actual StatusMonitor working thread] begin");
    	    	
		while (true) {
			try {
				System.out.println("js com.jsigle.noa/StatusMonitor.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - about to do NOAText statusMonitoring work...");
				
				//Process all monitoredDocuments...
				int i=0;
				for (MonitoredDocument entry : monitoredDocuments) {
					if (entry.isActive) {
						System.out.println("js com.jsigle.noa/StatusMonitor.java - Processing slot "+i+" with entry.docDoc == " + entry.docDoc.toString());
						System.out.println("js com.jsigle.noa/StatusMonitor.java - = entry.docDoc.getLabel() == " + entry.docDoc.getLabel());
						
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
							
							if (entry.docIsModified && (timeSinceLastDocumentModifyListenerReactOnUnspecifiedEvent < statusMonitoringIntervall) ) {
								entry.docShowViewHandler.showView();
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
	
									entry.docSaveHandler.save();
									
									//Wieso geht hier save() nicht, im Gegensatz zu RezeptBlatt.java? Siehe die unterschiedliche Einbettung in Klassen etc.
									//save();
									//actBrief.save(txt.getPlugin().storeToByteArray(), txt.getPlugin().getMimeType());
								}				
							}
							
							//Put the running thread to sleep for a while - after each checked entry (see Thread.sleep() below for alternative behaviour)
							//If you completely remove the Thread.sleep() here, you also need to change the try/catch block, so better just use a short sleep period.
							Thread.sleep(0);
	
						} catch (InterruptedException irEx) {
							//if the thread is interrupted, then return from it; i.e. stop running it.
					    	System.out.println("js com.jsigle.noa/StatusMonitor.java run() [The actual StatusMonitor working thread] interrupted, about to return.");
							return;
						}
	
					} // if entry.isActive
					i = i + 1;
				} // for entry
				
				//Put the running thread to sleep for a while - after each completely checked list of entries (see Thread.sleep() above for alternative behaviour)
				//If you completely remove the Thread.sleep() here, you also need to change the try/catch block, so better just use a short sleep period.
				//If you chose a long delay, then don't forget to raise the threshold for the showView() above as well, above! 
				Thread.sleep(statusMonitoringIntervall);

			} catch (InterruptedException irEx) {
				//if the thread is interrupted, then return from it; i.e. stop running it.
		    	System.out.println("js com.jsigle.noa/StatusMonitor.java run() [The actual StatusMonitor working thread] interrupted, about to return.");
				return;
			}
			
		} // while true
	} // public void run() 
}
