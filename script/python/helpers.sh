#!/usr/bin/env bash
cd python
export PYTHONPATH=$(pwd)
#https://stackoverflow.com/questions/27835619/urllib-and-ssl-certificate-verify-failed-error
#PYTHONHTTPSVERIFY works around the following error
#ssl.SSLError: [SSL: CERTIFICATE_VERIFY_FAILED] certificate verify failed
#TODO remove this line
export PYTHONHTTPSVERIFY=0
. environment/bin/activate
