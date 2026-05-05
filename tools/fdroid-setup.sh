#!/bin/bash
# This scripts setups or starts a local fdroid build environment

set -e

# Handle mandatory YAML parameter
if [ -z "$1" ]; then
    echo "Error: Missing required YAML file parameter"
    echo "Usage: $0 <path/to/app.yml> [gitlab_user]"
    exit 1
fi

YAML_FILE="$1"

# Validate YAML file exists
if [ ! -f "$YAML_FILE" ]; then
    echo "Error: YAML file $YAML_FILE not found"
    exit 1
fi

# Resolve paths from YAML file (relocatable base)
YAML_PATH=$(realpath "$YAML_FILE")
BASE_DIR=$(dirname "$YAML_PATH")
FDROID_SERVER_PATH=$BASE_DIR/fdroid-server
FDROID_DATA_PATH=$BASE_DIR/fdroid-data
APP_ID=$(basename "$YAML_FILE" .yml)

# Resolve GITLAB_USER: source .env if exists, allow second parameter override
if [ -f .env ]; then
    source .env
fi

if [ -n "$2" ]; then
    GITLAB_USER="$2"
fi

if [ -z "$GITLAB_USER" ]; then
    echo "Error: GITLAB_USER not defined. Provide as second parameter, set environment variable, or add to .env (cwd)"
    exit 1
fi

run() {
    echo -e "\033[34;1m$@\033[0m"
    "$@"
}

which docker || sudo sh -c 'apt-get update &&apt-get install -y docker.io'

cd $BASE_DIR

if [ ! -e "$FDROID_SERVER_PATH" ]
then
    run git clone --depth=1 https://gitlab.com/fdroid/fdroidserver $FDROID_SERVER_PATH
fi

if [ ! -e "$FDROID_DATA_PATH" ]
then
    run git clone --depth=100 git@gitlab.com:$GITLAB_USER/fdroiddata.git  $FDROID_DATA_PATH
    pushd $FDROID_DATA_PATH
        run git remote set-branches origin $APP_ID
        run git fetch origin $APP_ID && run git checkout $APP_ID || run git checkout -b $APP_ID
    popd
fi

# Remove old files/symlinks
rm -f "$FDROID_DATA_PATH"/fdroid-build.sh "$FDROID_DATA_PATH"/metadata/${APP_ID}.yml

# Generate fdroid-build.sh via heredoc (replaces symlink)
cat << BUILDEOF > "$FDROID_DATA_PATH/fdroid-build.sh"
# Should be run inside
# docker run --rm -itu vagrant --entrypoint /bin/bash   -v \$(pwd):/build:z   -v \$HOME/Documents/godot/fdroidserver:/home/vagrant/fdroidserver:Z   registry.gitlab.com/fdroid/fdroidserver:buildserver 
APP_ID=$APP_ID
run() {
	echo -e "\033[34;1m== \$*\033[0m"
	"\$@"
}

df -h
. /etc/profile
export serverwebroot=\$(pwd)/build
run echo \$serverwebroot
export PATH="\$fdroidserver:\$PATH" PYTHONPATH="\$fdroidserver"
export JAVA_HOME=\$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk -F'=' '{print \$2}' | tr -d ' ')
run sudo bash /home/vagrant/fdroidserver/buildserver/provision-gradle
run sdkmanager --install "platform-tools" "build-tools;36.0.0" "platforms;android-36" "cmdline-tools;latest" "cmake;3.10.2.4988404" "ndk;23.2.8568313"
#run sudo apt-get update -y
#run sudo apt-get install -y pkgconf clang scons imagemagick python3-pip python-is-python3 apksigner
cd /build
run fdroid readmeta -v
run fdroid rewritemeta -v \$APP_ID
run fdroid lint -v \$APP_ID
run fdroid checkupdates -v --auto --allow-dirty \$APP_ID
run fdroid build -vl \$APP_ID
BUILDEOF

chmod +x "$FDROID_DATA_PATH/fdroid-build.sh"

# Symlink the YAML file (preserve for sandbox edits to propagate outside)
run ln -f "$YAML_PATH" "$FDROID_DATA_PATH"/metadata/

run sudo chown -R 1000:1000 $FDROID_DATA_PATH $FDROID_SERVER_PATH

echo Inside the docker you usually run build/fdroid-build.sh or any part of it

run docker run --rm \
    -itu vagrant \
    --entrypoint /bin/bash \
    -v $FDROID_DATA_PATH:/build:z  \
    -v $FDROID_SERVER_PATH:/home/vagrant/fdroidserver:Z \
    registry.gitlab.com/fdroid/fdroidserver:buildserver || true

run sudo chown -R $USER:$USER $FDROID_DATA_PATH $FDROID_SERVER_PATH
