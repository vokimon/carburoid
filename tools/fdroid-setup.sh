#!/bin/bash
# This scripts setups or starts a local fdroid build environment

set -e
SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
FDROID_SERVER_PATH=$SCRIPTPATH/fdroid-server
FDROID_DATA_PATH=$SCRIPTPATH/fdroid-data
GITLAB_USER=vokimon
APP_ID=net.canvoki.carburoid

run() {
    echo -e "\033[34;1m$@\033[0m"
    "$@"
}


which docker || sudo sh -c 'apt-get update &&apt-get install -y docker.io'

cd $SCRIPTPATH

if [ ! -e "$FDROID_SERVER_PATH" ]
then
    run git clone --depth=1 https://gitlab.com/fdroid/fdroidserver $FDROID_SERVER_PATH
fi

if [ ! -e "$FDROID_DATA_PATH" ]
then
    run git clone --depth=100 git@gitlab.com:vokimon/fdroid-data.git  $FDROID_DATA_PATH
    pushd $FDROID_DATA_PATH
        run git remote set-branches origin $APP_ID
        run git fetch origin $APP_ID && run git checkout $APP_ID || run git checkout -b $APP_ID
    popd
    # hard link since soft link is not visible on 
fi

rm -f "$FDROID_DATA_PATH"/fdroid-build.sh "$FDROID_DATA_PATH"/metadata/${APP_ID}.yml
run ln -f $SCRIPTPATH/fdroid-build.sh "$FDROID_DATA_PATH"/fdroid-build.sh
run ln -f $SCRIPTPATH/${APP_ID}.yml "$FDROID_DATA_PATH"/metadata/

run sudo chown -R 1000:1000 $FDROID_DATA_PATH $FDROID_SERVER_PATH

echo Inside the docker you usually run build/fdroid-build.sh or any part of it

run docker run --rm \
    -itu vagrant \
    --entrypoint /bin/bash \
    -v $FDROID_DATA_PATH:/build:z  \
    -v $FDROID_SERVER_PATH:/home/vagrant/fdroidserver:Z \
    registry.gitlab.com/fdroid/fdroidserver:buildserver || true

run sudo chown -R $USER:$USER $FDROID_DATA_PATH $FDROID_SERVER_PATH


