#!/usr/bin/perl -w

############################################################
# Module Description:
############################################################

# A Perl script that takes three Arguments arg1, arg2 and arg3 and does the following:
# - interpret arg1 as name of a local directory (something like c:/temp)
# - interpret arg2 as url to a remote directory on a ftp server including username and password
# (something like ftp.mine.org/pub/files:user:password)

# do for every file in arg1:
# check if it exists in arg2
#   if no -> upload the file to arg2
#   if yes -> upload the file to arg2 if it is different
#   (different means:
#      if arg3 is "size" -> different size
#      if arg3 is "newer" -> newer filetime)


############################################################
# $Id: upload.pl 3752 2008-03-28 11:44:31Z rgw_ch $
############################################################

use strict;
#use warnings;
use Cwd;
use Net::FTP;

# get system current time
# file time is based on current time
my $curr_time = time();

MAIN:
{
    my ($local_dir) ,                 # Local directory
    my ($ftp_server) ,                # ftp server address
    my ($ftp_dir) ,                   # remote directory on the ftp server
    my ($ftp_user) ,
    my ($ftp_passwd) ,
    my ($diff_method) ,               # "size" or "newer"(default)
    my (@local_files) ,               # list of all local files with its info
    my $ftp,                          # ftp object
    my $help_msg   = "Usage: upload local_dir ftp_addr:user:password [size|newer]\n";

    # arg1 and arg2 are mandatory, arg3 is optional
    my $num_args   = $#ARGV + 1;
    if ($num_args < 2) { die $help_msg; }

    &GetArgs(\@ARGV, \$local_dir, \$ftp_server, \$ftp_dir, \$ftp_user, \$ftp_passwd, \$diff_method);

    # check arguments
    if (!defined($diff_method) || lc($diff_method) ne "size") { $diff_method = "newer"; }

    # verify local directory
    if (! -d $local_dir ) { die "Invalid local directory.\n"; }

    &GetLocalFiles(\$local_dir, \@local_files);
    #local directory should contain at least one file
    if (length(@local_files) < 1) { die "Invalid local directory or no file in it.\n"; }

    #login the ftp server
    print "Connecting to ftp server($ftp_server)...";
    $ftp = new Net::FTP($ftp_server)
        or die "Cannot connect to ftp server($ftp_server): $@";
    $ftp->login($ftp_user, $ftp_passwd)
        or die "Cannot login(user:$ftp_user, password:$ftp_passwd): $@";
    print "Successful!\n";

    #change to the working directory
    $ftp->cwd($ftp_dir)
        or die "Cannot changing working directory($ftp_dir): $ftp->message";

    # wir brauchen das Listing auf dem Server wg. spezieller Behandlung des Dateinamens
    print "Reading directory from ftp server\n";
    my @serverfiles=$ftp->ls;
    my %remote={};
    foreach my $serverfile(@serverfiles){
        next if($serverfile=~ /^\.+/);
        (my $prefix, my $version)=split /_[0-9]/, $serverfile;
        $remote{$prefix}=$serverfile;
    }
    print "processing files\n";
    #upload file one by one
    my ($file_name, $file_mode, $file_size, $file_mdtm, $remote_file_size, $remote_file_mdtm);
    my $file_processed = 0;
    foreach my $tmpfile (@local_files) {
        ($file_name,$file_mode,$file_size,$file_mdtm) = @$tmpfile;
        (my $local_shortname, my $unused)=split /_[0-9]/, $file_name;
        my $remote_fullname=$remote{$local_shortname};
        #print "processing file: $local_shortname: $file_name -> $remote_fullname (", $file_mode ? "binary" : "ascii", ")\n";

        # don't upload zero-size files
        if ($file_size == 0)  { next; }
        $file_processed ++;

        #compare with ftp files
        if (!($remote_file_size = $ftp->size($remote_fullname))){
            # upload file if it doesnot exist
            print "$file_name doesn't exist, upload it.\n";
            PutFile(\$ftp, $file_name, $file_mode);
        } elsif ($diff_method eq "size") {
            # replace existing file when its size is different
            if ($file_size ne $remote_file_size) {
                print "$file_name has been changed (from $file_size to $remote_file_size), upload it.\n";
                PutFile(\$ftp, $file_name, $file_mode);
            }
        } else {
            # replace existing file when it's old
            $remote_file_mdtm = $curr_time - $ftp->mdtm($remote_fullname);
            if ($file_mdtm < $remote_file_mdtm) {
                print "$file_name has been modified (from $file_mdtm to $remote_file_mdtm), upload it.\n";
                PutFile(\$ftp, $file_name, $file_mode);
            }
        }
    }
    $ftp->close;
    print "Upload $file_processed files.\n";
}



#================================#
# GetArgs: parsing arguments
#================================#

sub GetArgs
{
    my ($args) = shift,                      # Command line args
    my ($local_dir) = shift,                 # Local directory
    my ($ftp_server) = shift,                # ftp server address
    my ($remote_dir) = shift,                # remote directory on the ftp server
    my ($ftp_user) = shift,
    my ($ftp_passwd) = shift,
    my ($diff_method) = shift;               # "size" or "newer"(default)

    #set defaults
    $$ftp_server = "";
    $$remote_dir = "";
    $$ftp_user   = "";
    $$ftp_passwd = "";

    # Command-line parsing
    $$local_dir   = shift @$args;
    my $ftp_url   = shift @$args;
    $$diff_method = shift @$args;

    # split ftp url into: server address, directory, username, password
    ($$ftp_server, $$ftp_user, $$ftp_passwd) = split(':', $ftp_url);
    if (!defined($$ftp_user) || !$$ftp_user) {
        $$ftp_user = "anonymous";
        $$ftp_passwd = "abc\@mail.com";
    }
    if (!defined($$ftp_passwd) || !$$ftp_passwd) {
        $$ftp_passwd = "";
    }
    my $dir_position = index $$ftp_server, '/';
    if ($dir_position > 0) {
        $$remote_dir = substr $$ftp_server, $dir_position;
        $$ftp_server = substr $$ftp_server, 0, $dir_position;
    }
    #print "local dir: $$local_dir\n";
    #print "server: $$ftp_server dir: $$remote_dir user: $$ftp_user password: $$ftp_passwd\n";
    #print "condition: $$diff_method\n";
}

#================================#
# GetLocalFiles: 
#================================#
sub GetLocalFiles
{
    my ($local_dir) = shift,
    my ($local_files) = shift;

    @$local_files = ();
    if (!(chdir($$local_dir) && opendir(DIR, $$local_dir))) { returng (1); }

    # get file info one by one
    while (my $tmp_file = readdir(DIR)) {
        #print "file: $tmp_file\n";
        #only upload regular file
        if (-f $tmp_file) {
            # some filetypes, such as PDF, can't be recognized correctly
            # so, we have to handle this  manually
            my @except_suffix = (".pdf");
            my $force_binary = "false";
            foreach my $suffix (@except_suffix) {
                if ((length($tmp_file) > length($suffix)) &&
                    (substr(lc($tmp_file), length($tmp_file) - length($suffix), length($suffix)) eq $suffix)) {
                    $force_binary = "true";
                }
            }

            # store file information: file name, size, modified time
            # Caution:
            #     -M 	Age of file (at script startup) in days since modification.
            # So, it should be first converted to time in seconds
            #my $tmp_time = -M _;
            #$tmp_time = $tmp_time * 24 * 3600;
            #print "$tmp_file modified time: $tmp_time,", $curr_time - $tmp_time, ",",
            #    localtime($curr_time - $tmp_time ), "\n";
            push @$local_files,[$tmp_file, $force_binary eq "true" ? 1 : -B _ , -s _, (-M _) * 24 * 3600];
        }
    }

    closedir(DIR);
}

#================================#
# PutFile: upload file
#================================#
sub PutFile
{
    my $ftp = shift,
    my $name = shift,
    my $binary = shift;

    # specify file type
    if ($binary) {
        $$ftp->binary;
    } else {
        $$ftp->ascii;
    }
    # upload
    $$ftp->pasv;
    $$ftp->put($name) or print "Error: $@\n";
}

