#!/usr/bin/env ruby
# (c) 2010 Niklaus Giger, niklaus.giger@member.fsf.org
# Small script to create a workspace for elexis
#
require 'fileutils'
if !ARGV[0]
	puts "useage: #{__FILE__} path/to/root_of_workspace"
	exit 2
end

Dest = ARGV[0]
Origin    = "#{ENV['HOME']}/repos"

puts "Creating workspace with clone of repositories at #{Dest}"

Hg_Repos = [ 'elexis-base',
 'elexis-addons',
# 'medelexis-trunk',
]

Svn_Repos = {  'archie-trunk' => 'http://archie.googlecode.com/svn/archie/ch.unibe.iam.scg.archie/trunk', }

Hg_Repos.each { |name|
	dest = "#{Dest}/#{name}"
	FileUtils.makedirs(File.dirname(dest))
	# We assume that you access your elexis-grandma via a fast LAN
	# and use therefore the switch --uncompress
	cmd  = "hg clone --quiet --uncompress #{Origin}/#{name} #{dest}"
	if File.directory?(dest)
		puts "Destination #{dest} already exists. Skipping #{cmd}"
	else
		puts cmd
		if !system(cmd) 
		  puts "Running #{cmd} failed"
		  exit 3
		end
 	end
}

Svn_Repos.each{ |name, origin|
	dest = "#{Dest}/#{name}"
	FileUtils.makedirs(File.dirname(dest))
	cmd  = "svn checkout --quiet #{origin} #{dest}"
	puts cmd
	if File.directory?(dest)
		puts "Destination #{dest} already exists. Skipping #{cmd}"
	else
		puts cmd
		if !system(cmd) 
		  puts "Running #{cmd} failed"
		  exit 3
		end
 	end
}

FileUtils.makedirs("#{Dest}/workspace")

puts "Don't forget to import  #{Dest}/elexis-base/elexis-developer-resources/doc/ElexisFormatterProfile.xml"
puts "  as the default code style under Window..Preferences..Java..Formatter..Import"
puts "  start you new workspace using"
puts "eclipse -data #{Dest}/workspace"