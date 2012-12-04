#!/usr/bin/env ruby
# Copyright (c) Niklaus Giger, 2011, niklaus.giger@member.fsf.org
# License: Eclipse Public Version 1.0
#
require 'optparse'
require 'tempfile'

options = {}
options[:oldVersion] = '2.1.5.4'
options[:newVersion] = nil
if File.exists?("#{Dir.pwd}/.hg")
  options[:root] = Dir.pwd
else
  options[:root] = File.dirname(File.dirname(File.dirname(File.expand_path(__FILE__))))
end

OptionParser.new do |opts|
  opts.banner = "Usage: #{File.basename(__FILE__)} [options]\n" +
      "Check wheter all plugins with changed files (since oldVersion) have a new plugin version.\n"+
      "Will look for all plugins under #{options[:root]} which don't have 'svn' in theirs names (excluding archie repository)"
  opts.on("-o", "--oldVersion version", "changes since this version.") do |v|
    options[:oldVersion] = v
  end
  opts.on("-n", "--newVersion version", "changes up to this version. Default to current revision") do |v|
    options[:newVersion] = v
  end
  opts.on("-r", "--root root", "Look for plugins here instead of #{options[:root]}") do |v|
   options[:root] = File.expand_path(v)
  end
  opts.on("-h", "--help", "Show this help") do |v|
    puts opts
    exit
  end
end.parse!

BundleVersion =/Bundle-Version\:/
ShowNew = options[:newVersion] ? options[:newVersion] : " tip of branch"
puts "Checking versions of all plugins under #{options[:root]} between Mercurial version #{options[:oldVersion] } and #{ShowNew}"
RequireAtLeast_1_0 = false
if RequireAtLeast_1_0
  puts "All plug-ins must have at least Version 1.0"
end

file = Tempfile.new('plugin')
TempName = file.path    
file.close

def getPlugInVersion(baseDir, whichVersion=nil)
  saved = Dir.pwd
  Dir.chdir(baseDir)
  mf = "#{baseDir}/META-INF/MANIFEST.MF"
  puts mf if $VERBOSE
  if !File.exists?(mf)
    puts "Was ist hier los? #{mf} müsste existieren"
    exit 3
  end
  fileToRead = nil
  if whichVersion == nil
    fileToRead = mf
  else
    fileToRead = TempName
    cmd = "hg cat #{mf}"
    cmd += " -r #{whichVersion} " if whichVersion 
    cmd += " > #{TempName}"
    res = system(cmd)
    Dir.chdir(saved)
    if !res && whichVersion == nil then
      puts "running #{cmd} failed"
      exit 2 
    end
  end
  IO.readlines(fileToRead).each {
    |line|
      return line.split(': ')[1].chomp if BundleVersion.match(line)
  }
  puts "Could not find #{BundleVersion.to_s}. May be did not exist in #{whichVersion}"
  return '0.0.0'
end

def checkVersion(plugin, version)
  details = version.split('.')
  if (details[0] == '0' and RequireAtLeast_1_0) 
    puts "WARNING !!!! Version #{version} for #{plugin} must be at least 1.0.0"
  end
end

def nrDiffLines(pluginPath, oldVers, newVers)
  saved = Dir.pwd
  Dir.chdir(pluginPath)
  cmd = "hg diff . -r #{oldVers}"
  cmd += " -r #{newVers} " if newVers
  cmd += " 2>&1 > #{TempName}"
  res = system(cmd)
  if !res then
    puts "running #{cmd} failed"
    exit 2 
  end
  return IO.readlines(TempName).size
  Dir.chdir(saved) 
end


nrPlugins=0
path = "#{options[:root]}/*/plugin.xml"
path = "#{options[:root]}/*/*/plugin.xml" if Dir.glob("#{options[:root]}/*/.hg").size > 0


def handlePlugin(path, oldVersion, newVersion)
      next if /svn\//.match(path) # Skip archie and possibly other SVN plug-ins
      newPlugInVersion = getPlugInVersion(File.dirname(path), newVersion)
      oldPlugInVersion = getPlugInVersion(File.dirname(path), oldVersion )
      name = File.basename(File.dirname(path))
      checkVersion(name, newPlugInVersion)
      nrDiffs =  nrDiffLines(File.dirname(path), oldVersion , newVersion)
      if nrDiffs > 0
	warning = newPlugInVersion.eql?(oldPlugInVersion) ? "WARNING: Must change version!!" : " okay"
	puts sprintf("Plugin %-40s versions %7s -> %7s. %6d difflines since %7s %7s. %s",
                     name, oldPlugInVersion, newPlugInVersion, nrDiffs, oldVersion , ShowNew, warning)
      end
end

Dir.glob(path).
each { 
  |y|
    handlePlugin(y, options[:oldVersion], options[:newVersion])
    nrPlugins += 1
}
puts "Checked #{nrPlugins} plugins (searched for #{path})"

