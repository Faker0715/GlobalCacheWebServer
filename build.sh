#!/bin/bash
SCRIPT_HOME=$(cd $(dirname $0)/; pwd)

GlobalCacheSDK_path=$SCRIPT_HOME/3rdparty/GlobalCacheSDK
xxl_job_path=$SCRIPT_HOME/3rdparty/xxl-job
xxl_job_auto_register_path=$SCRIPT_HOME/3rdparty/xxl-job-auto-register

function download_3rdparty()
{
    git submodule sync 
    git submodule update --recursive --remote

    cd $GlobalCacheSDK_path
    tag=$(cat $SCRIPT_HOME/dependencies.txt | grep "GlobalCacheSDK:" | cut -d ' ' -f 2)
    git checkout $tag

    cd $xxl_job_path
    tag=$(cat $SCRIPT_HOME/dependencies.txt | grep "xxl-job:" | cut -d ' ' -f 2)
    git checkout $tag
    
    cd $xxl_job_auto_register_path
    tag=$(cat $SCRIPT_HOME/dependencies.txt | grep "xxl-job-auto-register:" | cut -d ' ' -f 2)
    git checkout $tag 
}

function build_dependencies()
{
    cd $GlobalCacheSDK_path
    mvn clean install

    cd $xxl_job_auto_register_path
    mvn clean install

    cd $xxl_job_path/xxl-job-core
    mvn clean install

    cd $xxl_job_path/xxl-job-admin
    mvn clean install
}

function build()
{
    cp -r $GlobalCacheSDK_path/src/main/resources/configure $SCRIPT_HOME/src/resources

    cd $SCRIPT_HOME
    mvn clean install
}

function main()
{
    download_3rdparty

    build_dependencies

    build
}
main