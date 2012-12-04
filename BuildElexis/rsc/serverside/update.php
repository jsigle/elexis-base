<?php
/*
 * Created on 14-Dec-2006
 * @author Matthew Wilson
 * $Id: update.php 1573 2007-01-07 21:45:04Z rgw_ch $
 */

 // Directory in which files are stored.
 $dir = 'elexis/update';

 if ($_POST['method'] == 'check') {
 	// Find newest revision matching supplied basename
 	if (preg_match('/([a-zA-Z][a-zA-Z0-9-_\.]*)_([0-9]+)\.([0-9]+)\.([0-9]+)\.?([a-zA-Z0-9]*)\.[a-zA-Z]+/', $_POST['filename'], $matches ) > 0) {
 	  $provided = array($matches[2], $matches[3], $matches[4], $matches[5]);
 	  $file = findNewest($matches[1]);
 	} else {
 		print 'does not match';
 		$file = NULL;
 	}
 	// Compare newest version to supplied

 	if ($file == NULL) {
 	// File does not exist
 		print '0';
 	} else if ($file == $provided) {
 	// Server version is same as provided
 		print '2';
 	} else if ($file[0] > $provided[0]) {
 	// Newer major version
 	    print '6';
 	} else if ($file[0] == $provided[0] && $file[1] > $provided[1]) {
 	// Newer minor version
 	    print '5';
 	} else if ($file[0] == $provided[0] && $file[1] == $provided[1] && $file[2] > $provided[2]) {
 	// Newer revision
 		print '4';
 	} else if ($file[0] == $provided[0] && $file[1] == $provided[1] && $file[2] == $provided[2] && $file[3] > $provided[3]) {
 	// Newer build
 		print '3';
 	} else {
 	// Server version older
 		print '1';
 	}
 } else if ($_POST['method'] == 'download') {
 	preg_match('/([a-zA-Z][a-zA-Z0-9-_\.]*)_([0-9]+)\.([0-9]+)\.([0-9]+)\.?([a-zA-Z0-9]*)\.([a-zA-Z]+)/', $_POST['filename'], $matches );
 	// Work out which file to download
 	$file = findNewest($matches[1]);
 	if ($file != NULL) {
 		// Construct path
 		$filename = $matches[1] . '_' . $file[0] . '.' . $file[1] . '.' . $file[2];
 		if ($file[3] != NULL) {
 			$filename .= '.' . $file[3];
 		}
 		$filename .= '.' . $matches[6];

 		$path = $dir . '/' . $filename;
 		if (file_exists($path)) {
 			print '0' . $filename . "|";
 			ob_start();
 			$bytes = @readfile($path);
 			@ob_flush();
 		return;
 		}

 	}
 	// If we've got here something's gone wrong - either the file does not exist
 	// or cannot be read -  send error code.
 	print 1;


 }

 function findNewest($filename) {
 global $dir;

 // Get all files matching basename
 $files = glob($dir . '/' . $filename . '*');
 if (sizeof($files) == 0 || $files == FALSE) {
 	return NULL;
 }
 // Create array of versions split into each part of the version number
 foreach ($files as $version) {
 	preg_match('/([0-9]+)\.([0-9]+)\.([0-9]+)\.?([a-zA-Z0-9]*)\.[a-zA-Z]+/', $version, $matches );
 	$versions[] = array($matches[1], $matches[2], $matches[3], $matches[4]);
 }

// Find newest version
 $newest = $versions[0];
 foreach ($versions as $version) {
 	if (
 	      ($version[0] > $newest[0])
 	   || ($version[0] == $newest[0] && $version[1] > $newest[1])
 	   || ($version[0] == $newest[0] && $version[1] == $newest[1] && $version[2] > $newest[2])
 	   || ($version[0] == $newest[0] && $version[1] == $newest[1] && $version[2] == $newest[2] && $version[3] > $newest[3])
 	   )  {
 	   	 $newest = $version;
 	   }
 }
 	return $newest;
 }
?>