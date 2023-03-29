#!/bin/bash
SCRIPT_HOME=$(cd $(dirname $0)/; pwd)

GlobalCacheSDK_path=$SCRIPT_HOME/3rdparty/GlobalCacheSDK
xxl_job_path=$SCRIPT_HOME/3rdparty/xxl-job
xxl_job_auto_register_path=$SCRIPT_HOME/3rdparty/xxl-job-auto-register


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
    cp -r $GlobalCacheSDK_path/src/main/resources/configure $SCRIPT_HOME/src/main/resources

    cd $SCRIPT_HOME
    mvn clean install
}

function main()
{
    build_dependencies

    build
}
main