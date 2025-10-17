# Should be run inside
# docker run --rm -itu vagrant --entrypoint /bin/bash   -v $(pwd):/build:z   -v $HOME/Documents/godot/fdroidserver:/home/vagrant/fdroidserver:Z   registry.gitlab.com/fdroid/fdroidserver:buildserver 
APP_ID=net.canvoki.carburoid
run() {
	echo -e "\033[34;1m== $*\033[0m"
	"$@"
}

df -h
. /etc/profile
export serverwebroot=$(pwd)/build
run echo $serverwebroot
export PATH="$fdroidserver:$PATH" PYTHONPATH="$fdroidserver"
export JAVA_HOME=$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk -F'=' '{print $2}' | tr -d ' ')
run sudo bash /home/vagrant/fdroidserver/buildserver/provision-gradle
run sdkmanager --install "platform-tools" "build-tools;36.0.0" "platforms;android-36" "cmdline-tools;latest" "cmake;3.10.2.4988404" "ndk;23.2.8568313"
#run sudo apt-get update -y
#run sudo apt-get install -y pkgconf clang scons imagemagick python3-pip python-is-python3 apksigner
cd /build
run fdroid readmeta -v
run fdroid rewritemeta -v $APP_ID
run fdroid lint -v $APP_ID
run fdroid checkupdates -v --auto --allow-dirty $APP_ID
run fdroid update -v --create-metadata net.canvoki.carburoid
run fdroid build -vl $APP_ID

