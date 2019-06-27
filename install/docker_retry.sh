#!/bin/bash

docker_available() {
    if hash docker 2>/dev/null; then
        echo 1;
    else
        echo 0;
    fi
}

while [ $(docker_available) -eq 0 ]; do
        sleep 5
        wget https://get.docker.com -O ./docker_install.sh
        sudo chmod +x ./docker_install.sh
        ./docker_install.sh
done
