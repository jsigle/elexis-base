<?php 

// $Id: download.php 1555 2007-01-07 13:42:41Z rgw_ch $

//SETTINGS
	
$directory='elexis'; //enter the path without the last /
$counter='counter.txt';	//path to where the counter is
$error_message='The file you specified does not exist !'; 	//error message if file does not exist

//END SETTINGS



// Is the OS Windows or Mac or Linux 
if (strtoupper(substr(PHP_OS,0,3)=='WIN'))
	$eol="\r\n"; 	
else
	if (strtoupper(substr(PHP_OS,0,3)=='MAC'))
		$eol="\r"; 
	else 
		$eol="\n"; 

@$file=$_GET["file"];
$aux=explode(".",$file,2);
$fileName=$aux[0];
@$fileExt=strtolower($aux[1]);

$dh  = opendir($directory);
while (false !== ($filename = readdir($dh)))
   $files[] = $filename;
rsort($files);

$size=sizeof($files)-2;
$download='';
$latest=0;


for ($i=0;$i<$size;$i++)
{
	$path=$directory.'/'.$files[$i];
	if (is_file($path))
	{
		@$pos = strpos($files[$i],$fileName);
		if ($pos!==false && !$pos)
			if (filemtime($path)>$latest)
			{
				if ($fileExt)
				{
					$aux=explode(".",$files[$i],2);
					if (@strtolower($aux[1])==$fileExt)
					{
						$download=$path;
						$latest=filemtime($path);
					}
				}
				else 
				{
					$download=$path;
					$latest=filemtime($path);
				}
			}
	}
}

if ($download)
{
	$towrite=array();
	@$fp=fopen($counter,'r');
	$ok=0;
	while ($fp && !feof($fp))
	{
		$line=fscanf($fp,"%s".$eol);
		$line=$line[0];
		if ($line)
		{
			$aux=explode("=",$line);
			if (strtolower($aux[0])==strtolower($file))
			{
				$ok=1;
				$aux[1]++;
			}
			$towrite[]=implode("=",$aux);
		}
		
	}
	if (!$ok)
		$towrite[]=$file.'=1';
	if ($fp)
		fclose($fp);
	$fp=fopen($counter,'w');
	fwrite($fp,implode($eol,$towrite));
	fclose($fp);
	
	
	$filename=$download;
	// required for IE, otherwise Content-disposition is ignored
	if(ini_get('zlib.output_compression'))
  		ini_set('zlib.output_compression', 'Off');

	$file_extension = strtolower(substr(strrchr($filename,"."),1));
	switch( $file_extension )
	{
  		case "pdf": $ctype="application/pdf"; break;
  		case "exe": $ctype="application/octet-stream"; break;
  		case "zip": $ctype="application/zip"; break;
  		case "doc": $ctype="application/msword"; break;
  		case "xls": $ctype="application/vnd.ms-excel"; break;
  		case "ppt": $ctype="application/vnd.ms-powerpoint"; break;
  		case "gif": $ctype="image/gif"; break;
  		case "png": $ctype="image/png"; break;
  		case "jpeg":
  		case "jpg": $ctype="image/jpg"; break;
  		default: $ctype="application/force-download";
	}
	header("Pragma: public"); // required
	header("Expires: 0");
	header("Cache-Control: must-revalidate, post-check=0, pre-check=0");
	header("Cache-Control: private",false); // required for certain browsers 
	header("Content-Type: $ctype");
	header("Content-Disposition: attachment; filename=\"".basename($filename)."\";" );
	header("Content-Transfer-Encoding: binary");
	header("Content-Length: ".filesize($filename));
	readfile($filename);
	exit();
	
}
else 
	echo $error_message;

?>