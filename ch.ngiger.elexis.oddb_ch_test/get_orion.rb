#!/usr/bin/env ruby1.9.1
# Copyright Niklaus Giger 2012 <niklaus.giger@member.fsf.org
# Small script to get all info from oddb.yaml about Orion products into a file
start = 'name: Orion Pharma AG'
ende  = 'name: LifeBiotech AG'
src   = '/opt/downloads/oddb.yaml'
dest  = 'orion.tst'

a = `grep --before-context=3 -n '#{start}' #{src}`
b = `grep --before-context=3 -n '#{ende}'  #{src}`
startZeile = a.to_i-1
endeZeile  = b.to_i-2
inhalt = IO.readlines(src)
ausgabe = File.open(dest, 'w+')
startZeile.upto(endeZeile).each {
  |nr|
  ausgabe.puts(inhalt[nr])
}
ausgabe.close
system("head #{dest} && echo '' && tail #{dest}")

                                