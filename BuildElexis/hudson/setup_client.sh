#! /bin/bash -v
# 2010-04-10 Niklaus Giger
#
# *.key-Dateien sollten geheim gehalten werden
# *.crt und *.pem-Dateien sind öffentlich
# Am besten keine Leerzeichen beim Kundenamen haben.
#

export KundenName="$1"
if test -z $1
then
  echo "Sie muessen einen Kundennamen angeben!"
else
  echo OpenVPN setup für $KundenName generieren
fi

#------------------------------------------------------------------------------
# Hier noch ihre Parameter beim UserAdd hinzufügen
# Habe mal -m zum Erstellen des Home-Verzeichnisses genommen
#------------------------------------------------------------------------------
# /usr/sbin/useradd --create-home  $KundenName
# passwd $KundenName ??

#------------------------------------------------------------------------------
# Ab hier sollte es nichts mehr zum Andern geben
#------------------------------------------------------------------------------

# Dann diese Datei als root ausführen
cd /etc/openvpn/easy-rsa
. /etc/openvpn/vars
#Jetzt noch den ersten Test-Clients erstellen
echo /etc/openvpn/easy-rsa/build-key --batch $KundenName
/etc/openvpn/easy-rsa/build-key --batch $KundenName

# What follows is a so called Here document (or variable) of bash
confFile=$(cat <<EndOfVar
##############################################
# Config-File for Analytica OpenVPN          #
##############################################

client
dev tun

# Windows needs the TAP-Win32 adapter name
# from the Network Connections panel
# if you have more than one.  On XP SP2,
# you may need to disable the firewall
# for the TAP adapter.
;dev-node MyTap

proto udp 
remote 172.25.1.40  1194

# Keep trying indefinitely to resolve the
# host name of the OpenVPN server.  Very useful
# on machines which are not permanently connected
# to the internet such as laptops.
resolv-retry infinite

nobind

# Try to preserve some state across restarts.
persist-key
persist-tun

ca   analytica-ca.crt
cert $KundenName.crt
key  $KundenName.key

ns-cert-type server
comp-lzo

# Set log file verbosity.
verb 3

# Silence repeating messages
mute 20
mute-replay-warnings
EndOfVar) 
echo "$confFile" > /etc/openvpn/keys/$KundenName.ovpn
chown $KundenName /etc/openvpn/keys/$KundenName.*
unix2dos /etc/openvpn/keys/$KundenName.ovpn
cp /etc/openvpn/keys/ca.crt /home/$KundenName/analytica-ca.crt
cp /etc/openvpn/keys/$KundenName.crt /home/$KundenName
cp /etc/openvpn/keys/$KundenName.key /home/$KundenName
cp -p /etc/openvpn/keys/$KundenName.ovpn /home/$KundenName/$KundenName.ovpn
echo /home/$KundeName/$KundenName.ovpn
echo "Created config/key-files for $KundenName in /home/$KundenName"
ls -l /home/$KundenName
echo $clientConf
