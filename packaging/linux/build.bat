@REM @REM Deb packaging
docker build -t lunalauncher/atlauncher-packaging-linux-deb -f deb/Dockerfile .
docker run --rm -i -v "%cd%"\out:/work/out -w /work/out lunalauncher/atlauncher-packaging-linux-deb dpkg-deb --build ../lunalauncher luna-launcher-1.4-1.deb

