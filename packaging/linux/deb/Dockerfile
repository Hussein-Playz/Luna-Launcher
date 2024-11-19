FROM debian:11
LABEL maintainer="houseinplayz@gmail.com"

# add in all the files
ADD deb/control /work/lunalauncher/DEBIAN/control
ADD deb/postrm /work/lunalauncher/DEBIAN/postrm
ADD deb/lunalauncher /work/lunalauncher/usr/bin/lunalauncher
ADD _common/luna-launcher.desktop /work/lunalauncher/usr/share/applications/luna-launcher.desktop
ADD _common/luna.metainfo.xml /work/lunalauncher/usr/share/metainfo/luna.metainfo.xml
ADD _common/luna.png /work/lunalauncher/usr/share/pixmaps/luna.png

# chmod to what is needed
RUN chmod -R 0755 /work

# set the workdir
WORKDIR /work
