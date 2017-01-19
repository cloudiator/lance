#!/bin/bash

version=ubuntu_1_12_5

docker_available() {
    if hash docker 2>/dev/null; then
        echo 1;
    else
        echo 0;
    fi
}

wget https://raw.githubusercontent.com/cloudiator/lance/master/install/docker_install_"$version".sh -O ./docker_install.sh
sudo chmod +x ./docker_install.sh
./docker_install.sh

while [ $(docker_available) -eq 0 ]; do
        sleep 5
        wget https://get.docker.com -O ./docker_install.sh
        sudo chmod +x ./docker_install.sh
        ./docker_install.sh
done
