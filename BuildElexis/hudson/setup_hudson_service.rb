#!/usr/bin/env ruby
# Niklaus Giger (c) 2010 niklaus.giger@member.fsf.org
#
# 2010.04.11	Simple setup hudson-service
#
require 'ftools'
require 'fileutils'

DryRun = false
def system(cmd, mayFail=false)
  puts "cd #{Dir.pwd} && #{cmd} # mayFail #{mayFail}"
  if DryRun then return
  else res =Kernel.system(cmd)
  end
  if !res and !mayFail then
    puts "running #{cmd} #{mayFail} failed"
    exit
  end
end

cmd="sudo addgroup --gid 1900 elexis"
system(cmd, true)

Passwd=<<EOF
1234
1234
EOF
datei = File.open("/tmp/passwd", "w+")
datei.puts Passwd
datei.close

1.upto(10) do
  |x|
    cmd="sudo userdel elexis-#{x}"
    system(cmd, true)
    cmd="sudo useradd --gid 1900 --uid 190#{x.to_s} --create-home elexis-#{x}"
    system(cmd, true)
    cmd="cat /tmp/passwd | sudo passwd elexis-#{x}"
    system(cmd, true)
end

