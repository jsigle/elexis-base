#!/usr/bin/env ruby
# (c) 2010 Niklaus Giger, niklaus.giger@member.fsf.org
# Small script to create a shared repository for elexis (mother)
# We assume that you access your elexis-grandma via a fast LAN
# and use therefore the switch --uncompress
#

require 'fileutils'

Origin   =  "/var/cache/hg"
# other possibilities might be
# Origin = "/nfs/server/mercurial-repos/
# Origin = "http://intranet.ourcompany.com/mercurial-repos/

Dest    = "#{ENV['HOME']}/repos"

Hg_Repos = [ 'elexis-base',
 'elexis-addons',
# 'medelexis-trunk',
]

Hg_Repos.each { |name|
	dest = "#{Dest}/#{name}"
	cmd  = "hg clone --noupdate --uncompress #{Origin}/#{name} #{dest}"
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
