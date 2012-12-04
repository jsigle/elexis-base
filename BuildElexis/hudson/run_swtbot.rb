#!/usr/bin/env ruby
#
# Run the SWTbot tests from the command line
#
require 'fileutils'

ECLIPSE_PLUGINS  ="/home/src/galileo/eclipse/plugins"
INSTALL_DIR      ="/home/src/elexis.trunk/deploy/Elexis-2.0.0"
TEST_CLASS       ="ch.elexis.uitests.AllTests"
# http://www.bonitasoft.org/blog/eclipse/swtbot-my-new-friend/
# see http://github.com/ketan/swtbot/blob/master/org.eclipse.swtbot.eclipse.finder.test/src/org/eclipse/swtbot/eclipse/finder/AllTests.java for an example
# war  org.eclipse.swtbot.eclipse.junit4.headless.swtbottestapplication
#  -testPluginName org.eclipse.swtbot.eclipse.finder.test 
# -product andin the particular order, did work but
# -testpluginname TestElexisUI 

where=Dir.pwd
Dir.chdir(ECLIPSE_PLUGINS+"/..")
Dir.chdir INSTALL_DIR
puts Dir.pwd
prepare="java -jar plugins/org.eclipse.equinox.launcher_1.0.201.R35x_v20090715.jar -application org.eclipse.equinox.p2.director -artifactRepository http://download.eclipse.org/technology/swtbot/galileo/dev-build/update-site -metadataRepository http://download.eclipse.org/technology/swtbot/galileo/dev-build/update-site -installIU org.eclipse.swtbot.eclipse.feature.group -installIU org.eclipse.swtbot.eclipse.gef.feature.group -consoleLog"
puts prepare
#system(prepare)

Dir.chdir INSTALL_DIR
WS="/tmp/workspace"
FileUtils.rm_rf(WS)

toCopyx = [ 
  'org.apache.ant_**',
  'org.eclipse.compare_**',
  'org.eclipse.core.filebuffers_**',
  'org.eclipse.core.filesystem_**',
  'org.eclipse.core.resources_**',
  'org.eclipse.core.variables_**',
  'org.eclipse.debug.core_**',
  'org.eclipse.equinox.p2**',
  'org.eclipse.debug.ui_**',
  'org.eclipse.equinox.frameworkadmin_**',
  'org.eclipse.equinox.simpleconfigurator.manipulator_**',
  'org.eclipse.jdt.apt.core_**',
  'org.eclipse.jdt.core_**',
  'org.eclipse.jdt.core.manipulation_**',
  'org.eclipse.jdt.debug_**',
  'org.eclipse.jdt.debug.ui_**',
  'org.eclipse.jdt.launching_**',
  'org.eclipse.jdt.ui_**',
  'org.eclipse.jface.text_**',
  'org.eclipse.ltk.core.refactoring_**',
  'org.eclipse.ltk.ui.refactoring_**',
  'org.eclipse.pde.ui_**',
  'org.eclipse.search_**',
  'org.eclipse.swtbot.eclipse.junit4.headless_**',
  'org.eclipse.swtbot.eclipse.ui_**',
  'org.eclipse.swtbot.junit4_**',
  'org.eclipse.team.core_**',
  'org.eclipse.team.ui_**',
  'org.eclipse.text_**',
  'org.eclipse.text_**',
  'org.eclipse.ui.cheatsheets_**',
  'org.eclipse.ui.console_**',
  'org.eclipse.ui.editors_**',
  'org.eclipse.ui.ide_**',
  'org.eclipse.ui.intro_**',
  'org.eclipse.ui.navigator_**',
  'org.eclipse.ui.navigator.resources_**',
  'org.eclipse.ui.views_**',
  'org.eclipse.ui.workbench.texteditor_**',
  'org.eclipse.jdt**',
]
toCopy = [ 
  'org.apache.ant**',
  'org.eclipse.core.runtime**',  
  #'org.eclipse.jdt.junit.runtime_*.jar',
  'org.eclipse.swtbot.eclipse.core_*.jar',
  'org.eclipse.swtbot.ant.optional.junit4*.jar', 
  'org.eclipse.swtbot.swt.finder_*.jar',
  'org.eclipse.swtbot.eclipse.finder_*.jar',
  'org.eclipse.swtbot.junit4_x**',
  'org.eclipse.jdt.junit4.runtime*.jar',
  'org.eclipse.swtbot.eclipse.junit4.headless_**',
  'org.hamcrest*.jar',
  'org.junit4**',
  'org.apache.log4j_*.jar',
  ]
  
def doCopy(from, src)
  if Dir.glob(from).size == 0
    puts "Could not find #{from}"
    exit 3
  end
  FileUtils.cp_r(Dir.glob(from.sub('*.jar','**')),src, :verbose=>true)
end

if true
  toCopy.each{ |x| doCopy("#{ECLIPSE_PLUGINS}/#{x}", INSTALL_DIR+'/plugins') }
#  Dir.glob("/opt/4hudson/swtbot/eclipse/plugins/**").each{ |x| doCopy("#{x}", INSTALL_DIR+'/plugins') }
#  Dir.glob("/opt/4hudson/swtbot/eclipse/features/**").each{ |x| doCopy("#{x}", INSTALL_DIR+'/features') }
end
ORIG="/opt/4hudson/elexis"
zipFile="org.eclipse.swtbot.eclipse*.zip"
# wget http://mirror.switch.ch/eclipse/technology/swtbot/galileo/dev-build/org.eclipse.swtbot.eclipse-2.0.0.536-dev-e35.zip
# wget http://github.com/downloads/KentBeck/junit/junit-4.8.2.jar
# cp junit
logFile= "#{where}/run_swtbot.log"
cmd = "#{ENV['JAVA_HOME']}/bin/java "+
#cmd = "#{ENV['JAVA_HOME']}/bin/java -DPLUGIN_PATH "+
#cmd = "./elexis -DPLUGIN_PATH=#{ECLIPSE_PLUGINS} "+
#cmd = "java -DPLUGIN_PATH=#{ECLIPSE_PLUGINS} "+
" -classpath #{INSTALL_DIR}/plugins/org.eclipse.equinox.launcher_*.jar "+
" org.eclipse.core.launcher.Main " +
" -application org.eclipse.swtbot.eclipse.junit4.headless.swtbottestapplication " +
" -testProduct ch.elexis.ElexisProduct " +
"-Dch.elexis.username=niklaus -Dch.elexis.password=ng1234 "+
#"-data #{WS} " +
# " formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,$ECLIPSE_HOME/$TEST_CLASS.xml " +
# " formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter " +
#" -testLoaderClass org.eclipse.jdt.internal.junit4.runner.JUnit4TestLoader -loaderpluginname org.eclipse.jdt.junit4.runtime " +
#" -testProduct ch.elexis.ElexisProduct " +
#" -testPluginName ElexisTestUI "+
#" -testProduct ch.elexis.ElexisProduct " +
#" -className $TEST_CLASS " +
" -os linux -ws gtk -arch x86 " +
" -consoleLog -debug -Xms40m -Xmx348m -XX:MaxPermSize=256m -XX:+HeapDumpOnOutOfMemoryError" +
" 2>&1 | tee --append #{logFile}"

FileUtils.cp(logFile, logFile+".1")
puts cmd
system("echo #{toCopy.inspect} > #{logFile}")
system("echo #{cmd} >> #{logFile}")
system(cmd)
puts "done"
cmd = "grep Missing #{logFile} |sort|uniq"
puts cmd
system(cmd)

# /home/src/swtbot.svn/org.eclipse.swtbot.releng/readme.txt
# Gemäss Tipp von http://dev.eclipse.org/mhonarc/newsLists/news.eclipse.swtbot/msg00582.html
#  /usr/lib/jvm/java-6-sun-1.6.0.20/bin/java -agentlib:jdwp=transport=dt_socket,suspend=y,address=localhost:36046 -Dosgi.requiredJavaVersion=1.6
# -XX:MaxPermSize=256m -Xms40m -Xmx512m -Dch.elexis.username=niklaus -Dch.elexis.password=ng1234 -Dch.elexis.saveScreenshot=true 
# -Declipse.pde.launch=true -Declipse.p2.data.area=@config.dir/p2 -Dfile.encoding=UTF-8 
# -classpath /home/src/galileo/eclipse/plugins/org.eclipse.equinox.launcher_1.0.201.R35x_v20090715.jar 
# org.eclipse.equinox.launcher.Main -os linux -ws gtk -arch x86 -nl de_CH -version 3 -port 51251
# -testLoaderClass org.eclipse.jdt.internal.junit4.runner.JUnit4TestLoader -loaderpluginname org.eclipse.jdt.junit4.runtime 
#-classNames ch.elexis.uitests.AllTests -application org.eclipse.swtbot.eclipse.core.swtbottestapplication 
#-product ch.elexis.ElexisProduct -data /home/src/elexis.trunk/../junit-workspace 
#-configuration file:/home/src/elexis.trunk/.metadata/.plugins/org.eclipse.pde.core/pde-junit/ 
#-dev file:/home/src/elexis.trunk/.metadata/.plugins/org.eclipse.pde.core/pde-junit/dev.properties
# -os linux -ws gtk -arch x86 -nl de_CH -testpluginname TestElexisUI