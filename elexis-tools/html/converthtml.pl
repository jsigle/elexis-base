#!/usr/bin/perl -w
# *****************************************************************
# convert the output of tex2html to corecttly formatted utf-8
# (c) 2007 G. Weirich and Elexis
# usage: converthtml <sourcedir>
# ****************************************************************
use strict;

my $dir=$ARGV[0];
if($dir=~ /.+\.html?/){
	&convert($dir);
}else{
    opendir DIR, $dir;
    foreach my $file(readdir(DIR)){
        if($file=~ /\.html?/i){
		print $file."\n";
	    	my $basename=$dir."/".$file;
		&convert($basename);
        }
    }
    closedir DIR;
}

&convert($ARGV[0]);

sub convert{
    my $basename=shift;
    open IN, $basename or die "Kann $basename nicht öffnen";
    open OUT, ">$basename".".new" or die "Kann file nicht schreiben";
    while(<IN>){
      my $line=$_;
      $line=~ s/&#195;&#164;/ä/g;
      $line=~ s/&#195;&#188;/ü/g;
      $line=~ s/&#195;&#182;/ö/g;
      $line=~ s/&#195;&#150;/Ö/g;
      $line=~ s/&#195;&#156;/Ü/g;
      $line=~ s/\"file:\/usr\/local\/share\/lib\/latex2html\/icons\/(\w+\.(png|gif))\"/\"icons\/$1\")/;
      $line=~ s/images\/([a-z0-9A-Z]+)\">/images\/$1\.png\">/g;
      print OUT $line;
    }
    close IN;
    close OUT;
    rename $basename, $basename.".old";
    rename $basename.".new", $basename;

}
