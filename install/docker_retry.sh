#!/bin/bash

docker_available() {
    if hash docker 2>/dev/null; then
        echo 1;
    else
        echo 0;
    fi
}

wget https://get.docker.com ./docker_install.sh
sudo chmod +x ./docker_install.sh
./docker_install.sh > 2>&1

while [ $(docker_available) -eq 0 ]; do
        sleep 5
        rm ./docker_install.sh
        wget https://get.docker.com ./docker_install.sh
        sudo chmod +x ./docker_install.sh
        ./docker_install.sh > docker_install.out 2>&1
done